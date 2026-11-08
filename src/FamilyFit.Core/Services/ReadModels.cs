using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;
using FamilyFit.Core.Data;
using Microsoft.EntityFrameworkCore;

namespace FamilyFit.Core.Services;

public static class ReadModels
{
    public static WorkoutResultView Result(WorkoutResult item) => new(item.Id, item.MemberId, item.ProgramDayId, item.StartedAtUtc, item.FinishedAtUtc, item.Status, JsonDefaults.Read<List<ExercisePerformance>>(item.PerformanceJson));
    public static SessionDay Day(ProgramDay day, IReadOnlyList<Exercise> catalogue, IReadOnlyList<Location> locations) => new(
        day.Id, day.ProgramId, day.Date, day.Title, day.LocationId, locations.FirstOrDefault(x => x.Id == day.LocationId)?.Name ?? "Repos", day.DurationMinutes, day.IsRest,
        day.Exercises.Select(p => { var e = catalogue.Single(x => x.Id == p.ExerciseId); return new SessionExercise(e.Id, e.Name, e.EquipmentId, e.Muscle, e.TargetType, e.Instructions, e.PostureTip, e.ImageUrl, e.VideoUrl, p.Sets, p.Target, p.RestSeconds); }).ToList());

    public static async Task<DashboardData> Dashboard(FamilyDb db, bool includeDrafts, CancellationToken ct)
    {
        var members = await db.Members.AsNoTracking().OrderBy(x => x.Nickname).ToListAsync(ct);
        var locations = await db.Locations.AsNoTracking().OrderBy(x => x.Name).ToListAsync(ct);
        var programs = await db.Programs.AsNoTracking().Include(x => x.Days).Where(x => includeDrafts || x.State == "published" || x.State == "archived").ToListAsync(ct);
        var results = await db.Results.AsNoTracking().OrderByDescending(x => x.FinishedAtUtc).ToListAsync(ct);
        var exercises = await db.Exercises.AsNoTracking().OrderBy(x => x.Name).ToListAsync(ct);
        var weights = await db.Weights.AsNoTracking().OrderBy(x => x.RecordedAtUtc).ToListAsync(ct);
        return new(members, locations, programs, results.Select(Result).ToList(), exercises, weights);
    }
}
