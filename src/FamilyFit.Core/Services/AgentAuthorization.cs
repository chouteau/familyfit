using System.Security.Claims;
using System.Security.Cryptography;
using System.Text;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;
using FamilyFit.Core.Data;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;

namespace FamilyFit.Core.Services;

public sealed class AgentAuthorization(IDbContextFactory<FamilyDb> factory, IConfiguration configuration)
{
    public bool IsConfigured => configuration["MCP_API_KEY"]?.Length is >= 32 and <= 512;
    private string KeyVersion => AuthService.Hash(configuration["MCP_API_KEY"] ?? "");
    public async Task<ClaimsPrincipal?> AuthenticateAsync(string token, CancellationToken ct = default)
    {
        if (!IsConfigured || token.Length is < 32 or > 512 || !CryptographicOperations.FixedTimeEquals(SHA256.HashData(Encoding.UTF8.GetBytes(token)), SHA256.HashData(Encoding.UTF8.GetBytes(configuration["MCP_API_KEY"]!)))) return null;
        await using var db = await factory.CreateDbContextAsync(ct);
        var admins = await db.Members.AsNoTracking().Where(x => x.IsAdmin).Take(2).ToListAsync(ct);
        if (admins.Count != 1) return null;
        var member = admins[0];
        var claims = new List<Claim> { new(ClaimTypes.NameIdentifier, member.Id.ToString()), new(ClaimTypes.Name, member.Nickname), new("agent", "server-key"), new("agent-key-version", KeyVersion), new("method", "agent"), new(ClaimTypes.Role, "Admin") };
        return new ClaimsPrincipal(new ClaimsIdentity(claims, "Agent"));
    }

    public async Task<Actor> RequireActorAsync(ClaimsPrincipal user, bool admin, CancellationToken ct)
    {
        if (!IsConfigured || user.Identity?.IsAuthenticated != true || user.FindFirstValue("method") != "agent" || user.FindFirstValue("agent") != "server-key" || user.FindFirstValue("agent-key-version") != KeyVersion || !Guid.TryParse(user.FindFirstValue(ClaimTypes.NameIdentifier), out var memberId))
            throw new DomainException("Accès agent non autorisé.", 401);
        await using var db = await factory.CreateDbContextAsync(ct);
        var admins = await db.Members.AsNoTracking().Where(x => x.IsAdmin).Take(2).ToListAsync(ct);
        if (admins.Count != 1 || admins[0].Id != memberId) throw new DomainException("Le profil administrateur a changé.", 401);
        return new Actor(memberId, true, Guid.Empty, null, Guid.Empty, !configuration.GetValue<bool>("MCP_READ_ONLY"));
    }

    public static void RequireBrowser(ClaimsPrincipal user)
    {
        if (user.FindFirstValue("method") != "browser") throw new DomainException("Crée les liens web depuis le site après confirmation sur ton mobile.", 403);
    }
}
