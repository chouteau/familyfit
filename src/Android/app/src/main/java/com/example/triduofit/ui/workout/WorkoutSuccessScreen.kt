package com.example.triduofit.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.triduofit.ui.components.MascotMood
import com.example.triduofit.ui.components.MascotView

@Composable
fun WorkoutSuccessScreen(
    completed: Boolean = true,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository(context) }
    val streak = settingsRepo.getUserStreak()
    val xp = settingsRepo.getUserXp()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DuoBackgroundSubtle)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // 1. Mascotte en fête & confettis
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎉 ⭐ 🏆 ⭐ 🎉", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(12.dp))
            MascotView(size = 140.dp, mood = MascotMood.CELEBRATING)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (completed) "SÉANCE TERMINÉE !" else "SÉANCE ENREGISTRÉE",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = DuoGreenDark,
                letterSpacing = 1.sp
            )
            Text(
                text = if (completed) "Un pas de plus pour garder le rythme en famille !" else "Ton effort compte. Le suivi indiquera une séance partielle ou abandonnée.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DuoTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // 2. Récapitulatif des gains
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Streak
                DuoCard(
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color.White,
                    borderColor = DuoOrange,
                    shadowColor = DuoOrangeDark
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔥", fontSize = 30.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$streak JOURS",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = DuoOrange
                        )
                        Text(
                            text = "SÉRIE EN COURS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = DuoTextMuted
                        )
                    }
                }

                // XP
                DuoCard(
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color.White,
                    borderColor = DuoBlue,
                    shadowColor = DuoBlueDark
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚡", fontSize = 30.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (completed) "+60 XP" else "EFFORT CONSERVÉ",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = DuoBlue
                        )
                        Text(
                            text = "TOTAL : $xp XP",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = DuoTextMuted
                        )
                    }
                }
            }

            // Badge Garmin
            DuoCard(
                backgroundColor = Color(0xFFF0F7FF),
                borderColor = DuoBlue,
                shadowColor = DuoBlueDark
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("⌚", fontSize = 30.sp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "SUIVI FAMILIAL",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = DuoBlueDark,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Les résultats du programme familial sont conservés sur ce téléphone puis envoyés au site quand le réseau est disponible.",
                            fontSize = 13.sp,
                            color = DuoTextDark,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // 3. Bouton Continuer
        DuoButton(
            text = "CONTINUER",
            color = DuoGreen,
            shadowColor = DuoGreenDark,
            onClick = onContinue
        )
    }
}
