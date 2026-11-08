using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;
using FamilyFit.Contracts.Models.Exercises;
using FamilyFit.Core.Data;
using FamilyFit.Core.Services;
using Microsoft.EntityFrameworkCore;
namespace FamilyFit.Core.Handlers.Exercises;

public sealed class CreateExerciseRequestHandler(IDbContextFactory<FamilyDb> factory, AuthService auth, IMediator mediator)
    : AuthorizedHandler<CreateExerciseRequest, Exercise>(auth, mediator)
{
    protected override async Task<Outcome<Exercise>> Execute(CreateExerciseRequest request, Actor actor, CancellationToken ct)
    {
        if (actor.AgentId == null) return Outcome<Exercise>.Fail("Le catalogue se modifie uniquement via le serveur MCP.", 403);
        var i = request.Input;
        if (i == null || string.IsNullOrWhiteSpace(i.Id) || !System.Text.RegularExpressions.Regex.IsMatch(i.Id, "^[a-z][a-z0-9_]{0,63}$")
            || string.IsNullOrWhiteSpace(i.Name) || i.Name.Length > 100 || string.IsNullOrWhiteSpace(i.EquipmentId) || i.EquipmentId.Length > 64
            || string.IsNullOrWhiteSpace(i.Muscle) || i.Muscle.Length > 200 || i.TargetType is not ("time" or "repetitions")
            || string.IsNullOrWhiteSpace(i.Instructions) || i.Instructions.Length > 2000 || string.IsNullOrWhiteSpace(i.PostureTip) || i.PostureTip.Length > 1000)
            return Outcome<Exercise>.Fail("Vérifie les champs : id (64 caractères a-z, 0-9, _), nom (100), matériel (64), muscles (200), type time/repetitions, instructions (2000), posture (1000).");
        await using var db = await factory.CreateDbContextAsync(ct);
        if (await db.Exercises.AnyAsync(x => x.Id == i.Id, ct)) return Outcome<Exercise>.Fail("Cet identifiant existe déjà ; aucun exercice modifié.", 409);
        if (!await db.Equipment.AnyAsync(x => x.Id == i.EquipmentId, ct)) return Outcome<Exercise>.Fail("Matériel inconnu : consulte list_equipment.");
        var exercise = new Exercise { Id = i.Id, Name = i.Name.Trim(), EquipmentId = i.EquipmentId, Muscle = i.Muscle.Trim(), TargetType = i.TargetType, Instructions = i.Instructions.Trim(), PostureTip = i.PostureTip.Trim() };
        db.Exercises.Add(exercise);
        try { await db.SaveChangesAsync(ct); }
        catch (DbUpdateException ex) when (ex.InnerException is Microsoft.Data.Sqlite.SqliteException { SqliteErrorCode: 19 })
        { return Outcome<Exercise>.Fail("Cet identifiant existe déjà ; relis le catalogue.", 409); }
        return Outcome<Exercise>.Ok(exercise);
    }
}
