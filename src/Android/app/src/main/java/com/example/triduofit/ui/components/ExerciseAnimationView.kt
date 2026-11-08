package com.example.triduofit.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.triduofit.model.AnimationType
import com.example.triduofit.theme.DuoBlue
import com.example.triduofit.theme.DuoBlueDark
import com.example.triduofit.theme.DuoGreen
import com.example.triduofit.theme.DuoOrange
import com.example.triduofit.theme.DuoRed
import com.example.triduofit.theme.DuoTextDark
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Fonction de cinématique inverse 2D exacte (IK) à segments rigides
// Garantit mathématiquement que la cuisse et le tibia conservent une longueur 100% fixe (aucun étirement "elasticman")
private fun solve2BoneIK(
    p1: Offset,
    p2: Offset,
    l1: Float,
    l2: Float,
    bendPositive: Boolean
): Offset {
    val dx = (p2.x - p1.x).toDouble()
    val dy = (p2.y - p1.y).toDouble()
    val dRaw = kotlin.math.hypot(dx, dy).toFloat()
    val maxD = (l1 + l2) * 0.998f
    val minD = kotlin.math.abs(l1 - l2) + 0.5f
    val d = dRaw.coerceIn(minD, maxD)
    val gamma = kotlin.math.atan2(dy, dx)
    val cosA = ((l1 * l1 + d * d - l2 * l2) / (2f * l1 * d)).coerceIn(-1f, 1f)
    val a = kotlin.math.acos(cosA.toDouble())
    val angle = if (bendPositive) gamma + a else gamma - a
    return Offset(
        (p1.x + l1 * kotlin.math.cos(angle)).toFloat(),
        (p1.y + l1 * kotlin.math.sin(angle)).toFloat()
    )
}

