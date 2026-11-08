using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;

namespace FamilyFit.Core.Validators;

public static class ProgramValidator
{
    public static string? Validate(ProgramInput input, Member member, IReadOnlyList<Location> locations, IReadOnlyList<Exercise> catalogue)
    {
        if (input.MemberId == Guid.Empty || input.MemberId != member.Id) return "Membre invalide.";
        if (string.IsNullOrWhiteSpace(input.Title) || input.Title.Length > 100) return "Donne un titre au programme (100 caractères maximum).";
        if (input.WeekStart.DayOfWeek != DayOfWeek.Monday) return "La semaine doit commencer un lundi.";
        if (input.Days == null || input.Days.Count != 7 || input.Days.Any(x => x == null) || input.Days.Select(x => x.Date).Distinct().Count() != 7) return "Le programme doit contenir les sept jours de la semaine, repos compris.";
        var excluded = FamilyCalendar.Split(member.ExcludedExerciseIds).ToHashSet();
        foreach (var day in input.Days)
        {
            if (day.Date < input.WeekStart || day.Date > input.WeekStart.AddDays(6)) return "Une séance est située en dehors de la semaine choisie.";
            if (string.IsNullOrWhiteSpace(day.Title) || day.Title.Length > 100) return "Chaque journée doit avoir un titre.";
            if (day.Exercises == null || day.Exercises.Any(x => x == null)) return "La liste d’exercices est absente ou invalide.";
            if (day.IsRest) { if (day.Exercises.Count != 0) return "Un jour de repos ne contient pas d’exercice."; continue; }
            var location = locations.FirstOrDefault(x => x.Id == day.LocationId && x.MemberId == member.Id);
            if (location == null) return "Choisis un lieu appartenant au membre pour chaque séance.";
            if (day.DurationMinutes is < 5 or > 180) return "La durée d’une séance doit être comprise entre 5 et 180 minutes.";
            if (day.Exercises.Count is < 1 or > 20 || day.Exercises.Select(x => x.ExerciseId).Distinct().Count() != day.Exercises.Count) return "Prévois entre un et vingt exercices distincts par séance.";
            var equipment = FamilyCalendar.Split(location.EquipmentIds).Append("BODYWEIGHT").ToHashSet();
            foreach (var item in day.Exercises)
            {
                var exercise = catalogue.FirstOrDefault(x => x.Id == item.ExerciseId);
                if (exercise == null || excluded.Contains(item.ExerciseId)) return "Le programme contient un exercice inconnu ou exclu par le membre.";
                if (!equipment.Contains(exercise.EquipmentId)) return $"Le matériel nécessaire à « {exercise.Name} » n’est pas disponible à {location.Name}.";
                if (item.Sets is < 1 or > 10 || item.Target is < 1 or > 3600 || item.RestSeconds is < 0 or > 300 || (exercise.TargetType == "repetitions" && item.Target > 200)) return "Vérifie le nombre de séries, la cible et le temps de repos.";
            }
        }
        return null;
    }
}
