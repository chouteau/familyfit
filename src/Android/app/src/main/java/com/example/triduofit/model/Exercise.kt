package com.example.triduofit.model
import kotlinx.serialization.Serializable

@Serializable
enum class EquipmentType(val displayName: String) {
    ELLIPTICAL("Vélo Elliptique (Cardio & Échauffement)"),
    BALANCE_BOARD("Plateau d'Équilibre (Proprioception)"),
    PARALLEL_BARS("Barres Parallèles (Dips & Dos)"),
    BACK_BENCH("Banc à Dorsaux / Lombaires"),
    AB_BENCH("Banc à Abdominaux Incliné"),
    PLYO_BOX("Cube en Bois (Plyo Box)"),
    BODYWEIGHT("Poids de Corps"),
    DUMBBELLS("Haltères"), LAT_PULLDOWN("Poulie haute"), CHEST_PRESS("Presse à pectoraux"),
    SEATED_ROW("Rowing assis"), LEG_PRESS("Presse à cuisses"), TREADMILL("Tapis de course"),
    BIKE("Vélo"), MAT("Tapis de sol"), RESISTANCE_BAND("Élastique")
}

@Serializable
enum class TargetType {
    TIME_SECONDS,  // Décompte de temps en secondes (ex: Vélo elliptique, Planche)
    REPETITIONS    // Nombre de répétitions ciblées (ex: Abdos, Squats bulgares, Dips)
}

@Serializable enum class AnimationType {
    ELLIPTICAL_CARDIO,
    BALANCE_BOARD_STABILITY,
    PARALLEL_BARS_DIPS,
    PARALLEL_BARS_INVERTED_ROW,
    PLYO_BOX_STEP_UP,
    PLYO_BOX_BULGARIAN_SQUAT,
    AB_BENCH_CRUNCH,
    BACK_BENCH_HYPEREXTENSION,
    BODYWEIGHT_PUSHUP,
    BODYWEIGHT_BURPEE,
    BODYWEIGHT_SQUAT,
    BODYWEIGHT_PLANK,
    BODYWEIGHT_MOUNTAIN_CLIMBER,
    BODYWEIGHT_SWIMMER,
    BODYWEIGHT_LUNGES,
    BODYWEIGHT_SUPERMAN,
    BODYWEIGHT_GLUTE_BRIDGE,
    BODYWEIGHT_CALF_RAISE,
    BODYWEIGHT_BICYCLE_CRUNCH,
    BODYWEIGHT_RUSSIAN_TWIST,
    BODYWEIGHT_SQUAT_JACKS,
    BODYWEIGHT_TRICEP_DIP,
    GENERAL_FITNESS
}

@Serializable data class Exercise(
    val id: String,
    val name: String,
    val equipment: EquipmentType,
    val targetMuscle: String,
    val targetType: TargetType = TargetType.TIME_SECONDS,
    val targetCount: Int = 40,
    val repUnitLabel: String = "SECONDES", // "SECONDES", "RÉPÉTITIONS", "PAR JAMBE"
    val restSeconds: Int = 20,
    val voiceCue: String,
    val postureTip: String,
    val animationType: AnimationType,
    val executionSteps: List<String> = emptyList(),
    val commonMistakes: String = "",
    val drawableResName: String? = null,
    val videoResName: String? = null,
    val cachedImageUrl: String? = null,
    val cachedVideoUrl: String? = null
) {
    val durationSeconds: Int get() = targetCount
}
