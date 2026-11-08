package com.example.triduofit.family

import com.example.triduofit.model.*
import kotlinx.serialization.Serializable

@Serializable data class FamilyMember(val id: String, val nickname: String, val age: Int = 18, val weightKg: Double = 70.0, val goal: String = "", val level: String = "Débutant", val isAdmin: Boolean = false)
@Serializable data class DeviceGrant(val token: String, val deviceId: String, val member: FamilyMember)
@Serializable data class InvitationPreview(val memberId: String, val nickname: String, val expiresAtUtc: String)
@Serializable data class ServerExercise(val id: String, val name: String, val equipmentId: String = "BODYWEIGHT", val muscle: String = "", val targetType: String = "time", val instructions: String = "", val postureTip: String = "", val imageUrl: String? = null, val videoUrl: String? = null, val sets: Int = 3, val target: Int = 40, val restSeconds: Int = 20)
@Serializable data class FamilyLocation(val id: String, val memberId: String, val name: String, val equipmentIds: String = "")
@Serializable data class FamilyDay(val id: String, val programId: String, val date: String, val title: String, val locationId: String? = null, val locationName: String = "", val durationMinutes: Int = 30, val isRest: Boolean = false, val exercises: List<ServerExercise> = emptyList()) {
    fun nativeSession() = WorkoutSession(
        id = id, dayOfWeek = java.time.LocalDate.parse(date).dayOfWeek.value,
        dayName = java.time.LocalDate.parse(date).dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.FRENCH),
        splitType = SplitType.SPLIT_BODYWEIGHT, isRestDay = isRest, estimatedDurationMinutes = durationMinutes,
        title = title, subtitle = locationName, programDayId = id,
        exercises = exercises.flatMap { e -> (1..e.sets).map { set -> Exercise(
            id = e.id, name = "${e.name} — série $set/${e.sets}", equipment = runCatching { EquipmentType.valueOf(e.equipmentId) }.getOrDefault(EquipmentType.BODYWEIGHT),
            targetMuscle = e.muscle, targetType = if (e.targetType == "repetitions") TargetType.REPETITIONS else TargetType.TIME_SECONDS,
            targetCount = e.target, repUnitLabel = if (e.targetType == "repetitions") "RÉPÉTITIONS" else "SECONDES", restSeconds = e.restSeconds,
            voiceCue = e.instructions, postureTip = e.postureTip, animationType = AnimationType.GENERAL_FITNESS,
            drawableResName = e.imageUrl?.substringAfterLast('/')?.substringBeforeLast('.'), videoResName = e.videoUrl?.substringAfterLast('/')?.substringBeforeLast('.'),
            cachedImageUrl = e.imageUrl, cachedVideoUrl = e.videoUrl
        ) } }
    )
}
@Serializable data class ExercisePerformance(val exerciseId: String, val completedSets: Int, val repetitions: Int? = null, val durationSeconds: Int? = null, val weightKg: Double? = null, val skipped: Boolean = false)
@Serializable data class WorkoutSubmission(val id: String, val programDayId: String, val startedAtUtc: String, val finishedAtUtc: String, val exercises: List<ExercisePerformance>)
@Serializable data class FamilyResult(val id: String, val memberId: String, val programDayId: String, val status: String)
@Serializable data class FamilySnapshot(val serverTimeUtc: String, val member: FamilyMember, val members: List<FamilyMember> = emptyList(), val locations: List<FamilyLocation> = emptyList(), val days: List<FamilyDay> = emptyList(), val results: List<FamilyResult> = emptyList())
@Serializable data class WorkoutCheckpoint(val currentExerciseIndex: Int = 0, val secondsRemaining: Int = 0, val stepState: String = "WAITING_FOR_GO", val completedIndices: List<Int> = emptyList(), val skippedIndices: List<Int> = emptyList())
@Serializable data class FamilyLink(val action: String, val code: String? = null, val day: String? = null)
