using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;
using FamilyFit.Contracts.Models.Agents;
using FamilyFit.Core.Data;
using FamilyFit.Core.Services;
using Microsoft.EntityFrameworkCore;

namespace FamilyFit.Core.Handlers.Agents;

public sealed class GetAgentDataRequestHandler(IDbContextFactory<FamilyDb> factory, AuthService auth, IMediator mediator, TimeProvider clock)
    : AuthorizedHandler<GetAgentDataRequest, object>(auth, mediator)
{
    protected override async Task<Outcome<object>> Execute(GetAgentDataRequest request, Actor actor, CancellationToken ct)
    {
        var q = request.Query;
        if (q == null || q.Skip is < 0 or > 100000 || q.Take is < 1 or > 100 || q.Search?.Length > 80)
            return Outcome<object>.Fail("Pagination invalide : 1 à 100 éléments par page, recherche de 80 caractères maximum.");
        var today = FamilyCalendar.Today(clock);
        var from = q.From ?? today.AddDays(-30);
        var to = q.To ?? (q.Resource == "dashboard" ? q.From ?? today : today.AddDays(30));
        if (q.Resource == "dashboard") from = q.From ?? today;
        if (from.DayNumber < 6 || to == DateOnly.MaxValue || from > to || to.DayNumber - from.DayNumber > 366) return Outcome<object>.Fail("Choisis une période valide de 366 jours maximum, dans l’ordre chronologique.");
        await using var db = await factory.CreateDbContextAsync(ct);
        // All queries below belong to the new MCP read path; historical web/mobile queries are unchanged.
        switch (q.Resource)
        {
            case "members":
            {
                var rows = await db.Members.AsNoTracking().Where(x => (q.MemberId == null || x.Id == q.MemberId) && (q.Search == null || x.Nickname.Contains(q.Search)))
                    .OrderBy(x => x.Nickname).ThenBy(x => x.Id).Skip(q.Skip).Take(q.Take + 1).ToListAsync(ct);
                return Page(q, rows);
            }
            case "member":
            {
                var member = await db.Members.AsNoTracking().SingleOrDefaultAsync(x => x.Id == q.MemberId, ct);
                if (member == null) return Outcome<object>.Fail("Membre introuvable.", 404);
                var weights = await db.Weights.AsNoTracking().Where(x => x.MemberId == member.Id).OrderByDescending(x => x.RecordedAtUtc).Take(20).ToListAsync(ct);
                return Outcome<object>.Ok(new { member, recentWeights = weights, weightHistoryLimit = 20 });
            }
            case "locations":
            {
                var rows = await db.Locations.AsNoTracking().Where(x => q.MemberId == null || x.MemberId == q.MemberId)
                    .OrderBy(x => x.Name).ThenBy(x => x.Id).Skip(q.Skip).Take(q.Take + 1).ToListAsync(ct);
                return Page(q, rows);
            }
            case "exercises":
            {
                var rows = await db.Exercises.AsNoTracking().Where(x => (q.EquipmentId == null || x.EquipmentId == q.EquipmentId) && (q.Search == null || x.Name.Contains(q.Search)))
                    .OrderBy(x => x.Name).ThenBy(x => x.Id).Skip(q.Skip).Take(q.Take + 1).ToListAsync(ct);
                return Page(q, rows);
            }
            case "programs":
            {
                var rows = await db.Programs.AsNoTracking().Include(x => x.Days)
                    .Where(x => (actor.IsAdmin || x.State != "draft") && (q.MemberId == null || x.MemberId == q.MemberId) && (q.State == null || x.State == q.State) && x.WeekStart >= from.AddDays(-6) && x.WeekStart <= to)
                    .OrderByDescending(x => x.WeekStart).ThenBy(x => x.Id).Skip(q.Skip).Take(q.Take + 1).ToListAsync(ct);
                return Page(q, rows);
            }
            case "program":
            {
                var row = await db.Programs.AsNoTracking().Include(x => x.Days).SingleOrDefaultAsync(x => x.Id == q.Id && (actor.IsAdmin || x.State != "draft"), ct);
                return row == null ? Outcome<object>.Fail("Programme introuvable ou non accessible.", 404) : Outcome<object>.Ok(row);
            }
            case "workouts":
            {
                var start = ParisInstant(from); var end = ParisInstant(to.AddDays(1));
                var rows = await db.Results.AsNoTracking().Where(x => (q.MemberId == null || x.MemberId == q.MemberId) && x.FinishedAtUtc >= start && x.FinishedAtUtc < end)
                    .OrderByDescending(x => x.FinishedAtUtc).ThenBy(x => x.Id).Skip(q.Skip).Take(q.Take + 1).ToListAsync(ct);
                return Page(q, rows.Select(ReadModels.Result).ToList());
            }
            case "workout":
            {
                var row = await db.Results.AsNoTracking().SingleOrDefaultAsync(x => x.Id == q.Id, ct);
                return row == null ? Outcome<object>.Fail("Résultat introuvable.", 404) : Outcome<object>.Ok(new { result = ReadModels.Result(row), prescription = JsonDefaults.Read<SessionDay>(row.SnapshotJson) });
            }
            case "weights":
            {
                var start = ParisInstant(from); var end = ParisInstant(to.AddDays(1));
                var rows = await db.Weights.AsNoTracking().Where(x => (q.MemberId == null || x.MemberId == q.MemberId) && x.RecordedAtUtc >= start && x.RecordedAtUtc < end)
                    .OrderByDescending(x => x.RecordedAtUtc).ThenBy(x => x.Id).Skip(q.Skip).Take(q.Take + 1).ToListAsync(ct);
                return Page(q, rows);
            }
            case "planning":
            case "dashboard":
                return await Planning(db, q, q.Resource == "dashboard" ? q.From ?? today : from, q.Resource == "dashboard" ? q.From ?? today : to, ct);
            default: return Outcome<object>.Fail("Lecture MCP inconnue.");
        }
    }

    private static Outcome<object> Page<T>(AgentQuery q, List<T> rows) => Outcome<object>.Ok(new AgentPage(q.Resource, rows.Take(q.Take).ToList(), q.Skip, q.Take, rows.Count > q.Take));
    private static DateTimeOffset ParisInstant(DateOnly date) => new(TimeZoneInfo.ConvertTimeToUtc(date.ToDateTime(TimeOnly.MinValue), FamilyCalendar.Paris), TimeSpan.Zero);

    private static async Task<Outcome<object>> Planning(FamilyDb db, AgentQuery q, DateOnly from, DateOnly to, CancellationToken ct)
    {
        var query = db.Days.AsNoTracking().Join(db.Programs.AsNoTracking().Where(x => x.State == "published" && (q.MemberId == null || x.MemberId == q.MemberId)), day => day.ProgramId, program => program.Id, (day, program) => new { Day = day, program.MemberId })
            .Where(x => x.Day.Date >= from && x.Day.Date <= to).OrderBy(x => x.Day.Date).ThenBy(x => x.Day.Id);
        var isDashboard = q.Resource == "dashboard";
        var rows = await query.Skip(isDashboard ? 0 : q.Skip).Take(isDashboard ? 1001 : q.Take + 1).ToListAsync(ct);
        if (isDashboard && rows.Count > 1000) return Outcome<object>.Fail("Plus de 1000 séances pour cette date : filtre par membre.");
        var visible = isDashboard ? rows : rows.Take(q.Take).ToList();
        var dayIds = visible.Select(x => x.Day.Id).ToList();
        var exerciseIds = visible.SelectMany(x => x.Day.Exercises).Select(x => x.ExerciseId).Distinct().ToList();
        var locationIds = visible.Select(x => x.Day.LocationId).OfType<Guid>().Distinct().ToList();
        var memberIds = visible.Select(x => x.MemberId).Distinct().ToList();
        var catalogue = await db.Exercises.AsNoTracking().Where(x => exerciseIds.Contains(x.Id)).ToListAsync(ct);
        var locations = await db.Locations.AsNoTracking().Where(x => locationIds.Contains(x.Id)).ToListAsync(ct);
        var members = await db.Members.AsNoTracking().Where(x => memberIds.Contains(x.Id)).ToListAsync(ct);
        var statuses = await db.Results.AsNoTracking().Where(x => dayIds.Contains(x.ProgramDayId)).GroupBy(x => x.ProgramDayId)
            .Select(group => new { Id = group.Key, Completed = group.Any(x => x.Status == "completed"), Partial = group.Any(x => x.Status == "partial"), HasResults = group.Any() }).ToListAsync(ct);
        var items = visible.Select(x =>
        {
            var status = statuses.FirstOrDefault(s => s.Id == x.Day.Id);
            return new { memberId = x.MemberId, nickname = members.First(m => m.Id == x.MemberId).Nickname, day = ReadModels.Day(x.Day, catalogue, locations), status = x.Day.IsRest ? "rest" : status?.Completed == true ? "completed" : status?.Partial == true ? "partial" : status?.HasResults == true ? "abandoned" : "not_recorded" };
        }).ToList();
        return isDashboard
            ? Outcome<object>.Ok(new { date = from, planned = items.Count(x => !x.day.IsRest), completed = items.Count(x => x.status == "completed"), partial = items.Count(x => x.status == "partial"), abandoned = items.Count(x => x.status == "abandoned"), rest = items.Count(x => x.day.IsRest), items, note = "Les jours de repos sont exclus. Une séance hors ligne n’est visible qu’après sa synchronisation." })
            : Outcome<object>.Ok(new AgentPage(q.Resource, items, q.Skip, q.Take, rows.Count > q.Take));
    }
}
