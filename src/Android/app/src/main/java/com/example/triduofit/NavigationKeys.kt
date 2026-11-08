package com.example.triduofit

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object HomeNavKey : NavKey
@Serializable data class WorkoutNavKey(val sessionId: String) : NavKey
@Serializable data object SuccessNavKey : NavKey
@Serializable data object SettingsNavKey : NavKey