@Composable
fun ExerciseAnimationView(
    animationType: AnimationType,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "exercise_movement")

    // Rotation continue 0 -> 2*PI pour les mouvements cycliques (Vélo Elliptique)
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )

    // Progression 0.0f -> 1.0f -> 0.0f pour les mouvements de va-et-vient
    val progress = (sin(rotationAngle - PI.toFloat() / 2f) + 1f) / 2f

    Box(
        modifier = modifier.padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val woodColor = Color(0xFFC88B52)
            val woodDark = Color(0xFFA56834)
            val metalColor = Color(0xFF3A3A3A)
            val padColor = DuoRed
            val athleteSkin = Color(0xFFFFD199)
            val athleteClothes = DuoBlue
            val strokeThickness = 12f

            when (animationType) {
                AnimationType.ELLIPTICAL_CARDIO -> {
                    // Sol
                    drawLine(Color(0xFFE2E4E8), Offset(0f, h * 0.90f), Offset(w, h * 0.90f), 5f)

                    // Couleurs carrosserie NordicTrack
                    val chassisDark = Color(0xFF1E2124)
                    val chassisSilver = Color(0xFF909AA2)
                    val chassisHighlight = Color(0xFF33383E)
                    val accentRed = DuoRed
                    val consoleScreen = Color(0xFF1CB0F6)

                    val theta = rotationAngle
                    val thetaOpposite = theta + PI.toFloat()

                    // Position du volant d'inertie avant (Front Drive Flywheel)
                    val flyCenterX = w * 0.28f
                    val flyCenterY = h * 0.70f
                    val crankR = 26f

                    // Trajectoire des manivelles avant
                    val crank1X = flyCenterX + crankR * cos(theta)
                    val crank1Y = flyCenterY + crankR * sin(theta)

                    val crank2X = flyCenterX + crankR * cos(thetaOpposite)
                    val crank2Y = flyCenterY + crankR * sin(thetaOpposite)

                    // Patins arrières glissant sur le rail
                    val railY = h * 0.86f
                    val rearTravel = 32f
                    val slide1X = w * 0.72f + rearTravel * cos(theta)
                    val slide2X = w * 0.72f + rearTravel * cos(thetaOpposite)

                    // Position des repose-pieds (pédales)
                    val pedal1X = crank1X * 0.38f + slide1X * 0.62f
                    val pedal1Y = crank1Y * 0.38f + railY * 0.62f

                    val pedal2X = crank2X * 0.38f + slide2X * 0.62f
                    val pedal2Y = crank2Y * 0.38f + railY * 0.62f

                    // Poignées des bras oscillants articulés
                    val mastPivotX = w * 0.35f
                    val mastPivotY = h * 0.42f
                    val handleSwing = 34f
                    val handle1X = mastPivotX - handleSwing * cos(theta)
                    val handle1Y = h * 0.25f

                    val handle2X = mastPivotX - handleSwing * cos(thetaOpposite)
                    val handle2Y = h * 0.25f

                    // === 1. COUCHE ARRIÈRE (Far Leg & Far Handlebar - Effet 3D profondeur) ===
                    val farLegColor = Color(0xFF333333)
                    val farClothesColor = Color(0xFF126E99)
                    val farSkinColor = Color(0xFFDEB07D)

                    // Barre de pédale arrière
                    drawLine(Color(0xFF555555), Offset(crank2X, crank2Y), Offset(slide2X, railY), 10f, StrokeCap.Round)
                    // Repose-pied arrière
                    drawRoundRect(Color(0xFF222222), Offset(pedal2X - 22f, pedal2Y - 7f), Size(44f, 12f), CornerRadius(4f, 4f))

                    // Bras oscillant arrière
                    drawLine(chassisSilver, Offset(mastPivotX, mastPivotY), Offset(handle2X, handle2Y), 8f, StrokeCap.Round)
                    drawLine(chassisSilver, Offset(mastPivotX, mastPivotY), Offset(crank2X, crank2Y), 8f, StrokeCap.Round)
                    drawLine(Color(0xFF111111), Offset(handle2X + 2f, handle2Y + 22f), Offset(handle2X, handle2Y), 12f, StrokeCap.Round)

                    // Jambe arrière articulée (Hanche -> Genou -> Pied)
                    val hipX = w * 0.54f
                    val hipY = h * 0.50f + (sin(theta * 2f) * 4f)
                    val knee2X = (hipX + pedal2X) / 2f - 18f
                    val knee2Y = (hipY + pedal2Y) / 2f - 12f
                    drawLine(farClothesColor, Offset(hipX, hipY), Offset(knee2X, knee2Y), 12f, StrokeCap.Round)
                    drawLine(farLegColor, Offset(knee2X, knee2Y), Offset(pedal2X, pedal2Y), 10f, StrokeCap.Round)

                    // Bras arrière (Épaule -> Coude -> Poignée)
                    val shoulderX = w * 0.48f
                    val shoulderY = h * 0.35f
                    val elbow2X = (shoulderX + handle2X) / 2f - 8f
                    val elbow2Y = (shoulderY + handle2Y) / 2f + 14f
                    drawLine(farSkinColor, Offset(shoulderX, shoulderY), Offset(elbow2X, elbow2Y), 9f, StrokeCap.Round)
                    drawLine(farSkinColor, Offset(elbow2X, elbow2Y), Offset(handle2X, handle2Y + 8f), 9f, StrokeCap.Round)

                    // === 2. COUCHE CENTRALE (Châssis NordicTrack) ===
                    // Rail double au sol
                    drawLine(chassisDark, Offset(w * 0.16f, h * 0.88f), Offset(w * 0.86f, h * 0.88f), 16f, StrokeCap.Round)
                    drawLine(chassisSilver, Offset(w * 0.42f, railY), Offset(w * 0.82f, railY), 6f)
                    // Stabilisateur arrière avec roulettes
                    drawRoundRect(chassisDark, Offset(w * 0.82f, h * 0.84f), Size(36f, 16f), CornerRadius(6f, 6f))

                    // Carénage profilé du Volant d'inertie avant
                    drawCircle(chassisDark, 46f, Offset(flyCenterX, flyCenterY))
                    drawCircle(chassisHighlight, 40f, Offset(flyCenterX, flyCenterY))
                    drawCircle(chassisSilver, 26f, Offset(flyCenterX, flyCenterY), style = Stroke(width = 5f))
                    drawCircle(accentRed, 12f, Offset(flyCenterX, flyCenterY))
                    // Manivelle avant
                    drawLine(chassisSilver, Offset(flyCenterX, flyCenterY), Offset(crank1X, crank1Y), 9f, StrokeCap.Round)

                    // Mât central robuste
                    drawLine(chassisDark, Offset(w * 0.28f, h * 0.84f), Offset(w * 0.36f, h * 0.32f), 18f, StrokeCap.Round)
                    drawLine(chassisHighlight, Offset(w * 0.30f, h * 0.82f), Offset(w * 0.36f, h * 0.34f), 8f)

                    // Console digitale NordicTrack
                    drawRoundRect(chassisDark, Offset(w * 0.34f, h * 0.22f), Size(38f, 26f), CornerRadius(6f, 6f))
                    drawRoundRect(consoleScreen, Offset(w * 0.36f, h * 0.24f), Size(30f, 16f), CornerRadius(3f, 3f))
                    // Guidon fixe intérieur
                    drawLine(Color(0xFF222222), Offset(w * 0.37f, h * 0.36f), Offset(w * 0.44f, h * 0.38f), 9f, StrokeCap.Round)

                    // === 3. COUCHE AVANT (Corps Athlète & Membres Avant) ===
                    // Tête athlète avec bandeau orange
                    drawCircle(athleteSkin, 22f, Offset(shoulderX - 4f, h * 0.22f))
                    drawRoundRect(DuoOrange, Offset(shoulderX - 22f, h * 0.17f), Size(36f, 9f), CornerRadius(3f, 3f))

                    // Torse incliné vers l'avant (position athlétique)
                    val torsoPath = Path().apply {
                        moveTo(shoulderX + 10f, shoulderY)
                        lineTo(shoulderX - 16f, shoulderY + 14f)
                        lineTo(hipX - 12f, hipY)
                        lineTo(hipX + 10f, hipY - 6f)
                        close()
                    }
                    drawPath(torsoPath, DuoBlue)
                    drawLine(DuoBlueDark, Offset(shoulderX + 6f, shoulderY + 4f), Offset(hipX + 8f, hipY - 6f), 4f)

                    // Jambe avant articulée (Hanche -> Genou -> Pied)
                    val knee1X = (hipX + pedal1X) / 2f - 20f
                    val knee1Y = (hipY + pedal1Y) / 2f - 14f
                    drawLine(DuoBlue, Offset(hipX, hipY), Offset(knee1X, knee1Y), 15f, StrokeCap.Round)
                    drawLine(DuoTextDark, Offset(knee1X, knee1Y), Offset(pedal1X, pedal1Y), 12f, StrokeCap.Round)

                    // Barre de pédale avant
                    drawLine(chassisSilver, Offset(crank1X, crank1Y), Offset(slide1X, railY), 12f, StrokeCap.Round)
                    // Repose-pied avant grand format
                    drawRoundRect(Color(0xFF1E2124), Offset(pedal1X - 24f, pedal1Y - 8f), Size(48f, 14f), CornerRadius(5f, 5f))
                    drawLine(accentRed, Offset(pedal1X - 20f, pedal1Y - 8f), Offset(pedal1X + 20f, pedal1Y - 8f), 3f)

                    // Bras oscillant avant
                    drawLine(chassisSilver, Offset(mastPivotX, mastPivotY), Offset(handle1X, handle1Y), 10f, StrokeCap.Round)
                    drawLine(chassisSilver, Offset(mastPivotX, mastPivotY), Offset(crank1X, crank1Y), 10f, StrokeCap.Round)
                    drawLine(Color(0xFF111111), Offset(handle1X + 2f, handle1Y + 24f), Offset(handle1X, handle1Y), 14f, StrokeCap.Round)

                    // Bras avant (Épaule -> Coude -> Poignée)
                    val elbow1X = (shoulderX + handle1X) / 2f - 10f
                    val elbow1Y = (shoulderY + handle1Y) / 2f + 16f
                    drawLine(athleteSkin, Offset(shoulderX, shoulderY), Offset(elbow1X, elbow1Y), 11f, StrokeCap.Round)
                    drawLine(athleteSkin, Offset(elbow1X, elbow1Y), Offset(handle1X, handle1Y + 10f), 11f, StrokeCap.Round)
                }

                AnimationType.BALANCE_BOARD_STABILITY -> {
                    drawLine(Color(0xFFE0E0E0), Offset(0f, h * 0.88f), Offset(w, h * 0.88f), 6f)

                    val centerX = w * 0.5f
                    val baseY = h * 0.85f
                    drawCircle(metalColor, 30f, Offset(centerX, baseY))

                    val tiltAngle = (progress - 0.5f) * 30f
                    val tiltYLeft = baseY - 8f + (tiltAngle * 0.5f)
                    val tiltYRight = baseY - 8f - (tiltAngle * 0.5f)

                    drawLine(DuoRed, Offset(w * 0.28f, tiltYLeft), Offset(w * 0.72f, tiltYRight), 16f, StrokeCap.Round)
                    drawLine(Color(0xFFEEEEEE), Offset(w * 0.30f, tiltYLeft - 2f), Offset(w * 0.70f, tiltYRight - 2f), 4f)

                    val athleteX = centerX - (tiltAngle * 0.3f)
                    val feetY = (tiltYLeft + tiltYRight) / 2f - 6f
                    val headY = feetY - 145f

                    drawCircle(athleteSkin, 24f, Offset(athleteX, headY))
                    drawLine(athleteClothes, Offset(athleteX, headY + 24f), Offset(athleteX, feetY - 60f), strokeThickness, StrokeCap.Round)
                    drawLine(athleteSkin, Offset(athleteX - 45f, headY + 50f + tiltAngle * 0.4f), Offset(athleteX + 45f, headY + 50f - tiltAngle * 0.4f), 10f, StrokeCap.Round)
                    drawLine(DuoTextDark, Offset(athleteX, feetY - 60f), Offset(w * 0.42f, tiltYLeft - 6f), strokeThickness, StrokeCap.Round)
                    drawLine(DuoTextDark, Offset(athleteX, feetY - 60f), Offset(w * 0.58f, tiltYRight - 6f), strokeThickness, StrokeCap.Round)
                }

                AnimationType.PARALLEL_BARS_DIPS -> {
                    drawLine(Color(0xFFE0E0E0), Offset(0f, h * 0.88f), Offset(w, h * 0.88f), 6f)

                    val bar1X = w * 0.36f
                    val bar2X = w * 0.64f
                    val barTopY = h * 0.52f

                    drawLine(metalColor, Offset(bar1X - 35f, h * 0.88f), Offset(bar1X + 35f, h * 0.88f), 12f, StrokeCap.Round)
                    drawLine(metalColor, Offset(bar2X - 35f, h * 0.88f), Offset(bar2X + 35f, h * 0.88f), 12f, StrokeCap.Round)
                    drawLine(metalColor, Offset(bar1X, h * 0.88f), Offset(bar1X, barTopY), 14f, StrokeCap.Round)
                    drawLine(metalColor, Offset(bar2X, h * 0.88f), Offset(bar2X, barTopY), 14f, StrokeCap.Round)
                    drawRoundRect(Color(0xFF1E1E1E), Offset(bar1X - 25f, barTopY - 6f), Size(50f, 14f), CornerRadius(6f, 6f))
                    drawRoundRect(Color(0xFF1E1E1E), Offset(bar2X - 25f, barTopY - 6f), Size(50f, 14f), CornerRadius(6f, 6f))

                    val dipDepth = progress * 45f
                    val headY = (barTopY - 95f) + dipDepth
                    val shoulderY = headY + 26f
                    val hipY = shoulderY + 65f

                    drawCircle(athleteSkin, 24f, Offset(w * 0.5f, headY))
                    drawLine(athleteClothes, Offset(w * 0.5f, shoulderY), Offset(w * 0.5f, hipY), strokeThickness, StrokeCap.Round)
                    drawLine(athleteSkin, Offset(w * 0.5f, shoulderY), Offset(bar1X, barTopY), 10f, StrokeCap.Round)
                    drawLine(athleteSkin, Offset(w * 0.5f, shoulderY), Offset(bar2X, barTopY), 10f, StrokeCap.Round)
                    drawLine(DuoTextDark, Offset(w * 0.5f, hipY), Offset(w * 0.44f, hipY + 45f), strokeThickness, StrokeCap.Round)
                    drawLine(DuoTextDark, Offset(w * 0.44f, hipY + 45f), Offset(w * 0.38f, hipY + 35f), strokeThickness, StrokeCap.Round)
                }

                AnimationType.PARALLEL_BARS_INVERTED_ROW -> {
                    val barX = w * 0.5f
                    val barTopY = h * 0.45f
                    drawLine(metalColor, Offset(w * 0.25f, barTopY), Offset(w * 0.75f, barTopY), 16f, StrokeCap.Round)
                    drawLine(metalColor, Offset(w * 0.28f, h * 0.88f), Offset(w * 0.28f, barTopY), 14f, StrokeCap.Round)
                    drawLine(metalColor, Offset(w * 0.72f, h * 0.88f), Offset(w * 0.72f, barTopY), 14f, StrokeCap.Round)

                    val pullProgress = progress
                    val chestY = (barTopY + 70f) - (pullProgress * 45f)
                    val headY = chestY - 26f
                    val feetX = w * 0.78f
                    val feetY = h * 0.84f

                    drawCircle(athleteSkin, 22f, Offset(w * 0.38f, headY))
                    drawLine(athleteClothes, Offset(w * 0.40f, chestY), Offset(feetX - 40f, feetY - 20f), strokeThickness, StrokeCap.Round)
                    drawLine(DuoTextDark, Offset(feetX - 40f, feetY - 20f), Offset(feetX, feetY), strokeThickness, StrokeCap.Round)
                    drawLine(athleteSkin, Offset(w * 0.42f, chestY), Offset(barX, barTopY + 4f), 10f, StrokeCap.Round)
                }

                AnimationType.PLYO_BOX_BULGARIAN_SQUAT -> {
                    // Sol
                    val groundY = h * 0.86f
                    drawLine(Color(0xFFDCDFE4), Offset(0f, groundY), Offset(w, groundY), 6f)

                    // 1. CUBE EN BOIS (Plyo Box aux proportions humaines réelles)
                    val boxW = w * 0.26f
                    val boxH = h * 0.32f
                    val boxLeft = w * 0.52f
                    val boxTop = groundY - boxH

                    // Corps du cube en bois avec biseau, vis et veinures réalistes
                    drawRoundRect(woodColor, Offset(boxLeft, boxTop), Size(boxW, boxH), CornerRadius(8f, 8f))
                    drawRoundRect(woodDark, Offset(boxLeft, boxTop), Size(boxW, boxH), CornerRadius(8f, 8f), style = Stroke(width = 3f))
                    // Planches et jointures
                    drawLine(woodDark, Offset(boxLeft, boxTop + boxH * 0.33f), Offset(boxLeft + boxW, boxTop + boxH * 0.33f), 2.5f)
                    drawLine(woodDark, Offset(boxLeft, boxTop + boxH * 0.66f), Offset(boxLeft + boxW, boxTop + boxH * 0.66f), 2.5f)
                    // Poignée ovale découpée
                    drawRoundRect(Color(0xFF6B3E18), Offset(boxLeft + boxW * 0.32f, boxTop + boxH * 0.38f), Size(boxW * 0.36f, 16f), CornerRadius(8f, 8f))
                    // Vis métalliques aux 4 coins
                    drawCircle(metalColor, 3f, Offset(boxLeft + 8f, boxTop + 8f))
                    drawCircle(metalColor, 3f, Offset(boxLeft + boxW - 8f, boxTop + 8f))
                    drawCircle(metalColor, 3f, Offset(boxLeft + 8f, groundY - 8f))
                    drawCircle(metalColor, 3f, Offset(boxLeft + boxW - 8f, groundY - 8f))

                    // 2. BIOMÉCANIQUE À SEGMENTS RIGIDES INVARIANTS (Zéro effet "Elasticman")
                    // Longueurs osseuses humaines fixes (ne changent jamais pendant l'animation)
                    val lThigh = h * 0.22f
                    val lShin = h * 0.22f
                    val lTorso = h * 0.26f

                    // Ancrages des pieds (distance biométrique de fente naturelle ~1 mètre)
                    val frontFoot = Offset(w * 0.32f, groundY)
                    val rearFoot = Offset(boxLeft + 14f, boxTop)

                    // Trajectoire de descente du bassin (de debout à la flexion 90°)
                    val hipStanding = Offset(frontFoot.x + lThigh * 0.42f, groundY - (lThigh + lShin) * 0.95f)
                    val hipSquat = Offset(frontFoot.x + lThigh * 0.98f, groundY - lShin * 1.01f)

                    val hipX = hipStanding.x + progress * (hipSquat.x - hipStanding.x)
                    val hipY = hipStanding.y + progress * (hipSquat.y - hipStanding.y)
                    val hip = Offset(hipX, hipY)

                    // Résolution cinématique exacte des genoux (angle avant qui plie vers l'AVANT, arrière vers le SOL)
                    val frontKnee = solve2BoneIK(hip, frontFoot, lThigh, lShin, bendPositive = true)
                    val rearKnee = solve2BoneIK(hip, rearFoot, lThigh, lShin, bendPositive = true)

                    // Buste et tête
                    val shoulder = Offset(hip.x - lTorso * 0.15f, hip.y - lTorso * 0.98f)
                    val head = Offset(shoulder.x - 6f, shoulder.y - 18f)

                    // Bras (mains jointes au sternum en équilibre de course)
                    val hands = Offset(shoulder.x + 8f, shoulder.y + lTorso * 0.45f)
                    val elbow = solve2BoneIK(shoulder, hands, lThigh * 0.52f, lThigh * 0.48f, bendPositive = false)

                    // 3. RENDU ANATOMIQUE RÉALISTE AVEC ÉPAISSEUR MUSCULAIRE

                    // --- Jambe arrière (plan arrière) ---
                    val rearSkinColor = Color(0xFFDEB07D)
                    // Cuisse arrière
                    drawLine(rearSkinColor, hip, rearKnee, 16f, StrokeCap.Round)
                    // Mollet arrière
                    drawLine(rearSkinColor, rearKnee, rearFoot, 13f, StrokeCap.Round)
                    // Chaussure arrière posée sur le cube (lacets sur le bois, semelle blanche visible)
                    drawLine(Color(0xFF1E293B), Offset(rearFoot.x - 4f, rearFoot.y - 5f), Offset(rearFoot.x + 22f, rearFoot.y - 5f), 10f, StrokeCap.Round)
                    drawLine(Color.White, Offset(rearFoot.x - 2f, rearFoot.y - 9f), Offset(rearFoot.x + 20f, rearFoot.y - 9f), 3.5f, StrokeCap.Round)

                    // --- Jambe avant (moteur de l'exercice) ---
                    // Cuisse avant avec galbe musculaire du quadriceps
                    drawLine(athleteSkin, hip, frontKnee, 18f, StrokeCap.Round)
                    // Mollet avant
                    drawLine(athleteSkin, frontKnee, frontFoot, 15f, StrokeCap.Round)
                    // Chaussette de sport blanche à la cheville
                    drawLine(Color.White, Offset(frontFoot.x, frontFoot.y - 18f), Offset(frontFoot.x, frontFoot.y - 4f), 13f, StrokeCap.Round)
                    // Chaussure avant avec semelle d'appui épaisse à plat au sol
                    drawLine(Color(0xFF1E293B), Offset(frontFoot.x - 14f, frontFoot.y - 6f), Offset(frontFoot.x + 18f, frontFoot.y - 6f), 12f, StrokeCap.Round)
                    drawLine(Color.White, Offset(frontFoot.x - 14f, frontFoot.y - 2f), Offset(frontFoot.x + 18f, frontFoot.y - 2f), 4f, StrokeCap.Round)

                    // Short de sport (bassin et haut des cuisses)
                    val shortsPath = Path().apply {
                        moveTo(hip.x - 12f, hip.y - 8f)
                        lineTo(hip.x + 12f, hip.y - 8f)
                        lineTo(frontKnee.x * 0.4f + hip.x * 0.6f + 8f, frontKnee.y * 0.4f + hip.y * 0.6f)
                        lineTo(frontKnee.x * 0.4f + hip.x * 0.6f - 8f, frontKnee.y * 0.4f + hip.y * 0.6f)
                        close()
                    }
                    drawPath(shortsPath, Color(0xFF1E293B))

                    // Torse athlétique gainé
                    val torsoPath = Path().apply {
                        moveTo(shoulder.x + 12f, shoulder.y)
                        lineTo(shoulder.x - 10f, shoulder.y + 6f)
                        lineTo(hip.x - 10f, hip.y)
                        lineTo(hip.x + 10f, hip.y - 4f)
                        close()
                    }
                    drawPath(torsoPath, athleteClothes)

                    // Bras en équilibre de course
                    drawLine(athleteSkin, shoulder, elbow, 11f, StrokeCap.Round)
                    drawLine(athleteSkin, elbow, hands, 10f, StrokeCap.Round)

                    // Tête avec bandeau cyan
                    drawCircle(athleteSkin, 17f, head)
                    drawRoundRect(DuoBlueDark, Offset(head.x - 15f, head.y - 11f), Size(30f, 6.5f), CornerRadius(3f, 3f))

                    // 4. REPÈRES VISUELS BIOMÉCANIQUES (Guide 90° & Poussée)
                    if (progress > 0.6f) {
                        // Cercle vert à l'angle du genou à 90°
                        drawCircle(DuoGreen, 8f, frontKnee)
                        drawCircle(Color.White, 4f, frontKnee)
                        // Ligne verticale d'alignement cheville-genou
                        drawLine(DuoGreen, frontKnee, frontFoot, 2.5f, StrokeCap.Round)
                    }

                    // Flèche dynamique de poussée / descente
                    val arrowX = frontFoot.x - 40f
                    if (progress > 0.5f) {
                        // Poussée talon vers le haut
                        drawLine(DuoGreen, Offset(arrowX, groundY - 15f), Offset(arrowX, groundY - 55f), 5f, StrokeCap.Round)
                        drawLine(DuoGreen, Offset(arrowX - 8f, groundY - 42f), Offset(arrowX, groundY - 55f), 5f, StrokeCap.Round)
                        drawLine(DuoGreen, Offset(arrowX + 8f, groundY - 42f), Offset(arrowX, groundY - 55f), 5f, StrokeCap.Round)
                    } else {
                        // Descente vers le bas
                        drawLine(DuoOrange, Offset(arrowX, groundY - 55f), Offset(arrowX, groundY - 15f), 5f, StrokeCap.Round)
                        drawLine(DuoOrange, Offset(arrowX - 8f, groundY - 28f), Offset(arrowX, groundY - 15f), 5f, StrokeCap.Round)
                        drawLine(DuoOrange, Offset(arrowX + 8f, groundY - 28f), Offset(arrowX, groundY - 15f), 5f, StrokeCap.Round)
                    }
                }

                AnimationType.PLYO_BOX_STEP_UP -> {
                    val groundY = h * 0.86f
                    drawLine(Color(0xFFDCDFE4), Offset(0f, groundY), Offset(w, groundY), 6f)

                    val boxW = w * 0.28f
                    val boxH = h * 0.32f
                    val boxLeft = w * 0.48f
                    val boxTop = groundY - boxH

                    drawRoundRect(woodColor, Offset(boxLeft, boxTop), Size(boxW, boxH), CornerRadius(8f, 8f))
                    drawRoundRect(woodDark, Offset(boxLeft, boxTop), Size(boxW, boxH), CornerRadius(8f, 8f), style = Stroke(width = 3f))
                    drawLine(woodDark, Offset(boxLeft, boxTop + boxH * 0.5f), Offset(boxLeft + boxW, boxTop + boxH * 0.5f), 2.5f)
                    drawRoundRect(Color(0xFF6B3E18), Offset(boxLeft + boxW * 0.32f, boxTop + boxH * 0.35f), Size(boxW * 0.36f, 14f), CornerRadius(7f, 7f))

                    // Segments rigides
                    val lThigh = h * 0.24f
                    val lShin = h * 0.23f
                    val lTorso = h * 0.28f

                    val footOnBox = Offset(boxLeft + boxW * 0.35f, boxTop)
                    val footOnGround = Offset(w * 0.32f, groundY)

                    // L'athlète se hisse sur le cube
                    val stepProgress = progress
                    val hipX = (w * 0.38f) + (stepProgress * (boxLeft + boxW * 0.35f - w * 0.38f))
                    val hipY = (groundY - (lThigh + lShin) * 0.85f) - (stepProgress * (boxH + 15f))
                    val hip = Offset(hipX, hipY)

                    // Jambe d'appui
                    val supportKnee = solve2BoneIK(hip, footOnBox, lThigh, lShin, bendPositive = false)

                    // Jambe libre montant le genou
                    val freeKneeTarget = Offset(hip.x - lThigh * 0.7f, hip.y + lThigh * 0.1f - (stepProgress * lThigh * 0.4f))
                    val freeFootTarget = Offset(freeKneeTarget.x - 10f, freeKneeTarget.y + lShin * 0.9f)

                    val shoulder = Offset(hip.x - lTorso * 0.12f, hip.y - lTorso * 0.98f)
                    val head = Offset(shoulder.x - 4f, shoulder.y - 18f)

                    // Dessin jambe d'appui
                    drawLine(athleteSkin, hip, supportKnee, 18f, StrokeCap.Round)
                    drawLine(athleteSkin, supportKnee, footOnBox, 15f, StrokeCap.Round)
                    drawLine(Color(0xFF1E293B), Offset(footOnBox.x - 14f, footOnBox.y - 5f), Offset(footOnBox.x + 16f, footOnBox.y - 5f), 11f, StrokeCap.Round)
                    drawLine(Color.White, Offset(footOnBox.x - 14f, footOnBox.y - 2f), Offset(footOnBox.x + 16f, footOnBox.y - 2f), 3.5f, StrokeCap.Round)

                    // Dessin jambe libre (genou haut de coureur)
                    drawLine(DuoBlueDark, hip, freeKneeTarget, 16f, StrokeCap.Round)
                    drawLine(athleteSkin, freeKneeTarget, freeFootTarget, 13f, StrokeCap.Round)

                    // Torse
                    drawLine(athleteClothes, shoulder, hip, 18f, StrokeCap.Round)

                    // Tête
                    drawCircle(athleteSkin, 17f, head)
                    drawRoundRect(DuoGreen, Offset(head.x - 14f, head.y - 11f), Size(28f, 6.5f), CornerRadius(3f, 3f))

                    // Bras en action de course
                    drawLine(athleteSkin, shoulder, Offset(shoulder.x + 28f, shoulder.y + 24f), 10f, StrokeCap.Round)
                    drawLine(athleteSkin, shoulder, Offset(shoulder.x - 28f, shoulder.y + 24f), 10f, StrokeCap.Round)
                }

                AnimationType.AB_BENCH_CRUNCH -> {
                    val groundY = h * 0.88f
                    drawLine(Color(0xFFDCDFE4), Offset(0f, groundY), Offset(w, groundY), 6f)

                    // 1. BANC INCLINÉ ROUGE (Fidèle à la photo utilisateur)
                    val benchBottomX = w * 0.22f
                    val benchBottomY = groundY - 15f
                    val benchTopX = w * 0.78f
                    val benchTopY = h * 0.45f

                    // Pieds métalliques noirs
                    drawLine(metalColor, Offset(benchBottomX, benchBottomY), Offset(benchBottomX, groundY), 12f, StrokeCap.Round)
                    drawLine(metalColor, Offset(benchTopX, benchTopY), Offset(benchTopX, groundY), 14f, StrokeCap.Round)
                    drawLine(metalColor, Offset(benchTopX - 30f, groundY), Offset(benchTopX + 30f, groundY), 14f, StrokeCap.Round)

                    // Planche rembourrée rouge vif
                    drawLine(DuoRed, Offset(benchBottomX, benchBottomY), Offset(benchTopX, benchTopY), 24f, StrokeCap.Round)
                    drawLine(Color(0xFFB82020), Offset(benchBottomX, benchBottomY + 4f), Offset(benchTopX, benchTopY + 4f), 6f, StrokeCap.Round)

                    // Manchons en mousse noire pour bloquer les pieds (en haut)
                    drawCircle(Color(0xFF1E2124), 16f, Offset(benchTopX - 10f, benchTopY - 20f))
                    drawCircle(Color(0xFF33383E), 10f, Offset(benchTopX - 10f, benchTopY - 20f))

                    // 2. ATHLÈTE EN CRUNCH
                    val crunchProgress = progress

                    // Pieds verrouillés sous le boudin supérieur
                    val feetX = benchTopX - 10f
                    val feetY = benchTopY - 20f

                    // Genoux fléchis
                    val kneeX = benchTopX - 55f
                    val kneeY = benchTopY - 50f

                    // Bassin calé sur la partie médiane du banc
                    val hipX = w * 0.48f
                    val hipY = (benchBottomY + benchTopY) / 2f + 10f

                    // Épaules et tête qui s'enroulent vers les genoux
                    val shoulderX = (hipX - 55f) + (crunchProgress * 35f)
                    val shoulderY = (hipY - 25f) - (crunchProgress * 40f)

                    val headX = shoulderX - 12f
                    val headY = shoulderY - 20f

                    // Jambes
                    drawLine(athleteSkin, Offset(hipX, hipY), Offset(kneeX, kneeY), 14f, StrokeCap.Round)
                    drawLine(Color(0xFF1E293B), Offset(kneeX, kneeY), Offset(feetX, feetY), 12f, StrokeCap.Round)

                    // Torse enroulé
                    drawLine(athleteClothes, Offset(hipX, hipY), Offset(shoulderX, shoulderY), 16f, StrokeCap.Round)

                    // Bras croisés ou aux tempes (sans tirer sur la nuque)
                    val elbowX = shoulderX + 18f
                    val elbowY = shoulderY - 14f
                    drawLine(athleteSkin, Offset(shoulderX, shoulderY), Offset(elbowX, elbowY), 10f, StrokeCap.Round)
                    drawLine(athleteSkin, Offset(elbowX, elbowY), Offset(headX + 10f, headY), 10f, StrokeCap.Round)

                    // Tête avec bandeau
                    drawCircle(athleteSkin, 18f, Offset(headX, headY))
                    drawRoundRect(DuoGreen, Offset(headX - 14f, headY - 14f), Size(28f, 7f), CornerRadius(3f, 3f))

                    // Indicateur d'abdominaux gainés
                    if (crunchProgress > 0.6f) {
                        drawCircle(DuoGreen, 8f, Offset((hipX + shoulderX) / 2f, (hipY + shoulderY) / 2f))
                    }
                }

                AnimationType.BACK_BENCH_HYPEREXTENSION -> {
                    val groundY = h * 0.88f
                    drawLine(Color(0xFFDCDFE4), Offset(0f, groundY), Offset(w, groundY), 6f)

                    // 1. BANC À DORSAUX / LOMBAIRES 45° (Fidèle à la photo utilisateur)
                    val baseBackX = w * 0.75f
                    val baseFrontX = w * 0.35f
                    val pivotX = w * 0.48f
                    val pivotY = h * 0.52f

                    // Structure tubulaire noire à 45 degrés
                    drawLine(metalColor, Offset(baseBackX - 30f, groundY), Offset(baseBackX + 30f, groundY), 14f, StrokeCap.Round)
                    drawLine(metalColor, Offset(baseFrontX - 30f, groundY), Offset(baseFrontX + 30f, groundY), 14f, StrokeCap.Round)
                    drawLine(metalColor, Offset(baseBackX, groundY), Offset(pivotX, pivotY), 16f, StrokeCap.Round)
                    drawLine(metalColor, Offset(baseFrontX, groundY), Offset(pivotX, pivotY), 14f, StrokeCap.Round)

                    // Rouleaux de calage des chevilles à l'arrière
                    drawCircle(Color(0xFF1E2124), 16f, Offset(baseBackX - 15f, groundY - 28f))
                    drawCircle(Color(0xFF33383E), 10f, Offset(baseBackX - 15f, groundY - 28f))

                    // Coussin de hanche rouge/orange incliné à 45°
                    drawRoundRect(DuoOrange, Offset(pivotX - 25f, pivotY - 15f), Size(65f, 26f), CornerRadius(8f, 8f))

                    // 2. ATHLÈTE EN HYPEREXTENSION
                    // progress = 0.0 (buste fléchi vers le bas à 90°) -> progress = 1.0 (alignement droit à 180°)
                    val hyperProgress = progress

                    val ankleX = baseBackX - 15f
                    val ankleY = groundY - 28f

                    // Jambes tendues ancrées sous le boudin
                    drawLine(Color(0xFF1E293B), Offset(ankleX, ankleY), Offset(pivotX, pivotY), 16f, StrokeCap.Round)

                    // Interpolation du buste
                    // En bas (0.0) : buste fléchi vers le sol à angle droit
                    // En haut (1.0) : buste rigoureusement aligné avec les jambes (180°)
                    val lowShoulderX = pivotX - 25f
                    val lowShoulderY = pivotY + 60f

                    val highShoulderX = pivotX - 65f
                    val highShoulderY = pivotY - 60f

                    val shoulderX = lowShoulderX + (hyperProgress * (highShoulderX - lowShoulderX))
                    val shoulderY = lowShoulderY + (hyperProgress * (highShoulderY - lowShoulderY))

                    val headX = shoulderX - 18f
                    val headY = shoulderY - 15f

                    // Torse
                    drawLine(athleteClothes, Offset(pivotX, pivotY), Offset(shoulderX, shoulderY), 16f, StrokeCap.Round)

                    // Bras croisés sur le torse
                    val elbowX = (pivotX + shoulderX) / 2f - 12f
                    val elbowY = (pivotY + shoulderY) / 2f + 5f
                    drawLine(athleteSkin, Offset(shoulderX, shoulderY), Offset(elbowX, elbowY), 10f, StrokeCap.Round)
                    drawLine(athleteSkin, Offset(elbowX, elbowY), Offset(pivotX, pivotY), 10f, StrokeCap.Round)

                    // Tête alignée avec la colonne vertébrale
                    drawCircle(athleteSkin, 18f, Offset(headX, headY))
                    drawRoundRect(DuoOrange, Offset(headX - 14f, headY - 12f), Size(28f, 7f), CornerRadius(3f, 3f))

                    // Guide d'alignement vert à l'extension complète
                    if (hyperProgress > 0.8f) {
                        // Ligne droite parfaite des talons aux épaules
                        drawLine(DuoGreen, Offset(ankleX, ankleY), Offset(headX, headY), 3f, StrokeCap.Round)
                    }
                }

                AnimationType.BODYWEIGHT_PUSHUP,
                AnimationType.BODYWEIGHT_PLANK,
                AnimationType.BODYWEIGHT_MOUNTAIN_CLIMBER,
                AnimationType.BODYWEIGHT_SWIMMER,
                AnimationType.BODYWEIGHT_SUPERMAN,
                AnimationType.BODYWEIGHT_GLUTE_BRIDGE,
                AnimationType.BODYWEIGHT_BICYCLE_CRUNCH,
                AnimationType.BODYWEIGHT_RUSSIAN_TWIST -> {
                    drawLine(Color(0xFFE0E0E0), Offset(0f, h * 0.82f), Offset(w, h * 0.82f), 6f)
                    val bodyY = (h * 0.74f) + (progress * (h * 0.08f))

                    drawCircle(athleteSkin, 24f, Offset(w * 0.32f, bodyY - 18f))
                    drawLine(athleteClothes, Offset(w * 0.35f, bodyY), Offset(w * 0.75f, h * 0.80f), strokeThickness, StrokeCap.Round)
                    drawLine(athleteSkin, Offset(w * 0.38f, bodyY), Offset(w * 0.38f, h * 0.82f), 10f, StrokeCap.Round)
                }

                AnimationType.BODYWEIGHT_BURPEE,
                AnimationType.BODYWEIGHT_SQUAT,
                AnimationType.BODYWEIGHT_LUNGES,
                AnimationType.BODYWEIGHT_CALF_RAISE,
                AnimationType.BODYWEIGHT_SQUAT_JACKS,
                AnimationType.BODYWEIGHT_TRICEP_DIP,
                AnimationType.GENERAL_FITNESS -> {
                    val bounceY = (h * 0.55f) - (progress * 30f)
                    drawCircle(athleteSkin, 30f, Offset(w * 0.5f, bounceY - 70f))
                    drawLine(athleteClothes, Offset(w * 0.5f, bounceY - 40f), Offset(w * 0.5f, bounceY + 40f), 14f, StrokeCap.Round)
                    drawLine(DuoTextDark, Offset(w * 0.5f, bounceY + 40f), Offset(w * 0.42f, h * 0.85f), 12f, StrokeCap.Round)
                    drawLine(DuoTextDark, Offset(w * 0.5f, bounceY + 40f), Offset(w * 0.58f, h * 0.85f), 12f, StrokeCap.Round)
                }
            }
        }
    }
}
