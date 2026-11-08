using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;
using FamilyFit.Contracts.Models.Locations;
using FamilyFit.Core.Data;
using FamilyFit.Core.Services;
using Microsoft.EntityFrameworkCore;

namespace FamilyFit.Core.Handlers.Locations;

public sealed class SaveLocationRequestHandler(IDbContextFactory<FamilyDb> factory, AuthService auth, IMediator mediator)
    : AuthorizedHandler<SaveLocationRequest, Location>(auth, mediator)
{
    protected override async Task<Outcome<Location>> Execute(SaveLocationRequest request, Actor actor, CancellationToken ct)
    {
        var input = request.Input;
        if (input == null || input.MemberId == Guid.Empty || string.IsNullOrWhiteSpace(input.Name) || input.Name.Length > 80 || input.EquipmentIds == null || input.EquipmentIds.Any(x => !EquipmentCatalogue.All.ContainsKey(x))) return Outcome<Location>.Fail("Vérifie le membre, le nom du lieu et le matériel.");
        if (!actor.IsAdmin && input.MemberId != actor.MemberId) return Outcome<Location>.Fail("Tu ne peux gérer que tes propres lieux.", 403);
        await using var db = await factory.CreateDbContextAsync(ct);
        if (!await db.Members.AnyAsync(x => x.Id == input.MemberId, ct)) return Outcome<Location>.Fail("Membre introuvable.", 404);
        var location = request.Id == null ? new Location { MemberId = input.MemberId } : await db.Locations.SingleOrDefaultAsync(x => x.Id == request.Id, ct);
        if (location == null) return Outcome<Location>.Fail("Lieu introuvable.", 404);
        if (location.MemberId != input.MemberId) return Outcome<Location>.Fail("Un lieu ne peut pas changer de propriétaire.", 403);
        if (request.Id != null && location.Revision != input.Revision) return Outcome<Location>.Fail("Le lieu a changé. Recharge les informations.", 409);
        location.Name = input.Name.Trim(); location.EquipmentIds = string.Join(',', input.EquipmentIds.Append("BODYWEIGHT").Distinct()); location.Revision++;
        if (request.Id == null) db.Locations.Add(location);
        await db.SaveChangesAsync(ct);
        return Outcome<Location>.Ok(location);
    }
}
