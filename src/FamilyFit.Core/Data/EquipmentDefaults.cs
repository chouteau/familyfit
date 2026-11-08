using FamilyFit.Contracts;
using FamilyFit.Contracts.Models.Equipment;

namespace FamilyFit.Core.Data;

internal static class EquipmentDefaults
{
    public static IEnumerable<EquipmentDefinition> All => EquipmentCatalogue.All.Select(item =>
    {
        var (description, muscles) = Details(item.Key);
        return new EquipmentDefinition { Id = item.Key, Name = item.Value, Description = description, TargetMuscles = muscles };
    });

    private static (string Description, string[] Muscles) Details(string id) => id switch
    {
        "BODYWEIGHT" => ("Des exercices sans appareil, toujours disponibles. La sollicitation dépend du mouvement choisi.", ["Corps entier selon l’exercice"]),
        "ELLIPTICAL" => ("Des pédales et des poignées mobiles pour un mouvement continu, avec une résistance réglable.", ["Cardio", "Jambes", "Fessiers"]),
        "BALANCE_BOARD" => ("Un plateau instable pour des exercices de maintien et de contrôle de la posture.", ["Équilibre", "Sangle abdominale", "Chevilles"]),
        "PARALLEL_BARS" => ("Deux barres pour réaliser des dips, des maintiens et des relevés de genoux.", ["Triceps", "Pectoraux", "Abdominaux selon l’exercice"]),
        "BACK_BENCH" => ("Un support pour les jambes et le bassin permettant des extensions contrôlées des hanches ou du tronc.", ["Fessiers", "Ischio-jambiers", "Muscles du dos"]),
        "AB_BENCH" => ("Un banc pour effectuer des exercices abdominaux au sol ou en position inclinée.", ["Abdominaux", "Fléchisseurs de hanche selon l’exercice"]),
        "PLYO_BOX" => ("Une plateforme stable pour des montées, des descentes et des sauts adaptés à ton niveau.", ["Quadriceps", "Fessiers", "Mollets", "Équilibre"]),
        "DUMBBELLS" => ("Des poids libres tenus dans les mains pour varier les exercices et travailler un côté à la fois.", ["Selon l’exercice : bras, épaules, dos et jambes"]),
        "LAT_PULLDOWN" => ("Assis, tu tires une barre ou des poignées vers le haut de la poitrine contre une résistance réglable.", ["Grand dorsal", "Haut du dos", "Biceps"]),
        "CHEST_PRESS" => ("Assis contre un dossier, tu pousses les poignées devant toi puis contrôles leur retour.", ["Pectoraux", "Triceps", "Avant des épaules"]),
        "SEATED_ROW" => ("Assis, tu ramènes les poignées vers le buste en rapprochant les omoplates.", ["Grand dorsal", "Rhomboïdes", "Trapèzes", "Biceps"]),
        "LEG_PRESS" => ("Assis contre un dossier, tu pousses une plateforme avec les pieds.", ["Quadriceps", "Fessiers"]),
        "TREADMILL" => ("Un tapis motorisé pour marcher ou courir, avec une vitesse et une inclinaison réglables selon le modèle.", ["Cardio", "Jambes", "Mollets", "Fessiers"]),
        "BIKE" => ("Un vélo fixe avec résistance réglable pour pédaler à ton rythme.", ["Cardio", "Quadriceps", "Fessiers", "Mollets"]),
        "MAT" => ("Un support confortable pour les exercices au sol, le gainage et la mobilité.", ["Abdominaux selon l’exercice", "Mobilité"]),
        "RESISTANCE_BAND" => ("Une bande souple offrant une résistance progressive pour des exercices variés.", ["Selon l’exercice : bras, épaules, dos et jambes"]),
        _ => ("Description à compléter.", ["Selon l’exercice"])
    };
}
