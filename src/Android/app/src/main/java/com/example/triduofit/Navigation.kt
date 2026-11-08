package com.example.triduofit

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.triduofit.data.WorkoutProgramRepository
import com.example.triduofit.family.FamilyHubScreen
import com.example.triduofit.family.FamilyRepository
import com.example.triduofit.ui.home.HomeScreen
import com.example.triduofit.ui.settings.SettingsScreen
import com.example.triduofit.ui.workout.WorkoutScreen
import com.example.triduofit.ui.workout.WorkoutSuccessScreen

@Composable
fun MainNavigation(deepLink: String? = null, onLinkHandled: () -> Unit = {}) {
    val context = LocalContext.current
    val backStack = rememberNavBackStack(FamilyNavKey)
    val workoutRepo = remember { WorkoutProgramRepository() }
    val family = remember { FamilyRepository.get(context) }
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { family.load(); ready = true }
    if (!ready) { CircularProgressIndicator(); return }
    NavDisplay(backStack = backStack, onBack = { if (backStack.size > 1) backStack.removeLastOrNull() }, entryProvider = entryProvider {
        entry<FamilyNavKey> { FamilyHubScreen(deepLink, onStart = { session -> backStack.add(WorkoutNavKey(session.id)) }, onLocalCoach = { backStack.add(HomeNavKey) }, onLinkHandled = onLinkHandled) }
        entry<HomeNavKey> { HomeScreen(onStartWorkout = { session -> backStack.add(WorkoutNavKey(session.id)) }, onOpenSettings = { backStack.add(SettingsNavKey) }) }
        entry<WorkoutNavKey> { key ->
            val session = family.activeSession()?.takeIf { it.id == key.sessionId }
                ?: workoutRepo.getWeeklySplitSessions().firstOrNull { it.id == key.sessionId }
                ?: workoutRepo.getDedicatedPosterSession().takeIf { it.id == key.sessionId }
            if (session != null) WorkoutScreen(session, onBack = { backStack.removeLastOrNull() }, onWorkoutCompleted = { complete -> backStack.removeLastOrNull(); backStack.add(SuccessNavKey(complete)) })
            else LaunchedEffect(key.sessionId) { backStack.clear(); backStack.add(FamilyNavKey) }
        }
        entry<SuccessNavKey> { key ->
            LaunchedEffect(Unit) { family.sync() }
            WorkoutSuccessScreen(completed = key.completed, onContinue = { backStack.clear(); backStack.add(FamilyNavKey) })
        }
        entry<SettingsNavKey> { SettingsScreen(onBack = { backStack.removeLastOrNull() }) }
    })
}
