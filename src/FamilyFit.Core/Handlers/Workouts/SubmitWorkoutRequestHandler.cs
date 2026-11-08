using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;
using FamilyFit.Contracts.Models.Workouts;
using FamilyFit.Core.Data;
using FamilyFit.Core.Services;
using Microsoft.EntityFrameworkCore;

namespace FamilyFit.Core.Handlers.Workouts;

public sealed class SubmitWorkoutRequestHandler(IDbContextFactory<FamilyDb> factory, AuthService auth, IMediator mediator, TimeProvider clock)
    : AuthorizedHandler<SubmitWorkoutRequest, WorkoutResultView>(auth, mediator)
{
    protected override async Task<Outcome<WorkoutResultView>> Execute(SubmitWorkoutRequest request, Actor actor, CancellationToken ct)
    {
        var input = request.Input;
        if (input == null || input.Id == Guid.Empty || input.ProgramDayId == Guid.Empty || input.Exercises == null || input.Exercises.Count > 20 || input.Exercises.Any(x => x == null)) return Outcome<WorkoutResultView>.Fail("Résultat de séance invalide.");
        if (input.StartedAtUtc == default || input.FinishedAtUtc < input.StartedAtUtc || input.FinishedAtUtc > clock.GetUtcNow().AddMinutes(5)) return Outcome<WorkoutResultView>.Fail("Les horaires de la séance sont incohérents. Vérifie l’heure du téléphone.");
        var normalized = input with { StartedAtUtc = input.StartedAtUtc.ToUniversalTime(), FinishedAtUtc = input.FinishedAtUtc.ToUniversalTime(), Exercises = input.Exercises.OrderBy(x => x.ExerciseId).ToList() };
        var hash = AuthService.Hash(JsonDefaults.Write(normalized));
        await using var db = await factory.CreateDbContextAsync(ct);
        await using var tx = await db.Database.BeginTransactionAsync(System.Data.IsolationLevel.Serializable, ct);
        var existing = await db.Results.AsNoTracking().SingleOrDefaultAsync(x => x.Id == input.Id, ct);
        if (existing != null)
        {
            if (existing.MemberId != actor.MemberId || existing.PayloadHash != hash) return Outcome<WorkoutResultView>.Fail("Cet identifiant correspond déjà à un autre résultat.", 409);
            return Outcome<WorkoutResultView>.Ok(ReadModels.Result(existing));
        }
        var day = await db.Days.AsNoTracking().SingleOrDefaultAsync(x => x.Id == input.ProgramDayId, ct);
        if (day == null || day.IsRest) return Outcome<WorkoutResultView>.Fail("Cette séance n’existe pas ou correspond à un jour de repos.", 404);
        var program = await db.Programs.AsNoTracking().SingleAsync(x => x.Id == day.ProgramId, ct);
        if (program.MemberId != actor.MemberId || program.State == "draft") return Outcome<WorkoutResultView>.Fail("Cette séance ne fait pas partie de ton programme validé.", 403);
        var prescribed = day.Exercises;
        if (input.Exercises.Select(x => x.ExerciseId).Distinct().Count() != input.Exercises.Count) return Outcome<WorkoutResultView>.Fail("Un exercice apparaît plusieurs fois dans le résultat.");
        foreach (var performance in input.Exercises)
        {
            var target = prescribed.FirstOrDefault(x => x.ExerciseId == performance.ExerciseId);
            if (target == null || performance.CompletedSets < 0 || performance.CompletedSets > target.Sets || performance.Repetitions is < 0 or > 2000 || performance.DurationSeconds is < 0 or > 43200 || performance.WeightKg is < 0 or > 1000 || (performance.WeightKg != null && !double.IsFinite(performance.WeightKg.Value))) return Outcome<WorkoutResultView>.Fail("Le détail d’un exercice est invalide.");
        }
        var completed = prescribed.All(p => input.Exercises.Any(x => x.ExerciseId == p.ExerciseId && !x.Skipped && x.CompletedSets == p.Sets));
        var status = completed ? "completed" : input.Exercises.Any(x => x.CompletedSets > 0) ? "partial" : "abandoned";
        var snapshot = ReadModels.Day(day, await db.Exercises.AsNoTracking().ToListAsync(ct), await db.Locations.AsNoTracking().ToListAsync(ct));
        var result = new WorkoutResult { Id = input.Id, MemberId = actor.MemberId, ProgramDayId = day.Id, StartedAtUtc = normalized.StartedAtUtc, FinishedAtUtc = normalized.FinishedAtUtc, ReceivedAtUtc = clock.GetUtcNow(), Status = status, PerformanceJson = JsonDefaults.Write(normalized.Exercises), SnapshotJson = JsonDefaults.Write(snapshot), PayloadHash = hash };
        db.Results.Add(result);
        await db.SaveChangesAsync(ct); await tx.CommitAsync(ct);
        return Outcome<WorkoutResultView>.Ok(ReadModels.Result(result));
    }
}
