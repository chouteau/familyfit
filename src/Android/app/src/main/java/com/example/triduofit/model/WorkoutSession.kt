package com.example.triduofit.model
import kotlinx.serialization.Serializable

@Serializable
enum class SplitType(val title: String, val subtitle: String) {
    SPLIT_A("Split A : Cuisses & Abdos", "Cube plyo & Banc incliné"),
    SPLIT_B("Split B : Dorsaux & Haut du Corps", "Banc lombaires & Pompes"),
    SPLIT_C("Split C : Gainage & Pliométrie", "Stabilité bassin & Puissance"),
    SPLIT_D("Split D : Chaîne Postérieure & Core", "Prévention blessures triathlon"),
    SPLIT_BODYWEIGHT("Circuit Poster : 100% Poids de Corps", "Burpees, Pompes, Fentes, Nageur, Climbers & Gainage")
}

@Serializable data class WorkoutSession(
    val id: String,
    val dayOfWeek: Int, // 1 (Lundi) .. 7 (Dimanche)
    val dayName: String,
    val splitType: SplitType,
    val isRestDay: Boolean = false,
    val estimatedDurationMinutes: Int = 35,
    val exercises: List<Exercise> = emptyList(),
    val title: String = splitType.title,
    val subtitle: String = splitType.subtitle,
    val programDayId: String? = null
)
