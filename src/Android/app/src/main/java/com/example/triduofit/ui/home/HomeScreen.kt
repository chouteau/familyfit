package com.example.triduofit.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.triduofit.data.SettingsRepository
import com.example.triduofit.data.WorkoutProgramRepository
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
import com.example.triduofit.theme.DuoTextDark
import com.example.triduofit.theme.DuoTextMuted
import com.example.triduofit.ui.components.DuoButton
import com.example.triduofit.ui.components.DuoCard
import com.example.triduofit.ui.components.MascotMood
import com.example.triduofit.ui.components.MascotView

@Composable
fun HomeScreen(
    onStartWorkout: (WorkoutSession) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository(context) }
    val workoutRepo = remember { WorkoutProgramRepository() }

    val sessions = remember { workoutRepo.getWeeklySplitSessions() }
    val todaySession = sessions.firstOrNull { !it.isRestDay } ?: sessions.first()
    val streak = settingsRepo.getUserStreak()
    val xp = settingsRepo.getUserXp()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DuoBackgroundSubtle)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
    ) {
        // 1. Barre supérieure Duolingo (Streaks, XP, Paramètres)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flamme de Streak
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("🔥", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$streak JOURS",
                    fontWeight = FontWeight.Black,
                    color = DuoOrange,
                    fontSize = 15.sp
                )
            }

            // Points d'expérience (XP)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("⚡", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$xp XP",
                    fontWeight = FontWeight.Black,
                    color = DuoBlue,
                    fontSize = 15.sp
                )
            }

            // Bouton Réglages
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Text("⚙️", fontSize = 20.sp)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 2. Zone Mascotte & Message de motivation du coach
            item {
                DuoCard(
                    backgroundColor = Color.White,
                    borderColor = DuoGray,
                    shadowColor = DuoGrayDark
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MascotView(
                            size = 100.dp,
                            mood = MascotMood.CHEERING
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "COACH MOUETTE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = DuoOrangeDark,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Prêt pour ton renfo ? 35 minutes au poids de corps pour propulser ton triathlon !",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DuoTextDark,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // 3. Carte de la séance du jour mise en avant
            item {
                DuoCard(
                    backgroundColor = Color(0xFFF0FFF0),
                    borderColor = DuoGreen,
                    shadowColor = DuoGreenDark
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SÉANCE DU JOUR",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = DuoGreenDark,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "⏱️ ${todaySession.estimatedDurationMinutes} MIN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = DuoTextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = todaySession.splitType.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = DuoTextDark
                        )

                        Text(
                            text = todaySession.splitType.subtitle,
                            fontSize = 14.sp,
                            color = DuoTextMuted,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        DuoButton(
                            text = "LANCER LA SÉANCE",
                            color = DuoGreen,
                            shadowColor = DuoGreenDark,
                            onClick = { onStartWorkout(todaySession) }
                        )
                    }
                }
            }

            // 3bis. Carte spéciale "CIRCUIT POSTER 100% POIDS DU CORPS"
            item {
                val posterSession = remember { workoutRepo.getDedicatedPosterSession() }
                DuoCard(
                    backgroundColor = Color(0xFFFFF9E6),
                    borderColor = DuoOrange,
                    shadowColor = DuoOrangeDark,
                    onClick = { onStartWorkout(posterSession) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(DuoOrange),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📋", fontSize = 22.sp)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "CIRCUIT POSTER MUR",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    color = DuoOrangeDark,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "100% Poids de Corps (12 Ateliers)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = DuoTextDark
                                )
                                Text(
                                    text = "Burpees • Pompes • Fentes • Nageur • Gainage",
                                    fontSize = 12.sp,
                                    color = DuoTextMuted
                                )
                            }
                        }
                        Text("⚡", fontSize = 22.sp)
                    }
                }
            }

            // 4. Titre de la semaine en Split
            item {
                Text(
                    text = "PROGRAMME SPLIT DE LA SEMAINE",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = DuoTextMuted,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // 5. Liste des étapes du parcours (Chemin de progression)
            items(sessions) { session ->
                val isToday = (session.id == todaySession.id)
                val cardColor = if (session.isRestDay) Color(0xFFF8F8F8) else if (isToday) Color.White else Color(0xFFFAFAFA)
                val statusColor = if (session.isRestDay) DuoGrayDark else if (isToday) DuoGreen else DuoBlue

                DuoCard(
                    backgroundColor = cardColor,
                    borderColor = if (isToday) DuoGreen else DuoGray,
                    shadowColor = if (isToday) DuoGreenDark else DuoGrayDark,
                    onClick = {
                        if (!session.isRestDay) onStartWorkout(session)
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Badge circulaire du jour
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(statusColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = session.dayName.take(3).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = if (session.isRestDay) "Dimanche : Repos Actif" else session.splitType.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = DuoTextDark
                                )
                                Text(
                                    text = if (session.isRestDay) "Sortie vélo douce ou régénération" else "${session.exercises.size} exercices • ${session.estimatedDurationMinutes} min",
                                    fontSize = 13.sp,
                                    color = DuoTextMuted
                                )
                            }
                        }

                        Text(
                            text = if (session.isRestDay) "🛌" else if (isToday) "⭐" else "▶",
                            fontSize = 20.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
