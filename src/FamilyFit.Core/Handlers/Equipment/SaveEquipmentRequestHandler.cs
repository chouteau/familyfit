using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models.Equipment;
using FamilyFit.Core.Data;
using FamilyFit.Core.Services;
using Microsoft.EntityFrameworkCore;

namespace FamilyFit.Core.Handlers.Equipment;

public sealed class SaveEquipmentRequestHandler(IDbContextFactory<FamilyDb> factory, AuthService auth, IMediator mediator)
    : AuthorizedHandler<SaveEquipmentRequest, EquipmentDefinition>(auth, mediator)
{
    protected override async Task<Outcome<EquipmentDefinition>> Execute(SaveEquipmentRequest request, Actor actor, CancellationToken ct)
    {
        if (actor.AgentId == null) return Outcome<EquipmentDefinition>.Fail("Le catalogue se modifie uniquement via le serveur MCP.", 403);
        var input = request.Input;
        if (input == null || string.IsNullOrWhiteSpace(input.Id) || !System.Text.RegularExpressions.Regex.IsMatch(input.Id, "^[A-Z][A-Z0-9_]{0,63}$")
            || string.IsNullOrWhiteSpace(input.Name) || input.Name.Length > 80 || string.IsNullOrWhiteSpace(input.Description) || input.Description.Length > 2000
            || input.TargetMuscles == null || input.TargetMuscles.Length is < 1 or > 12 || input.TargetMuscles.Any(x => string.IsNullOrWhiteSpace(x) || x.Length > 60)
            || input.Revision < 0)
            return Outcome<EquipmentDefinition>.Fail("Vérifie l’identifiant (64 caractères A-Z, 0-9, _), le nom (80), la description (2000) et les zones travaillées (1 à 12, 60 caractères chacune).");
        await using var db = await factory.CreateDbContextAsync(ct);
        var equipment = await db.Equipment.SingleOrDefaultAsync(x => x.Id == input.Id, ct);
        if (equipment == null && input.Revision != 0) return Outcome<EquipmentDefinition>.Fail("Équipement introuvable.", 404);
        if (equipment != null && equipment.Revision != input.Revision) return Outcome<EquipmentDefinition>.Fail("La fiche a changé. Relis le catalogue avant de réessayer.", 409);
        if (equipment == null)
        {
            equipment = new EquipmentDefinition { Id = input.Id, Revision = 0 };
            db.Equipment.Add(equipment);
        }
        equipment.Name = input.Name.Trim();
        equipment.Description = input.Description.Trim();
        equipment.TargetMuscles = input.TargetMuscles.Select(x => x.Trim()).Distinct(StringComparer.OrdinalIgnoreCase).ToArray();
        equipment.Revision++;
        try { await db.SaveChangesAsync(ct); }
        catch (DbUpdateException ex) when (ex.InnerException is Microsoft.Data.Sqlite.SqliteException { SqliteErrorCode: 19 })
        { return Outcome<EquipmentDefinition>.Fail("Cet identifiant existe déjà. Relis le catalogue avant de réessayer.", 409); }
        return Outcome<EquipmentDefinition>.Ok(equipment);
    }
}
