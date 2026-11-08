using FamilyFit.Contracts;

namespace FamilyFit.Core.Validators;

public static class MemberValidator
{
    public static string? Validate(MemberInput input)
    {
        if (string.IsNullOrWhiteSpace(input.Nickname) || input.Nickname.Trim().Length is < 2 or > 40) return "Le pseudo doit contenir entre 2 et 40 caractères.";
        if (input.Age is < 1 or > 120) return "Indique un âge entre 1 et 120 ans.";
        if (!double.IsFinite(input.WeightKg) || input.WeightKg is < 10 or > 500) return "Indique un poids entre 10 et 500 kg.";
        if (input.SessionMinutes is < 5 or > 180) return "La durée doit être comprise entre 5 et 180 minutes.";
        if (string.IsNullOrWhiteSpace(input.Goal) || input.Goal.Length > 500 || input.ExcludedExerciseIds == null || input.ExcludedExerciseIds.Length > 4000) return "Indique un objectif de 500 caractères maximum.";
        if (!new[] { "Débutant", "Intermédiaire", "Avancé" }.Contains(input.Level)) return "Choisis un niveau proposé.";
        var days = FamilyCalendar.Split(input.TrainingDays ?? "");
        if (days.Length is < 1 or > 7 || days.Distinct().Count() != days.Length || days.Any(x => !int.TryParse(x, out var day) || day is < 0 or > 6)) return "Choisis entre un et sept jours d’entraînement.";
        return null;
    }
}
