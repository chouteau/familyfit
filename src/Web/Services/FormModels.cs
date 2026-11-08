using System.ComponentModel.DataAnnotations;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Web.Services;

public sealed class MemberForm
{
    public Guid? Id { get; set; }
    [Required(ErrorMessage = "Choisis un pseudo."), StringLength(40, MinimumLength = 2, ErrorMessage = "Le pseudo doit contenir entre 2 et 40 caractères.")] public string Nickname { get; set; } = "";
    [Range(1, 120, ErrorMessage = "Indique un âge entre 1 et 120 ans.")] public int Age { get; set; } = 18;
    [Range(10, 500, ErrorMessage = "Indique un poids entre 10 et 500 kg.")] public double WeightKg { get; set; } = 70;
    [Required, StringLength(500)] public string Goal { get; set; } = "Retrouver la forme";
    public string Level { get; set; } = "Débutant";
    [Range(5, 180, ErrorMessage = "Choisis une durée entre 5 et 180 minutes.")] public int SessionMinutes { get; set; } = 30;
    public HashSet<int> Days { get; set; } = [1, 3, 5];
    public HashSet<string> Exclusions { get; set; } = [];
    public long Revision { get; set; } = 1;
    public static MemberForm From(Member m) => new() { Id = m.Id, Nickname = m.Nickname, Age = m.Age, WeightKg = m.WeightKg, Goal = m.Goal, Level = m.Level, SessionMinutes = m.SessionMinutes, Days = FamilyCalendar.Split(m.TrainingDays).Select(int.Parse).ToHashSet(), Exclusions = FamilyCalendar.Split(m.ExcludedExerciseIds).ToHashSet(), Revision = m.Revision };
    public MemberInput Input() => new(Nickname, Age, WeightKg, Goal, Level, SessionMinutes, string.Join(',', Days.Order()), string.Join(',', Exclusions.Order()), Revision);
}
public sealed class LocationForm
{
    public Guid? Id { get; set; }
    public Guid MemberId { get; set; }
    [Required(ErrorMessage = "Donne un nom au lieu."), StringLength(80)] public string Name { get; set; } = "";
    public HashSet<string> Equipment { get; set; } = ["BODYWEIGHT"];
    public long Revision { get; set; } = 1;
    public static LocationForm From(Location x) => new() { Id = x.Id, MemberId = x.MemberId, Name = x.Name, Equipment = FamilyCalendar.Split(x.EquipmentIds).ToHashSet(), Revision = x.Revision };
}
public sealed class DayDraft
{
    public DateOnly Date { get; set; }
    public string Title { get; set; } = "Renforcement";
    public Guid? LocationId { get; set; }
    public int DurationMinutes { get; set; } = 30;
    public bool IsRest { get; set; }
    public List<ExerciseDraft> Exercises { get; set; } = [];
    public string NewExerciseId { get; set; } = "";
    public ProgramDayInput Input() => new(Date, IsRest ? "Repos" : Title, IsRest ? null : LocationId, IsRest ? 0 : DurationMinutes, IsRest, IsRest ? [] : Exercises.Select(x => new Prescription(x.ExerciseId, x.Sets, x.Target, x.RestSeconds)).ToList());
}
public sealed class ExerciseDraft
{
    public string ExerciseId { get; set; } = "";
    public int Sets { get; set; } = 3;
    public int Target { get; set; } = 40;
    public int RestSeconds { get; set; } = 20;
}

public sealed class ProgramEditor
{
    public Guid? Id { get; set; }
    public Guid MemberId { get; set; }
    [Required(ErrorMessage = "Donne un titre au programme."), StringLength(100)] public string Title { get; set; } = "Ma semaine en mouvement";
    public DateOnly WeekStart { get; set; }
    public List<DayDraft> Days { get; set; } = [];
    public ProgramInput Input() => new(MemberId, Title, WeekStart, Days.Select(x => x.Input()).ToList());
}
