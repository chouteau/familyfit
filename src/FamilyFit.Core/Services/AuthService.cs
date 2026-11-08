using System.Data;
using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;
using FamilyFit.Core.Data;
using Microsoft.EntityFrameworkCore;
using Microsoft.AspNetCore.WebUtilities;
using Microsoft.Extensions.Configuration;
using FamilyFit.Contracts.Models.Authentication;

namespace FamilyFit.Core.Services;

public sealed class AuthService(IDbContextFactory<FamilyDb> factory, TimeProvider clock, IConfiguration configuration, AgentAuthorization agents)
{
    public DateTimeOffset Now => clock.GetUtcNow();
    public static string Hash(string text) => Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(text)));
    public static string Secret() => WebEncoders.Base64UrlEncode(RandomNumberGenerator.GetBytes(32));
    private static string Code()
    {
        const string chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        return string.Concat(Enumerable.Range(0, 10).Select(_ => chars[RandomNumberGenerator.GetInt32(chars.Length)]));
    }
    public static string NormalizeCode(string code)
    {
        if (string.IsNullOrWhiteSpace(code)) throw new DomainException("Saisis le code affiché sur le site.");
        var normalized = code.Trim().Replace("-", "").Replace(" ", "").ToUpperInvariant();
        if (normalized.Length != 10 || normalized.Any(c => !"ABCDEFGHJKLMNPQRSTUVWXYZ23456789".Contains(c)))
            throw new DomainException("Ce code n’est pas valide. Vérifie les dix caractères.");
        return normalized;
    }
    private string Qr(string purpose, string code) => $"familyfit://{purpose}?code={code}&server={Uri.EscapeDataString(configuration["FamilyFit:PublicUrl"] ?? "https://fit.chouteau.info")}";

    public async Task<InvitationGrant> CreateInvitationAsync(Guid memberId, CancellationToken ct = default)
    {
        await using var db = await factory.CreateDbContextAsync(ct);
        if (!await db.Members.AnyAsync(x => x.Id == memberId, ct)) throw new DomainException("Membre introuvable.", 404);
        var now = Now;
        await db.Invitations.Where(x => x.MemberId == memberId && x.UsedAtUtc == null).ExecuteUpdateAsync(s => s.SetProperty(x => x.UsedAtUtc, now), ct);
        var code = Code();
        var invitation = new Invitation { MemberId = memberId, CodeHash = Hash(code), ExpiresAtUtc = now.AddMinutes(10) };
        db.Invitations.Add(invitation);
        await db.SaveChangesAsync(ct);
        return new(code, Qr("enroll", code), invitation.ExpiresAtUtc);
    }

    public async Task<DeviceGrant> EnrollAsync(EnrollRequest request, CancellationToken ct = default)
    {
        var codeHash = Hash(NormalizeCode(request.Code));
        if (string.IsNullOrWhiteSpace(request.DeviceName) || request.DeviceName.Length > 80) throw new DomainException("Donne un nom à ce téléphone (80 caractères maximum).");
        await using var db = await factory.CreateDbContextAsync(ct);
        await using var tx = await db.Database.BeginTransactionAsync(IsolationLevel.Serializable, ct);
        var invitation = await db.Invitations.SingleOrDefaultAsync(x => x.CodeHash == codeHash, ct);
        if (invitation is null || invitation.UsedAtUtc != null || invitation.ExpiresAtUtc <= Now)
            throw new DomainException("Invitation expirée ou déjà utilisée. Demande une nouvelle invitation.", 400);
        var token = Secret();
        var device = new Device { MemberId = invitation.MemberId, Name = request.DeviceName.Trim(), TokenHash = Hash(token), CreatedAtUtc = Now };
        invitation.UsedAtUtc = Now;
        db.Devices.Add(device);
        var member = await db.Members.SingleAsync(x => x.Id == device.MemberId, ct);
        await db.SaveChangesAsync(ct);
        await tx.CommitAsync(ct);
        var privateDirectory = configuration["FamilyFit:DataDirectory"];
        if (privateDirectory != null && member.IsAdmin)
        {
            var bootstrap = Path.Combine(privateDirectory, "private", $"bootstrap-{member.Id}.html");
            if (File.Exists(bootstrap)) File.Delete(bootstrap);
        }
        return new(token, device.Id, member);
    }

    public async Task<MemberLoginLinkCreated> CreateMemberLoginLinkAsync(Guid memberId, CancellationToken ct = default)
    {
        await using var db = await factory.CreateDbContextAsync(ct);
        var member = await db.Members.AsNoTracking().SingleOrDefaultAsync(x => x.Id == memberId, ct) ?? throw new DomainException("Membre introuvable.", 404);
        var token = "ffb_" + Secret();
        var link = new MemberLoginLink { MemberId = member.Id, TokenHash = Hash(token), ExpiresAtUtc = Now.AddMinutes(5) };
        db.MemberLoginLinks.Add(link); await db.SaveChangesAsync(ct);
        var url = (configuration["FamilyFit:PublicUrl"] ?? "https://fit.chouteau.info").TrimEnd('/') + "/login#secret=" + Uri.EscapeDataString(token);
        return new(member.Id, member.Nickname, url, link.ExpiresAtUtc);
    }

    public async Task<MemberLoginPreview?> PreviewMemberLoginLinkAsync(string? token, string? browserSecret, CancellationToken ct = default)
    {
        if (token?.Length != 47 || !token.StartsWith("ffb_", StringComparison.Ordinal)) return null;
        await using var db = await factory.CreateDbContextAsync(ct);
        var hash = Hash(token);
        var link = await db.MemberLoginLinks.AsNoTracking().SingleOrDefaultAsync(x => x.TokenHash == hash, ct);
        if (link == null || link.UsedAtUtc != null || link.ExpiresAtUtc <= Now || (link.BrowserSecretHash != null && (browserSecret == null || link.BrowserSecretHash != Hash(browserSecret)))) return null;
        var member = await db.Members.AsNoTracking().SingleOrDefaultAsync(x => x.Id == link.MemberId, ct);
        return member == null ? null : new(member.Id, member.Nickname, link.ExpiresAtUtc);
    }

    public async Task<(ChallengeGrant Grant, string BrowserSecret)> CreateChallengeAsync(string linkToken, string? browserSecret = null, CancellationToken ct = default)
    {
        if (linkToken?.Length != 47 || !linkToken.StartsWith("ffb_", StringComparison.Ordinal)) throw new DomainException("Lien non accessible.", 404);
        var code = Code();
        var secret = browserSecret?.Length == 43 ? browserSecret : Secret();
        await using var db = await factory.CreateDbContextAsync(ct);
        await using var tx = await db.Database.BeginTransactionAsync(IsolationLevel.Serializable, ct);
        var hash = Hash(linkToken);
        var link = await db.MemberLoginLinks.SingleOrDefaultAsync(x => x.TokenHash == hash, ct);
        if (link == null || link.UsedAtUtc != null || link.ExpiresAtUtc <= Now) throw new DomainException("Lien non accessible.", 404);
        if (link.BrowserSecretHash != null && link.BrowserSecretHash != Hash(secret)) throw new DomainException("Ce lien est déjà utilisé dans un autre navigateur.", 403);
        link.BrowserSecretHash ??= Hash(secret);
        var now = Now;
        await db.LoginChallenges.Where(x => x.MemberLoginLinkId == link.Id && x.UsedAtUtc == null).ExecuteUpdateAsync(s => s.SetProperty(x => x.UsedAtUtc, now), ct);
        var challenge = new LoginChallenge { ExpectedMemberId = link.MemberId, MemberLoginLinkId = link.Id, CodeHash = Hash(code), BrowserSecretHash = Hash(secret), ExpiresAtUtc = link.ExpiresAtUtc };
        db.LoginChallenges.Add(challenge);
        await db.SaveChangesAsync(ct);
        await tx.CommitAsync(ct);
        return (new(challenge.Id, code, Qr("login", code), challenge.ExpiresAtUtc), secret);
    }

    public async Task ApproveAsync(Actor actor, string code, CancellationToken ct = default)
    {
        if (actor.SessionId != null || actor.AgentId != null) throw new DomainException("Confirme la connexion depuis ton application mobile.", 403);
        var hash = Hash(NormalizeCode(code));
        await using var db = await factory.CreateDbContextAsync(ct);
        var target = await db.LoginChallenges.AsNoTracking().SingleOrDefaultAsync(x => x.CodeHash == hash, ct);
        if (target == null || target.MemberLoginLinkId == null) throw new DomainException("Demande expirée ou inaccessible.");
        if (target.ExpectedMemberId != actor.MemberId) throw new DomainException("Ce lien est réservé à un autre membre. Utilise le téléphone associé au pseudo affiché.", 403);
        var link = await db.MemberLoginLinks.AsNoTracking().SingleOrDefaultAsync(x => x.Id == target.MemberLoginLinkId, ct);
        if (link == null || link.UsedAtUtc != null || link.ExpiresAtUtc <= Now) throw new DomainException("Le lien a expiré. Demande un nouveau lien à l’administrateur.");
        var affected = await db.LoginChallenges.Where(x => x.CodeHash == hash && x.ExpiresAtUtc > Now && x.UsedAtUtc == null && x.ApprovedDeviceId == null)
            .ExecuteUpdateAsync(s => s.SetProperty(x => x.ApprovedDeviceId, actor.DeviceId), ct);
        if (affected != 1) throw new DomainException("Demande expirée ou déjà confirmée. Actualise la page du site.");
    }

    public async Task<(string State, BrowserSession? Session)> ConsumeAsync(Guid id, string browserSecret, CancellationToken ct = default)
    {
        await using var db = await factory.CreateDbContextAsync(ct);
        await using var tx = await db.Database.BeginTransactionAsync(IsolationLevel.Serializable, ct);
        var item = await db.LoginChallenges.SingleOrDefaultAsync(x => x.Id == id, ct);
        if (item is null || !CryptographicOperations.FixedTimeEquals(Encoding.ASCII.GetBytes(item.BrowserSecretHash), Encoding.ASCII.GetBytes(Hash(browserSecret))))
            throw new DomainException("Cette demande appartient à un autre navigateur.", 403);
        if (item.UsedAtUtc != null || item.ExpiresAtUtc <= Now) return ("expired", null);
        if (item.MemberLoginLinkId == null) return ("expired", null);
        var link = await db.MemberLoginLinks.SingleOrDefaultAsync(x => x.Id == item.MemberLoginLinkId, ct);
        if (link == null || link.UsedAtUtc != null || link.ExpiresAtUtc <= Now) return ("expired", null);
        if (item.ApprovedDeviceId == null) return ("pending", null);
        var device = await db.Devices.SingleOrDefaultAsync(x => x.Id == item.ApprovedDeviceId && x.RevokedAtUtc == null, ct);
        if (device == null) return ("expired", null);
        if (device.MemberId != link.MemberId || device.MemberId != item.ExpectedMemberId) return ("expired", null);
        var session = new BrowserSession { MemberId = device.MemberId, DeviceId = device.Id, ExpiresAtUtc = Now.AddHours(12) };
        db.BrowserSessions.Add(session);
        item.UsedAtUtc = Now;
        link.UsedAtUtc = Now;
        await db.SaveChangesAsync(ct);
        await tx.CommitAsync(ct);
        return ("approved", session);
    }

    public async Task<ClaimsPrincipal?> AuthenticateDeviceAsync(string token, CancellationToken ct = default)
    {
        if (token.Length is < 40 or > 100) return null;
        await using var db = await factory.CreateDbContextAsync(ct);
        var hash = Hash(token);
        var device = await db.Devices.SingleOrDefaultAsync(x => x.TokenHash == hash && x.RevokedAtUtc == null, ct);
        if (device == null) return null;
        var member = await db.Members.SingleAsync(x => x.Id == device.MemberId, ct);
        return Principal(member, device.Id, null);
    }

    public static ClaimsPrincipal Principal(Member member, Guid deviceId, Guid? sessionId)
    {
        var claims = new List<Claim> { new(ClaimTypes.NameIdentifier, member.Id.ToString()), new(ClaimTypes.Name, member.Nickname), new("device", deviceId.ToString()), new("method", sessionId == null ? "device" : "browser") };
        if (member.IsAdmin) claims.Add(new(ClaimTypes.Role, "Admin"));
        if (sessionId != null) claims.Add(new("session", sessionId.ToString()!));
        return new(new ClaimsIdentity(claims, sessionId == null ? "Device" : "Cookies"));
    }

    public static ClaimsPrincipal BootstrapPrincipal(Member member, Guid sessionId) => new(new ClaimsIdentity([
        new(ClaimTypes.NameIdentifier, member.Id.ToString()), new(ClaimTypes.Name, member.Nickname),
        new(ClaimTypes.Role, "Admin"), new("method", "browser"), new("bootstrap-session", sessionId.ToString())
    ], "Cookies"));

    public async Task<Actor> RequireActorAsync(ClaimsPrincipal user, bool admin = false, CancellationToken ct = default)
    {
        if (user.FindFirstValue("method") == "agent") return await agents.RequireActorAsync(user, admin, ct);
        if (user.HasClaim(x => x.Type == "bootstrap-session")) return await RequireBootstrapActorAsync(user, ct);
        if (user.Identity?.IsAuthenticated != true || !Guid.TryParse(user.FindFirstValue(ClaimTypes.NameIdentifier), out var memberId) || !Guid.TryParse(user.FindFirstValue("device"), out var deviceId))
            throw new DomainException("Reconnecte-toi avec ton application mobile.", 401);
        await using var db = await factory.CreateDbContextAsync(ct);
        var device = await db.Devices.AsNoTracking().SingleOrDefaultAsync(x => x.Id == deviceId && x.MemberId == memberId && x.RevokedAtUtc == null, ct);
        if (device == null) throw new DomainException("Ce téléphone n’est plus autorisé.", 401);
        Guid? sessionId = null;
        if (user.HasClaim(x => x.Type == "session"))
        {
            if (!Guid.TryParse(user.FindFirstValue("session"), out var id) || !await db.BrowserSessions.AnyAsync(x => x.Id == id && x.DeviceId == deviceId && x.MemberId == memberId && x.RevokedAtUtc == null && x.ExpiresAtUtc > Now, ct))
                throw new DomainException("Ta session a expiré. Reconnecte-toi avec le mobile.", 401);
            sessionId = id;
        }
        var member = await db.Members.AsNoTracking().SingleAsync(x => x.Id == memberId, ct);
        if (admin && !member.IsAdmin) throw new DomainException("Cette action est réservée à l’administrateur familial.", 403);
        return new(memberId, member.IsAdmin, deviceId, sessionId);
    }

    private async Task<Actor> RequireBootstrapActorAsync(ClaimsPrincipal user, CancellationToken ct)
    {
        if (user.Identity?.IsAuthenticated != true || user.FindFirstValue("method") != "browser" ||
            !Guid.TryParse(user.FindFirstValue(ClaimTypes.NameIdentifier), out var memberId) || !Guid.TryParse(user.FindFirstValue("bootstrap-session"), out var sessionId))
            throw new DomainException("Session initiale non autorisée.", 401);
        await using var db = await factory.CreateDbContextAsync(ct);
        var valid = await db.BrowserSessions.AsNoTracking().AnyAsync(x => x.Id == sessionId && x.MemberId == memberId && x.DeviceId == null && x.RevokedAtUtc == null && x.ExpiresAtUtc > Now, ct);
        if (!valid || !await db.Members.AsNoTracking().AnyAsync(x => x.Id == memberId && x.IsAdmin, ct)) throw new DomainException("Ta session a expiré. Demande un lien personnel.", 401);
        return new(memberId, true, Guid.Empty, sessionId);
    }

    public async Task RevokeAsync(Actor actor, Guid deviceId, CancellationToken ct = default)
    {
        await using var db = await factory.CreateDbContextAsync(ct);
        var device = await db.Devices.SingleOrDefaultAsync(x => x.Id == deviceId, ct) ?? throw new DomainException("Téléphone introuvable.", 404);
        if (!actor.IsAdmin && actor.MemberId != device.MemberId) throw new DomainException("Tu ne peux gérer que tes propres téléphones.", 403);
        device.RevokedAtUtc = Now;
        await db.BrowserSessions.Where(x => x.DeviceId == deviceId && x.RevokedAtUtc == null).ExecuteUpdateAsync(s => s.SetProperty(x => x.RevokedAtUtc, Now), ct);
        await db.SaveChangesAsync(ct);
    }
}
