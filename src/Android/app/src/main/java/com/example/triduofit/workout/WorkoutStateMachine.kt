package com.example.triduofit.workout

import com.example.triduofit.audio.AudioCoachService
import com.example.triduofit.audio.TechnoBeatsPlayer
import com.example.triduofit.audio.VoiceCommand
import com.example.triduofit.data.SettingsRepository
import com.example.triduofit.model.Exercise
import com.example.triduofit.model.WorkoutSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.example.triduofit.family.WorkoutCheckpoint
import android.os.SystemClock

enum class StepState {
    WAITING_FOR_GO, // L'application attend le "GO" vocal de l'athlète installé sur son appareil
    EXERCISING,     // L'effort est en cours, le chronomètre tourne
    PAUSED,         // L'athlète a demandé une pause
    COMPLETED       // La séance est terminée
}

data class WorkoutUiState(
    val sessionTitle: String = "",
    val stepState: StepState = StepState.WAITING_FOR_GO,
    val currentExerciseIndex: Int = 0,
    val totalExercises: Int = 0,
    val currentExercise: Exercise? = null,
    val secondsRemaining: Int = 0,
    val totalSecondsInStep: Int = 0,
    val isTechnoPlaying: Boolean = false,
    val isListeningToVoice: Boolean = true,
    val voiceFeedbackMessage: String = "",
    val lastVoiceCommandHeard: String = "",
    val completedIndices: List<Int> = emptyList(),
    val skippedIndices: List<Int> = emptyList()
)

