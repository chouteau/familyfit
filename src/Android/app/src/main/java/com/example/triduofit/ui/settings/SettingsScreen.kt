package com.example.triduofit.ui.settings

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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.triduofit.audio.AudioCoachService
import com.example.triduofit.data.SettingsRepository
import com.example.triduofit.theme.DuoBackgroundSubtle
import com.example.triduofit.theme.DuoBlue
import com.example.triduofit.theme.DuoBlueDark
import com.example.triduofit.theme.DuoGray
import com.example.triduofit.theme.DuoGrayDark
import com.example.triduofit.theme.DuoGreen
import com.example.triduofit.theme.DuoGreenDark
import com.example.triduofit.theme.DuoOrange
import com.example.triduofit.theme.DuoOrangeDark
import com.example.triduofit.theme.DuoTextDark
import com.example.triduofit.theme.DuoTextMuted
import com.example.triduofit.ui.components.DuoButton
import com.example.triduofit.ui.components.DuoCard

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository(context) }

    var ttsEngine by remember { mutableStateOf(settingsRepo.getTtsEngine()) }
    var googleApiKey by remember { mutableStateOf(settingsRepo.getGoogleApiKey()) }
    var selectedGoogleVoice by remember { mutableStateOf(settingsRepo.getGoogleVoice()) }
    var openAiApiKey by remember { mutableStateOf(settingsRepo.getOpenAiApiKey()) }
    var selectedOpenAiVoice by remember { mutableStateOf(settingsRepo.getSelectedVoice()) }
    var technoEnabled by remember { mutableStateOf(settingsRepo.isTechnoEnabled()) }
    var technoBpm by remember { mutableIntStateOf(settingsRepo.getTechnoBpm()) }
    var dailyReminderEnabled by remember { mutableStateOf(settingsRepo.isDailyReminderEnabled()) }
    var reminderHour by remember { mutableIntStateOf(settingsRepo.getReminderHour()) }
    var saveMessage by remember { mutableStateOf("") }

    val audioCoach = remember { AudioCoachService(context, settingsRepo) {} }

    val googleVoices = listOf(
        "fr-FR-Journey-D" to "Journey-D (Masculin, dynamique & naturel)",
        "fr-FR-Journey-F" to "Journey-F (Féminin, fluide & chaleureux)",
        "fr-FR-Studio-A" to "Studio-A (Féminin, clarté studio pro)",
        "fr-FR-Studio-C" to "Studio-C (Féminin, posé & précis)",
        "fr-FR-Neural2-B" to "Neural2-B (Masculin, athlétique & direct)"
    )

    val openAiVoices = listOf(
        "onyx" to "Onyx (Masculin, grave & athlétique)",
        "nova" to "Nova (Féminin, énergique & chaleureux)",
        "alloy" to "Alloy (Neutre & équilibré)",
        "shimmer" to "Shimmer (Clair & motivant)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DuoBackgroundSubtle)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        // Barre supérieure
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Text("←", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DuoTextDark)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = "RÉGLAGES & IA",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = DuoTextDark
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Choix du Moteur Vocal IA
            item {
                DuoCard(
                    backgroundColor = Color.White,
                    borderColor = DuoGray,
                    shadowColor = DuoGrayDark
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "MOTEUR VOCAL DU COACH",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = DuoGreenDark,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sélectionne l'intelligence artificielle qui incarne ton coach :",
                            fontSize = 12.sp,
                            color = DuoTextMuted
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (ttsEngine == "google") DuoGreen else DuoGray)
                                    .clickable {
                                        ttsEngine = "google"
                                        settingsRepo.setTtsEngine("google")
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Google (1M gratuit)",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = if (ttsEngine == "google") Color.White else DuoTextDark
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (ttsEngine == "openai") DuoBlue else DuoGray)
                                    .clickable {
                                        ttsEngine = "openai"
                                        settingsRepo.setTtsEngine("openai")
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "OpenAI (TTS-1)",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = if (ttsEngine == "openai") Color.White else DuoTextDark
                                )
                            }
                        }
                    }
                }
            }

            // 2. Configuration Google Cloud TTS (Si sélectionné)
            if (ttsEngine == "google") {
                item {
                    DuoCard(
                        backgroundColor = Color.White,
                        borderColor = DuoGray,
                        shadowColor = DuoGrayDark
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "CLÉ API GOOGLE CLOUD TTS",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = DuoGreenDark,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1 000 000 de caractères gratuits par mois à vie. Voix Studio & Journey ultra-naturelles.",
                                fontSize = 12.sp,
                                color = DuoTextMuted
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = googleApiKey,
                                onValueChange = { googleApiKey = it },
                                placeholder = { Text("AIzaSy...") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            DuoButton(
                                text = "ENREGISTRER LA CLÉ GOOGLE",
                                color = DuoGreen,
                                shadowColor = DuoGreenDark,
                                height = 44.dp,
                                onClick = {
                                    settingsRepo.setGoogleApiKey(googleApiKey)
                                    saveMessage = "Clé Google enregistrée !"
                                }
                            )

                            if (saveMessage.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "✓ $saveMessage",
                                    color = DuoGreenDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                item {
                    DuoCard(
                        backgroundColor = Color.White,
                        borderColor = DuoGray,
                        shadowColor = DuoGrayDark
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "VOIX FRANÇAISES GOOGLE",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = DuoBlueDark,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            googleVoices.forEach { (code, label) ->
                                val isSelected = (selectedGoogleVoice == code)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFEBF7FF) else Color(0xFFF7F7F7))
                                        .clickable {
                                            selectedGoogleVoice = code
                                            settingsRepo.setGoogleVoice(code)
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(if (isSelected) "●" else "○", color = if (isSelected) DuoBlue else DuoTextMuted)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) DuoBlueDark else DuoTextDark,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            DuoButton(
                                text = "TESTER LA VOIX DU COACH",
                                color = DuoBlue,
                                shadowColor = DuoBlueDark,
                                height = 44.dp,
                                onClick = {
                                    audioCoach.speak("Allez champion ! Squats bulgares pied sur le cube, descends à 90 degrés et pousse dans le talon !")
                                }
                            )
                        }
                    }
                }
            } else {
                // Configuration OpenAI
                item {
                    DuoCard(
                        backgroundColor = Color.White,
                        borderColor = DuoGray,
                        shadowColor = DuoGrayDark
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "CLÉ API OPENAI (TTS & WHISPER)",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = DuoBlueDark,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Clé API OpenAI pour les voix Onyx, Nova et la reconnaissance vocale.",
                                fontSize = 12.sp,
                                color = DuoTextMuted
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = openAiApiKey,
                                onValueChange = { openAiApiKey = it },
                                placeholder = { Text("sk-proj-...") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            DuoButton(
                                text = "ENREGISTRER LA CLÉ OPENAI",
                                color = DuoBlue,
                                shadowColor = DuoBlueDark,
                                height = 44.dp,
                                onClick = {
                                    settingsRepo.setOpenAiApiKey(openAiApiKey)
                                    saveMessage = "Clé OpenAI enregistrée !"
                                }
                            )
                        }
                    }
                }

                item {
                    DuoCard(
                        backgroundColor = Color.White,
                        borderColor = DuoGray,
                        shadowColor = DuoGrayDark
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "VOIX OPENAI",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = DuoBlueDark,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            openAiVoices.forEach { (code, label) ->
                                val isSelected = (selectedOpenAiVoice == code)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFEBF7FF) else Color(0xFFF7F7F7))
                                        .clickable {
                                            selectedOpenAiVoice = code
                                            settingsRepo.setSelectedVoice(code)
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(if (isSelected) "●" else "○", color = if (isSelected) DuoBlue else DuoTextMuted)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) DuoBlueDark else DuoTextDark,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            DuoButton(
                                text = "TESTER LA VOIX DU COACH",
                                color = DuoBlue,
                                shadowColor = DuoBlueDark,
                                height = 44.dp,
                                onClick = {
                                    audioCoach.speak("Allez champion ! 12 répétitions de squats bulgares, pousse fort !")
                                }
                            )
                        }
                    }
                }
            }

            // 3. Section Musique Techno
            item {
                DuoCard(
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
                            Column {
                                Text(
                                    text = "MUSIQUE TECHNO DE FOND",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = DuoOrange,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Rythme avec Audio Ducking automatique",
                                    fontSize = 12.sp,
                                    color = DuoTextMuted
                                )
                            }
                            Switch(
                                checked = technoEnabled,
                                onCheckedChange = {
                                    technoEnabled = it
                                    settingsRepo.setTechnoEnabled(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = DuoGreen
                                )
                            )
                        }

                        if (technoEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "CADENCE : $technoBpm BPM",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = DuoTextDark
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(130, 135, 140, 145).forEach { bpm ->
                                    val isSelected = (technoBpm == bpm)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) DuoOrange else DuoGray)
                                            .clickable {
                                                technoBpm = bpm
                                                settingsRepo.setTechnoBpm(bpm)
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$bpm",
                                            fontWeight = FontWeight.Black,
                                            color = if (isSelected) Color.White else DuoTextDark,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3bis. Section Rappels Quotidiens Duolingo (9h00)
            item {
                DuoCard(
                    backgroundColor = Color(0xFFFFF9E6),
                    borderColor = DuoOrange,
                    shadowColor = DuoOrangeDark
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "RAPPEL QUOTIDIEN COACH MOUETTE (9H00)",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = DuoOrangeDark,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Notifications marrantes style Duolingo pour te motiver à 9h",
                                    fontSize = 12.sp,
                                    color = DuoTextMuted
                                )
                            }
                            Switch(
                                checked = dailyReminderEnabled,
                                onCheckedChange = {
                                    dailyReminderEnabled = it
                                    settingsRepo.setDailyReminderEnabled(it)
                                    if (it) {
                                        com.example.triduofit.notification.NotificationScheduler.scheduleDailyReminder(context)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = DuoOrange
                                )
                            )
                        }

                        if (dailyReminderEnabled) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "⏰ HEURE DU RAPPEL : ${reminderHour}h00 chaque matin",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DuoTextDark
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            DuoButton(
                                text = "🔔 TESTER LA NOTIFICATION DUOLINGO",
                                color = DuoOrange,
                                shadowColor = DuoOrangeDark,
                                height = 44.dp,
                                onClick = {
                                    com.example.triduofit.notification.NotificationScheduler.sendNotification(context, isTest = true)
                                }
                            )
                        }
                    }
                }
            }

            // 4. Section Garmin
            item {
                DuoCard(
                    backgroundColor = Color(0xFFF0F7FF),
                    borderColor = DuoBlue,
                    shadowColor = DuoBlueDark
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "COMPTE DÉVELOPPEUR GARMIN",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = DuoBlueDark,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Générateur .FIT (Strength Training) activé.\n• Chaque séance validée exporte la durée, les séries et les calories dans l'écosystème Garmin.",
                            fontSize = 13.sp,
                            color = DuoTextDark,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
