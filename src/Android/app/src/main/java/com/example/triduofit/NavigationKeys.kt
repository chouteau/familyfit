package com.example.triduofit

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object HomeNavKey : NavKey
@Serializable data object FamilyNavKey : NavKey
@Serializable data class WorkoutNavKey(val sessionId: String) : NavKey
@Serializable data class SuccessNavKey(val completed: Boolean = true) : NavKey
@Serializable data object SettingsNavKey : NavKey
