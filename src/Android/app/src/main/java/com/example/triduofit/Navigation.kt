package com.example.triduofit

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.triduofit.data.WorkoutProgramRepository
import com.example.triduofit.ui.home.HomeScreen
import com.example.triduofit.ui.settings.SettingsScreen
import com.example.triduofit.ui.workout.WorkoutScreen
import com.example.triduofit.ui.workout.WorkoutSuccessScreen

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(HomeNavKey)
    val workoutRepo = remember { WorkoutProgramRepository() }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<HomeNavKey> {
                HomeScreen(
                    onStartWorkout = { session ->
                        backStack.add(WorkoutNavKey(session.id))
                    },
                    onOpenSettings = {
                        backStack.add(SettingsNavKey)
                    }
                )
            }

            entry<WorkoutNavKey> { key ->
                val session = workoutRepo.getWeeklySplitSessions().firstOrNull { it.id == key.sessionId }
                    ?: workoutRepo.getWeeklySplitSessions().first()

                WorkoutScreen(
                    session = session,
                    onBack = { backStack.removeLastOrNull() },
                    onWorkoutCompleted = {
                        backStack.removeLastOrNull()
                        backStack.add(SuccessNavKey)
                    }
                )
            }

            entry<SuccessNavKey> {
                WorkoutSuccessScreen(
                    onContinue = {
                        backStack.clear()
                        backStack.add(HomeNavKey)
                    }
                )
            }

            entry<SettingsNavKey> {
                SettingsScreen(
                    onBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}
