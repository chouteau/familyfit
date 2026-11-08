using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace FamilyFit.Contracts.Models;

public static class JsonDefaults
{
    public static readonly JsonSerializerOptions Options = new(JsonSerializerDefaults.Web);
    public static string Write<T>(T value) => JsonSerializer.Serialize(value, Options);
    public static T Read<T>(string value) => JsonSerializer.Deserialize<T>(value, Options)!;
}

public sealed class Member
{
    public Guid Id { get; set; } = Guid.NewGuid();
    [Required, StringLength(40, MinimumLength = 2)] public string Nickname { get; set; } = "";
    [Range(1, 120)] public int Age { get; set; } = 18;
    [Range(10, 500)] public double WeightKg { get; set; } = 70;
    [StringLength(500)] public string Goal { get; set; } = "Retrouver la forme";
    public string Level { get; set; } = "Débutant";
    [Range(5, 180)] public int SessionMinutes { get; set; } = 30;
    public string TrainingDays { get; set; } = "1,3,5";
    public string ExcludedExerciseIds { get; set; } = "";
    public string AvatarColor { get; set; } = "green";
    public bool IsAdmin { get; set; }
    public long Revision { get; set; } = 1;
    public DateTimeOffset? LastSyncUtc { get; set; }
}

public sealed class WeightEntry
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid MemberId { get; set; }
    public DateTimeOffset RecordedAtUtc { get; set; }
    public double WeightKg { get; set; }
}

public sealed class Location
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid MemberId { get; set; }
    [Required, StringLength(80)] public string Name { get; set; } = "";
    public string EquipmentIds { get; set; } = "BODYWEIGHT";
    public long Revision { get; set; } = 1;
}

public sealed class Exercise
{
    [Key] public string Id { get; set; } = "";
    public string Name { get; set; } = "";
    public string EquipmentId { get; set; } = "BODYWEIGHT";
    public string Muscle { get; set; } = "Corps entier";
    public string TargetType { get; set; } = "time";
    public string Instructions { get; set; } = "";
    public string PostureTip { get; set; } = "";
    public string? ImageUrl { get; set; }
    public string? VideoUrl { get; set; }
}

public sealed class WorkoutProgram
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid MemberId { get; set; }
    public string Title { get; set; } = "Ma semaine";
    public DateOnly WeekStart { get; set; }
    public string State { get; set; } = "draft";
    public int Version { get; set; } = 1;
    public DateTimeOffset CreatedAtUtc { get; set; }
    public Guid? ApprovedBy { get; set; }
    public DateTimeOffset? PublishedAtUtc { get; set; }
    public List<ProgramDay> Days { get; set; } = [];
}

public sealed class ProgramDay
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid ProgramId { get; set; }
    public DateOnly Date { get; set; }
    public string Title { get; set; } = "Renforcement";
    public Guid? LocationId { get; set; }
    public int DurationMinutes { get; set; } = 30;
    public bool IsRest { get; set; }
    [JsonIgnore] public string ExercisesJson { get; set; } = "[]";
    [NotMapped] public List<Prescription> Exercises
    {
        get => JsonDefaults.Read<List<Prescription>>(ExercisesJson);
        set => ExercisesJson = JsonDefaults.Write(value);
    }
}

public sealed record Prescription(string ExerciseId, int Sets = 3, int Target = 40, int RestSeconds = 20);
public sealed record ExercisePerformance(string ExerciseId, int CompletedSets, int? Repetitions = null, int? DurationSeconds = null, double? WeightKg = null, bool Skipped = false);

public sealed class WorkoutResult
{
    public Guid Id { get; set; }
    public Guid MemberId { get; set; }
    public Guid ProgramDayId { get; set; }
    public DateTimeOffset StartedAtUtc { get; set; }
    public DateTimeOffset FinishedAtUtc { get; set; }
    public DateTimeOffset ReceivedAtUtc { get; set; }
    public string Status { get; set; } = "abandoned";
    public string PerformanceJson { get; set; } = "[]";
    public string SnapshotJson { get; set; } = "{}";
    public string PayloadHash { get; set; } = "";
}

public sealed class Device
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid MemberId { get; set; }
    public string Name { get; set; } = "Mon téléphone";
    [JsonIgnore] public string TokenHash { get; set; } = "";
    public DateTimeOffset CreatedAtUtc { get; set; }
    public DateTimeOffset? RevokedAtUtc { get; set; }
}

public sealed class Invitation
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid MemberId { get; set; }
    [JsonIgnore] public string CodeHash { get; set; } = "";
    public DateTimeOffset ExpiresAtUtc { get; set; }
    public DateTimeOffset? UsedAtUtc { get; set; }
}

public sealed class LoginChallenge
{
    public Guid Id { get; set; } = Guid.NewGuid();
    [JsonIgnore] public string CodeHash { get; set; } = "";
    [JsonIgnore] public string BrowserSecretHash { get; set; } = "";
    public DateTimeOffset ExpiresAtUtc { get; set; }
    public Guid? ApprovedDeviceId { get; set; }
    public DateTimeOffset? UsedAtUtc { get; set; }
}

public sealed class BrowserSession
{
    public Guid Id { get; set; } = Guid.NewGuid();
    public Guid MemberId { get; set; }
    public Guid DeviceId { get; set; }
    public DateTimeOffset ExpiresAtUtc { get; set; }
    public DateTimeOffset? RevokedAtUtc { get; set; }
}
