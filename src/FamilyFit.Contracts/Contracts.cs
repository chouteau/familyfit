using System.ComponentModel.DataAnnotations;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Contracts;

public sealed record Actor(Guid MemberId, bool IsAdmin, Guid DeviceId, Guid? SessionId = null, Guid? AgentId = null, bool CanWrite = true);
public sealed record EnrollRequest([property: Required, StringLength(32)] string Code, [property: Required, StringLength(80)] string DeviceName);
public sealed record DeviceGrant(string Token, Guid DeviceId, Member Member);
public sealed record InvitationGrant(string Code, string QrPayload, DateTimeOffset ExpiresAtUtc);
public sealed record ChallengeGrant(Guid Id, string Code, string QrPayload, DateTimeOffset ExpiresAtUtc);
public sealed record ApproveRequest([property: Required, StringLength(32)] string Code);
public sealed record MemberInput(string Nickname, int Age, double WeightKg, string Goal, string Level, int SessionMinutes, string TrainingDays, string ExcludedExerciseIds, long Revision = 1);
public sealed record LocationInput(Guid MemberId, string Name, string[] EquipmentIds, long Revision = 1);
public sealed record ProgramDayInput(DateOnly Date, string Title, Guid? LocationId, int DurationMinutes, bool IsRest, List<Prescription> Exercises);
public sealed record ProgramInput(Guid MemberId, string Title, DateOnly WeekStart, List<ProgramDayInput> Days);
public sealed record WorkoutSubmission(Guid Id, Guid ProgramDayId, DateTimeOffset StartedAtUtc, DateTimeOffset FinishedAtUtc, List<ExercisePerformance> Exercises);
public sealed record SessionExercise(string Id, string Name, string EquipmentId, string Muscle, string TargetType, string Instructions, string PostureTip, string? ImageUrl, string? VideoUrl, int Sets, int Target, int RestSeconds);
public sealed record SessionDay(Guid Id, Guid ProgramId, DateOnly Date, string Title, Guid? LocationId, string LocationName, int DurationMinutes, bool IsRest, List<SessionExercise> Exercises);
public sealed record SyncSnapshot(DateTimeOffset ServerTimeUtc, Member Member, List<Member> Members, List<Location> Locations, List<Exercise> Exercises, List<SessionDay> Days, List<WorkoutResultView> Results, List<WeightEntry> Weights);
public sealed record WorkoutResultView(Guid Id, Guid MemberId, Guid ProgramDayId, DateTimeOffset StartedAtUtc, DateTimeOffset FinishedAtUtc, string Status, List<ExercisePerformance> Exercises);
public sealed record DashboardData(List<Member> Members, List<Location> Locations, List<WorkoutProgram> Programs, List<WorkoutResultView> Results, List<Exercise> Exercises, List<WeightEntry> Weights);
public sealed record DeviceView(Guid Id, Guid MemberId, string Name, DateTimeOffset CreatedAtUtc, DateTimeOffset? RevokedAtUtc);
public sealed class DomainException(string message, int status = 400) : Exception(message)
{
    public int Status { get; } = status;
}

public static class FamilyCalendar
{
    public static readonly TimeZoneInfo Paris = TimeZoneInfo.FindSystemTimeZoneById("Europe/Paris");
    public static DateOnly Today(TimeProvider clock) => DateOnly.FromDateTime(TimeZoneInfo.ConvertTimeFromUtc(clock.GetUtcNow().UtcDateTime, Paris));
    public static DateOnly Monday(DateOnly date) => date.AddDays(-(((int)date.DayOfWeek + 6) % 7));
    public static string[] Split(string value) => value.Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);
}

public static class EquipmentCatalogue
{
    public static readonly Dictionary<string, string> All = new()
    {
        ["BODYWEIGHT"] = "Poids du corps", ["ELLIPTICAL"] = "Vélo elliptique", ["BALANCE_BOARD"] = "Plateau d’équilibre",
        ["PARALLEL_BARS"] = "Barres parallèles", ["BACK_BENCH"] = "Banc à dorsaux", ["AB_BENCH"] = "Banc à abdominaux",
        ["PLYO_BOX"] = "Cube pliométrique", ["DUMBBELLS"] = "Haltères", ["LAT_PULLDOWN"] = "Poulie haute",
        ["CHEST_PRESS"] = "Presse à pectoraux", ["SEATED_ROW"] = "Rowing assis", ["LEG_PRESS"] = "Presse à cuisses",
        ["TREADMILL"] = "Tapis de course", ["BIKE"] = "Vélo", ["MAT"] = "Tapis de sol", ["RESISTANCE_BAND"] = "Élastique"
    };
}