class WorkoutStateMachine(
    private val session: WorkoutSession,
    private val audioCoach: AudioCoachService,
    private val technoPlayer: TechnoBeatsPlayer,
    private val settingsRepository: SettingsRepository,
    private val onWorkoutCompleted: (WorkoutCheckpoint) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var timerJob: Job? = null

    private val _state = MutableStateFlow(
        WorkoutUiState(
            sessionTitle = session.title,
            stepState = StepState.WAITING_FOR_GO,
            currentExerciseIndex = 0,
            totalExercises = session.exercises.size,
            currentExercise = session.exercises.firstOrNull(),
            secondsRemaining = session.exercises.firstOrNull()?.durationSeconds ?: 40,
            totalSecondsInStep = session.exercises.firstOrNull()?.durationSeconds ?: 40,
            isTechnoPlaying = settingsRepository.isTechnoEnabled()
        )
    )
    val state: StateFlow<WorkoutUiState> = _state.asStateFlow()

    private var previousStepState = StepState.WAITING_FOR_GO

    fun start(checkpoint: WorkoutCheckpoint? = null) {
        if (session.exercises.isEmpty()) {
            _state.value = _state.value.copy(stepState = StepState.COMPLETED)
            return
        }

        if (settingsRepository.isTechnoEnabled()) {
            technoPlayer.bpm = settingsRepository.getTechnoBpm()
        }

        if (checkpoint != null) {
            val index = checkpoint.currentExerciseIndex.coerceIn(0, session.exercises.lastIndex)
            _state.value = _state.value.copy(currentExerciseIndex = index, currentExercise = session.exercises[index], secondsRemaining = checkpoint.secondsRemaining, totalSecondsInStep = session.exercises[index].targetCount,
                completedIndices = checkpoint.completedIndices, skippedIndices = checkpoint.skippedIndices,
                stepState = if (checkpoint.stepState == "COMPLETED") StepState.COMPLETED else if (checkpoint.stepState == "WAITING_FOR_GO") StepState.WAITING_FOR_GO else StepState.PAUSED)
            if (_state.value.stepState == StepState.COMPLETED) onWorkoutCompleted(progress()) else audioCoach.speak("Ta séance est conservée. Dis GO quand tu es prêt à reprendre.")
            return
        }
        // Attend calmement le GO pour le premier atelier
        prepareAtelier(0, isFirstAtelier = true)
    }

    private fun prepareAtelier(exerciseIndex: Int, isFirstAtelier: Boolean = false) {
        val exercise = session.exercises.getOrNull(exerciseIndex) ?: run {
            completeWorkout()
            return
        }

        timerJob?.cancel()
        technoPlayer.setDucking(true)

        val isReps = exercise.targetType == com.example.triduofit.model.TargetType.REPETITIONS
        val count = exercise.targetCount

        _state.value = _state.value.copy(
            stepState = StepState.WAITING_FOR_GO,
            currentExerciseIndex = exerciseIndex,
            currentExercise = exercise,
            secondsRemaining = count,
            totalSecondsInStep = count,
            voiceFeedbackMessage = if (isReps) {
                "Installe-toi. Objectif : $count ${exercise.repUnitLabel}. Dis GO quand tu démarres."
            } else {
                "Installe-toi sur l'appareil. Dis GO quand tu es prêt."
            }
        )

        val introMessage = if (isReps) {
            if (isFirstAtelier) {
                "Séance prête. Premier atelier : ${exercise.name}. Objectif : $count ${exercise.repUnitLabel}. Installe-toi calmement. Dis GO pour attaquer."
            } else {
                "Prochain atelier : ${exercise.name}. Objectif : $count ${exercise.repUnitLabel}. Prends le temps de t'installer. Dis GO quand tu es prêt."
            }
        } else {
            if (isFirstAtelier) {
                "Séance prête. Premier atelier : ${exercise.name}. Installe-toi tranquillement. Dès que tu es en position, dis GO pour démarrer."
            } else {
                "Prochain atelier : ${exercise.name}. Prends le temps de t'installer. Dis GO quand tu es prêt."
            }
        }

        audioCoach.speak(introMessage)
    }

    fun triggerGo() {
        when (_state.value.stepState) {
            StepState.WAITING_FOR_GO -> {
                startExerciseStep(_state.value.currentExerciseIndex)
            }
            StepState.PAUSED -> {
                resume()
            }
            StepState.EXERCISING -> {
                val currentEx = _state.value.currentExercise
                if (currentEx?.targetType == com.example.triduofit.model.TargetType.REPETITIONS) {
                    // L'athlète signale qu'il a terminé ses répétitions !
                    audioCoach.speak("Superbe série ! Bien joué !")
                    onExerciseFinished(_state.value.currentExerciseIndex)
                } else {
                    audioCoach.speak("C'est déjà parti, donne tout !")
                }
            }
            StepState.COMPLETED -> {}
        }
    }

    private fun startExerciseStep(exerciseIndex: Int) {
        val exercise = session.exercises[exerciseIndex]
        val isReps = exercise.targetType == com.example.triduofit.model.TargetType.REPETITIONS
        val target = exercise.targetCount

        _state.value = _state.value.copy(
            stepState = StepState.EXERCISING,
            secondsRemaining = target,
            totalSecondsInStep = target,
            voiceFeedbackMessage = exercise.postureTip
        )

        // Relance la musique techno
        if (settingsRepository.isTechnoEnabled()) {
            if (!_state.value.isTechnoPlaying) {
                technoPlayer.start()
                _state.value = _state.value.copy(isTechnoPlaying = true)
            }
            technoPlayer.setDucking(false)
        }

        if (isReps) {
            audioCoach.speak("C'est parti ! Réalise tes $target ${exercise.repUnitLabel}. Dis GO dès que ta série est terminée.")
            // En mode répétitions, on ne tourne pas de minuteur à 0, l'athlète gère son rythme
        } else {
            audioCoach.speak("C'est parti ! ${target} secondes d'effort.")
            runCountdown(target, onTick = { remaining ->
                if (remaining == target / 2) {
                    audioCoach.speak("Mi-temps ! ${exercise.postureTip}")
                } else if (remaining == 5) {
                    audioCoach.speak("5, 4, 3, 2, 1, stop !")
                }
            }, onFinish = {
                onExerciseFinished(exerciseIndex)
            })
        }
    }

    private fun onExerciseFinished(exerciseIndex: Int) {
        _state.value = _state.value.copy(completedIndices = (_state.value.completedIndices + exerciseIndex).distinct())
        val nextIndex = exerciseIndex + 1
        technoPlayer.setDucking(true)

        if (nextIndex >= session.exercises.size) {
            completeWorkout()
            return
        }

        val nextExercise = session.exercises[nextIndex]

        audioCoach.speak("Temps écoulé, bien joué ! Prends le temps de souffler et de boire un coup.") {
            // Passe à l'écran de l'atelier suivant, et attend calmement le GO
            prepareAtelier(nextIndex, isFirstAtelier = false)
        }
    }

    private fun runCountdown(
        totalSeconds: Int,
        onTick: ((Int) -> Unit)? = null,
        onFinish: () -> Unit
    ) {
        timerJob?.cancel()
        timerJob = scope.launch {
            val deadline = SystemClock.elapsedRealtime() + totalSeconds * 1000L
            var previousRemaining = -1
            while (isActive) {
                val remaining = ((deadline - SystemClock.elapsedRealtime() + 999) / 1000).coerceAtLeast(0).toInt()
                if (remaining <= 0) break
                if (remaining != previousRemaining) { _state.value = _state.value.copy(secondsRemaining = remaining); onTick?.invoke(remaining); previousRemaining = remaining }
                delay(200)
            }
            if (isActive) {
                _state.value = _state.value.copy(secondsRemaining = 0)
                onFinish()
            }
        }
    }

    fun handleVoiceCommand(command: VoiceCommand, rawSpeech: String) {
        _state.value = _state.value.copy(lastVoiceCommandHeard = rawSpeech)

        when (command) {
            VoiceCommand.GO -> triggerGo()

            VoiceCommand.PAUSE -> pause()

            VoiceCommand.RESET_TIMER -> resetCurrentTimer()

            VoiceCommand.RESUME -> resume()

            VoiceCommand.NEXT -> skipToNext()

            VoiceCommand.REPEAT -> repeatCue()

            VoiceCommand.UNKNOWN -> {}
        }
    }

    fun resetCurrentTimer() {
        val exercise = _state.value.currentExercise ?: return
        val totalDuration = exercise.durationSeconds

        timerJob?.cancel()

        when (_state.value.stepState) {
            StepState.EXERCISING -> {
                _state.value = _state.value.copy(
                    secondsRemaining = totalDuration,
                    totalSecondsInStep = totalDuration,
                    voiceFeedbackMessage = "Chrono remis à zéro !"
                )
                audioCoach.speak("Chrono remis à zéro. C'est reparti !")
                startExerciseStep(_state.value.currentExerciseIndex)
            }

            StepState.PAUSED, StepState.WAITING_FOR_GO -> {
                _state.value = _state.value.copy(
                    secondsRemaining = totalDuration,
                    totalSecondsInStep = totalDuration,
                    voiceFeedbackMessage = "Chrono remis à zéro. Dis GO pour démarrer."
                )
                audioCoach.speak("Chrono remis à zéro. Dis GO pour démarrer !")
            }

            else -> {}
        }
    }

    fun pause() {
        if (_state.value.stepState == StepState.EXERCISING) {
            previousStepState = _state.value.stepState
            timerJob?.cancel()
            technoPlayer.setDucking(true)
            _state.value = _state.value.copy(
                stepState = StepState.PAUSED,
                voiceFeedbackMessage = "Séance en pause. Dis « GO » pour continuer."
            )
            audioCoach.speak("Séance en pause. Dis GO pour reprendre.")
        }
    }

    fun resume() {
        if (_state.value.stepState == StepState.PAUSED) {
            _state.value = _state.value.copy(
                stepState = StepState.EXERCISING,
                voiceFeedbackMessage = "Reprise de la séance !"
            )
            technoPlayer.setDucking(false)
            audioCoach.speak("Reprise !")
            val duration = _state.value.secondsRemaining
            val exercise = _state.value.currentExercise ?: return
            if (exercise.targetType == com.example.triduofit.model.TargetType.REPETITIONS) return

            runCountdown(duration, onTick = { remaining ->
                if (remaining == exercise.durationSeconds / 2) {
                    audioCoach.speak("Mi-temps ! ${exercise.postureTip}")
                } else if (remaining == 5) {
                    audioCoach.speak("5, 4, 3, 2, 1, stop !")
                }
            }, onFinish = {
                onExerciseFinished(_state.value.currentExerciseIndex)
            })
        }
    }

    fun skipToNext() {
        timerJob?.cancel()
        _state.value = _state.value.copy(skippedIndices = (_state.value.skippedIndices + _state.value.currentExerciseIndex).distinct())
        val nextIndex = _state.value.currentExerciseIndex + 1
        if (nextIndex < session.exercises.size) {
            prepareAtelier(nextIndex, isFirstAtelier = false)
        } else {
            completeWorkout()
        }
    }

    private fun repeatCue() {
        val exercise = _state.value.currentExercise ?: return
        audioCoach.speak("${exercise.name}. ${exercise.postureTip}")
    }

    private fun completeWorkout() {
        if (_state.value.stepState == StepState.COMPLETED) return
        timerJob?.cancel()
        technoPlayer.stop()

        _state.value = _state.value.copy(
            stepState = StepState.COMPLETED,
            voiceFeedbackMessage = "Félicitations ! Entraînement terminé avec succès !"
        )

        audioCoach.speak("Félicitations champion ! Séance terminée. Ton renforcement musculaire est validé pour ton triathlon !")
        onWorkoutCompleted(progress())
    }

    fun progress() = WorkoutCheckpoint(_state.value.currentExerciseIndex, _state.value.secondsRemaining, _state.value.stepState.name, _state.value.completedIndices, _state.value.skippedIndices)

    fun toggleTechnoMusic() {
        val currentlyPlaying = _state.value.isTechnoPlaying
        if (currentlyPlaying) {
            technoPlayer.stop()
            _state.value = _state.value.copy(isTechnoPlaying = false)
        } else {
            technoPlayer.start()
            _state.value = _state.value.copy(isTechnoPlaying = true)
        }
    }

    fun release() {
        timerJob?.cancel()
        technoPlayer.stop()
        audioCoach.stop()
    }
}
