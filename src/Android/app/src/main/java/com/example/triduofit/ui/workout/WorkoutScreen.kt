package com.example.triduofit.ui.workout

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.triduofit.family.FamilyRepository
import com.example.triduofit.family.WorkoutCheckpoint
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.triduofit.audio.AudioCoachService
import com.example.triduofit.audio.TechnoBeatsPlayer
import com.example.triduofit.audio.VoiceCommandRecognizer
import com.example.triduofit.data.SettingsRepository
import com.example.triduofit.model.TargetType
import com.example.triduofit.model.WorkoutSession
import com.example.triduofit.theme.DuoBackgroundSubtle
import com.example.triduofit.theme.DuoBlue
import com.example.triduofit.theme.DuoBlueDark
import com.example.triduofit.theme.DuoGray
import com.example.triduofit.theme.DuoGrayDark
import com.example.triduofit.theme.DuoGreen
import com.example.triduofit.theme.DuoGreenDark
import com.example.triduofit.theme.DuoOrange
import com.example.triduofit.theme.DuoOrangeDark
import com.example.triduofit.theme.DuoRed
import com.example.triduofit.theme.DuoRedDark
import com.example.triduofit.theme.DuoTextDark
import com.example.triduofit.theme.DuoTextMuted
import com.example.triduofit.ui.components.DuoButton
import com.example.triduofit.ui.components.DuoCard
import com.example.triduofit.ui.components.VideoPlayerView
import com.example.triduofit.ui.components.ExerciseAnimationView
import com.example.triduofit.workout.StepState
import com.example.triduofit.workout.WorkoutStateMachine

