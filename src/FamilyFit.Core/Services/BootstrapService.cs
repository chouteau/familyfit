using System.Data;
using System.Security.Cryptography;
using System.Text;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;
using FamilyFit.Contracts.Models.Authentication;
using FamilyFit.Core.Data;
using FamilyFit.Core.Validators;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;

namespace FamilyFit.Core.Services;

public sealed class BootstrapService(IDbContextFactory<FamilyDb> factory, IConfiguration configuration, TimeProvider clock)
{
    private bool IsConfigured => configuration["FAMILYFIT_BOOTSTRAP_CODE"]?.Length is >= 16 and <= 128;
    private static async Task<bool> IsUnused(FamilyDb db, CancellationToken ct) =>
        !await db.AdminBootstraps.AnyAsync(ct) && !await db.BrowserSessions.AnyAsync(ct);

    public async Task<BootstrapStatus> StatusAsync(CancellationToken ct)
    {
        if (!IsConfigured) return new(false, false);
        await using var db = await factory.CreateDbContextAsync(ct);
        if (!await IsUnused(db, ct)) return new(false, false);
        var admins = await db.Members.AsNoTracking().Where(x => x.IsAdmin).Select(x => x.Id).Take(2).ToListAsync(ct);
        return new(admins.Count <= 1, admins.Count == 0);
    }

    public async Task<BootstrapGrant> CompleteAsync(BootstrapInput input, CancellationToken ct)
    {
        if (!IsConfigured || input?.Code == null || input.Code.Length is < 16 or > 128 ||
            !CryptographicOperations.FixedTimeEquals(SHA256.HashData(Encoding.UTF8.GetBytes(input.Code)), SHA256.HashData(Encoding.UTF8.GetBytes(configuration["FAMILYFIT_BOOTSTRAP_CODE"]!))))
            throw new DomainException("Code d’initialisation incorrect ou indisponible.", 403);
        await using var db = await factory.CreateDbContextAsync(ct);
        await using var transaction = await db.Database.BeginTransactionAsync(IsolationLevel.Serializable, ct);
        if (!await IsUnused(db, ct)) throw new DomainException("L’initialisation a déjà été effectuée. Utilise un lien personnel.", 409);
        var admins = await db.Members.Where(x => x.IsAdmin).Take(2).ToListAsync(ct);
        if (admins.Count > 1) throw new DomainException("L’administrateur ne peut pas être identifié.", 409);
        var member = admins.SingleOrDefault();
        if (member == null)
        {
            var profile = new MemberInput(input.Nickname ?? "", input.Age, input.WeightKg, "Retrouver la forme", "Débutant", 30, "1,3,5", "");
            var error = MemberValidator.Validate(profile);
            if (error != null) throw new DomainException(error);
            member = new Member { Nickname = profile.Nickname.Trim(), Age = profile.Age, WeightKg = profile.WeightKg, IsAdmin = true };
            db.Members.Add(member);
            db.Weights.Add(new WeightEntry { MemberId = member.Id, WeightKg = member.WeightKg, RecordedAtUtc = clock.GetUtcNow() });
        }
        var session = new BrowserSession { MemberId = member.Id, DeviceId = null, ExpiresAtUtc = clock.GetUtcNow().AddHours(12) };
        db.AdminBootstraps.Add(new AdminBootstrap { MemberId = member.Id, CompletedAtUtc = clock.GetUtcNow() });
        db.BrowserSessions.Add(session);
        await db.SaveChangesAsync(ct);
        await transaction.CommitAsync(ct);
        return new(member, session);
    }
}