@Composable
fun WorkoutScreen(
    session: WorkoutSession,
    onBack: () -> Unit,
    onWorkoutCompleted: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val family = remember { FamilyRepository.get(context) }
    val coroutineScope = rememberCoroutineScope()
    var finishedCheckpoint by remember { mutableStateOf<WorkoutCheckpoint?>(null) }
    var showExit by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val settingsRepo = remember { SettingsRepository(context) }
    val technoPlayer = remember { TechnoBeatsPlayer() }

    val audioCoach = remember {
        AudioCoachService(context, settingsRepo) { isDucking ->
            technoPlayer.setDucking(isDucking)
        }
    }

    val stateMachine = remember {
        WorkoutStateMachine(
            session = session,
            audioCoach = audioCoach,
            technoPlayer = technoPlayer,
            settingsRepository = settingsRepo,
            onWorkoutCompleted = { finishedCheckpoint = it }
        )
    }

    val voiceRecognizer = remember {
        VoiceCommandRecognizer(context, settingsRepo) { command, rawSpeech ->
            stateMachine.handleVoiceCommand(command, rawSpeech)
        }
    }

    val uiState by stateMachine.state.collectAsState()
    val familyState by family.state.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) voiceRecognizer.startListening() }

    LaunchedEffect(Unit) {
        stateMachine.start(family.checkpoint().takeIf { family.activeSession()?.id == session.id })
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) voiceRecognizer.startListening() else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    DisposableEffect(lifecycle) {
        val window = (context as? android.app.Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) { stateMachine.pause(); voiceRecognizer.stopListening() } else if (event == Lifecycle.Event.ON_RESUME && ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) voiceRecognizer.startListening() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    LaunchedEffect(uiState.currentExerciseIndex, uiState.secondsRemaining, uiState.stepState, uiState.completedIndices, uiState.skippedIndices) {
        if (session.programDayId != null) family.saveCheckpoint(stateMachine.progress())
    }
    BackHandler { stateMachine.pause(); showExit = true }
    LaunchedEffect(familyState.storageFailure) { if (familyState.storageFailure) stateMachine.pause() }
    if (familyState.storageFailure) AlertDialog(onDismissRequest = {}, title = { Text("L’enregistrement est en pause") }, text = { Text(familyState.message.orEmpty()) }, confirmButton = { TextButton(onClick = { coroutineScope.launch { family.saveCheckpoint(stateMachine.progress()) } }) { Text("Réessayer l’enregistrement") } })
    if (finishedCheckpoint != null || showExit) {
        val checkpoint = finishedCheckpoint ?: stateMachine.progress()
        val allDone = checkpoint.completedIndices.size == session.exercises.size && checkpoint.skippedIndices.isEmpty()
        AlertDialog(onDismissRequest = { if (!saving) showExit = false }, title = { Text(if (allDone) "Confirmer la séance ?" else "Enregistrer cette séance ?") },
            text = { Text(saveError ?: if (allDone) "Tous les exercices sont réalisés. Les résultats d’un programme familial seront synchronisés dès que le réseau le permettra." else "${checkpoint.completedIndices.size} série(s) réalisée(s). La séance sera enregistrée comme partielle ou abandonnée, selon ce qui a été fait.") },
            confirmButton = { TextButton(enabled = !saving, onClick = { coroutineScope.launch { saving = true; try { val completed = if (session.programDayId != null) family.finish(checkpoint) else allDone; if (completed) { settingsRepo.incrementStreak(); settingsRepo.addXp(60) }; finishedCheckpoint = null; showExit = false; onWorkoutCompleted(completed) } catch (e: Exception) { saveError = "L’enregistrement local a échoué. Libère de l’espace puis réessaie ; la séance ouverte est conservée." } finally { saving = false } } }) { Text("Confirmer et enregistrer") } },
            dismissButton = { TextButton(enabled = !saving, onClick = { if (finishedCheckpoint != null) onBack() else showExit = false }) { Text(if (finishedCheckpoint != null && session.programDayId != null) "Conserver pour plus tard" else if (finishedCheckpoint != null) "Revenir au coach" else "Continuer la séance") } })
    }

    DisposableEffect(Unit) {
        onDispose {
            stateMachine.release()
            voiceRecognizer.stopListening()
            voiceRecognizer.release()
            technoPlayer.stop()
            audioCoach.release()
        }
    }

    var showGuide by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.currentExerciseIndex) {
        showGuide = false
    }

    val exercise = uiState.currentExercise
    val isRepetitionMode = exercise?.targetType == TargetType.REPETITIONS
    val displayCount = if (isRepetitionMode) (exercise?.targetCount ?: 12) else uiState.secondsRemaining
    val displayUnit = if (isRepetitionMode) (exercise?.repUnitLabel ?: "RÉPÉTITIONS") else "SECONDES"

    val stateColor = when (uiState.stepState) {
        StepState.WAITING_FOR_GO -> DuoBlue
        StepState.EXERCISING -> DuoGreen
        StepState.PAUSED -> DuoOrange
        StepState.COMPLETED -> DuoGreen
    }
    val stateColorDark = when (uiState.stepState) {
        StepState.WAITING_FOR_GO -> DuoBlueDark
        StepState.EXERCISING -> DuoGreenDark
        StepState.PAUSED -> DuoOrangeDark
        StepState.COMPLETED -> DuoGreenDark
    }

    val progressValue = if (isRepetitionMode) {
        if (uiState.stepState == StepState.EXERCISING) 1f else 0.4f
    } else {
        if (uiState.totalSecondsInStep > 0) {
            uiState.secondsRemaining.toFloat() / uiState.totalSecondsInStep.toFloat()
        } else 0f
    }

    val animatedProgress by animateFloatAsState(targetValue = progressValue, label = "timer_progress")

    var showFullVisualDialog by remember { mutableStateOf(false) }

    val imageResId = remember(exercise?.drawableResName) {
        if (!exercise?.drawableResName.isNullOrBlank()) {
            val id = context.resources.getIdentifier(exercise.drawableResName, "drawable", context.packageName)
            if (id != 0) id else null
        } else null
    }

    val videoResId = remember(exercise?.videoResName) {
        if (!exercise?.videoResName.isNullOrBlank()) {
            val id = context.resources.getIdentifier(exercise.videoResName, "raw", context.packageName)
            if (id != 0) id else null
        } else null
    }

    val infiniteTransition = rememberInfiniteTransition(label = "coach_motion")
    val videoScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coach_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DuoBackgroundSubtle)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Barre supérieure : Quitter, Titre, Contrôle musique Techno
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    stateMachine.release()
                    stateMachine.pause()
                    showExit = true
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DuoTextDark)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "EXERCICE ${uiState.currentExerciseIndex + 1} / ${uiState.totalExercises}",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = DuoTextMuted,
                    letterSpacing = 1.sp
                )
                Text(
                    text = when (uiState.stepState) {
                        StepState.WAITING_FOR_GO -> "EN ATTENTE DU GO"
                        StepState.EXERCISING -> if (isRepetitionMode) "SÉRIE EN COURS" else "EFFORT INTENSE"
                        StepState.PAUSED -> "EN PAUSE"
                        StepState.COMPLETED -> "TERMINÉ"
                    },
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = stateColor
                )
            }

            // Bouton Techno On/Off
            IconButton(
                onClick = { stateMachine.toggleTechnoMusic() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (uiState.isTechnoPlaying) Color(0xFFDDF4FF) else Color.White)
            ) {
                Text(if (uiState.isTechnoPlaying) "🎵" else "🔇", fontSize = 18.sp)
            }
        }

        // Zone centrale défilable (Animation, Minuteur/Reps, Détails et Étapes)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 2. Zone de démonstration vidéo réaliste ou d'image
            DuoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(185.dp)
                    .clickable {
                        if (imageResId != null || videoResId != null) {
                            showFullVisualDialog = true
                        }
                    },
                backgroundColor = Color.Black,
                borderColor = DuoGray,
                shadowColor = DuoGrayDark
            ) {
                if (videoResId != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        VideoPlayerView(videoResId = videoResId, modifier = Modifier.fillMaxSize())

                        // Badge supérieur
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.72f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "🎬 VIDÉO EN BOUCLE (10-15s)",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.72f))
                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "🔍 PLEIN ÉCRAN",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Badge inférieur matériel
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Coach Triathlète 58 ans • ${exercise?.equipment?.displayName ?: ""}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                } else if (imageResId != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = painterResource(id = imageResId),
                            contentDescription = exercise?.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(if (uiState.stepState == StepState.EXERCISING) videoScale else 1.0f)
                        )

                        // Badge supérieur
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.72f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "🎥 DÉMO RÉELLE",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.72f))
                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "🔍 AGRANDIR",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Badge inférieur matériel
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Coach Triathlète 58 ans • ${exercise?.equipment?.displayName ?: ""}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                } else if (exercise != null) {
                    ExerciseAnimationView(animationType = exercise.animationType)
                }
            }

            // 3. Minuteur ou Compteur de répétitions circulaire
            Box(
                modifier = Modifier.size(125.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = DuoGray,
                    strokeWidth = 10.dp
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = stateColor,
                    strokeWidth = 10.dp
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$displayCount",
                        fontSize = if (displayCount >= 100) 36.sp else 42.sp,
                        fontWeight = FontWeight.Black,
                        color = stateColor
                    )
                    Text(
                        text = displayUnit.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = DuoTextMuted,
                        letterSpacing = 1.sp
                    )
                }
            }

            // 4. Cartouche d'information de l'exercice et guide technique
            DuoCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color.White,
                borderColor = DuoGray,
                shadowColor = DuoGrayDark
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = exercise?.name ?: "Séance",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = DuoTextDark
                            )
                            Text(
                                text = "Matériel : ${exercise?.equipment?.displayName ?: ""} • ${exercise?.targetMuscle ?: ""}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DuoBlueDark
                            )
                        }

                        if (!exercise?.executionSteps.isNullOrEmpty()) {
                            Text(
                                text = if (showGuide) "Masquer ▲" else "Étapes ▼",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = DuoBlue,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEBF7FF))
                                    .clickable { showGuide = !showGuide }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "💡 ${uiState.voiceFeedbackMessage}",
                        fontSize = 12.sp,
                        color = DuoTextDark,
                        lineHeight = 16.sp
                    )

                    if (showGuide && exercise?.executionSteps?.isNotEmpty() == true) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF8F9FA))
                                .padding(8.dp)
                        ) {
                            exercise.executionSteps.forEach { step ->
                                Text(
                                    text = step,
                                    fontSize = 11.sp,
                                    color = DuoTextDark,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                            if (exercise.commonMistakes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "⚠️ ${exercise.commonMistakes}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DuoOrangeDark,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 5. Indicateur d'écoute vocale permanente (Micro actif en continu)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFEBF7FF))
                .padding(vertical = 6.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("🟢", fontSize = 12.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isRepetitionMode) {
                        "Dis « GO » pour démarrer • « GO » quand la série est finie"
                    } else {
                        "Dis « GO » pour démarrer • « Mets en pause » • « Reviens à zéro »"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DuoBlueDark
                )
            }
            if (uiState.lastVoiceCommandHeard.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Ordre capté : « ${uiState.lastVoiceCommandHeard} »",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = DuoGreenDark
                )
            }
        }

        // 6. Boutons de contrôle (Dégagés de la barre de navigation Android)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DuoButton(
                text = "À ZÉRO",
                modifier = Modifier.weight(0.75f),
                color = DuoGray,
                shadowColor = DuoGrayDark,
                textColor = DuoTextDark,
                onClick = { stateMachine.resetCurrentTimer() }
            )

            if (uiState.stepState == StepState.WAITING_FOR_GO) {
                DuoButton(
                    text = "LANCER (GO)",
                    modifier = Modifier.weight(1.35f),
                    color = DuoGreen,
                    shadowColor = DuoGreenDark,
                    onClick = { stateMachine.triggerGo() }
                )
            } else if (uiState.stepState == StepState.PAUSED) {
                DuoButton(
                    text = "REPRENDRE (GO)",
                    modifier = Modifier.weight(1.35f),
                    color = DuoGreen,
                    shadowColor = DuoGreenDark,
                    onClick = { stateMachine.triggerGo() }
                )
            } else if (isRepetitionMode) {
                DuoButton(
                    text = "SÉRIE FINIE (GO)",
                    modifier = Modifier.weight(1.35f),
                    color = DuoGreen,
                    shadowColor = DuoGreenDark,
                    onClick = { stateMachine.triggerGo() }
                )
            } else {
                DuoButton(
                    text = "PAUSE",
                    modifier = Modifier.weight(1.35f),
                    color = DuoOrange,
                    shadowColor = DuoOrangeDark,
                    onClick = { stateMachine.pause() }
                )
            }

            DuoButton(
                text = "SUIVANT",
                modifier = Modifier.weight(0.9f),
                color = DuoBlue,
                shadowColor = DuoBlueDark,
                onClick = { stateMachine.skipToNext() }
            )
        }
    }

    // Modal de zoom et de posture ultra-détaillée (Vidéo ou Image HD)
    if (showFullVisualDialog && (imageResId != null || videoResId != null) && exercise != null) {
        Dialog(onDismissRequest = { showFullVisualDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = exercise.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = DuoTextDark,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { showFullVisualDialog = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DuoTextDark)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        if (videoResId != null) {
                            VideoPlayerView(videoResId = videoResId, modifier = Modifier.fillMaxSize())
                        } else if (imageResId != null) {
                            Image(
                                painter = painterResource(id = imageResId),
                                contentDescription = exercise.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "📋 CONSIGNES DE POSTURE EXACTES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = DuoBlueDark,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    exercise.executionSteps.forEach { step ->
                        Text(
                            text = step,
                            fontSize = 12.sp,
                            color = DuoTextDark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        )
                    }

                    if (exercise.commonMistakes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFFF2F2))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "⚠️ Erreur à éviter : ${exercise.commonMistakes}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = DuoRedDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    DuoButton(
                        text = "J'AI COMPRIS",
                        onClick = { showFullVisualDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        height = 42.dp
                    )
                }
            }
        }
    }
}
