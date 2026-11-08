package com.example.triduofit.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.triduofit.theme.DuoBlue
import com.example.triduofit.theme.DuoBlueDark
import com.example.triduofit.theme.DuoOrange
import com.example.triduofit.theme.DuoOrangeDark
import com.example.triduofit.theme.DuoRed
import com.example.triduofit.theme.DuoYellow

enum class MascotMood {
    IDLE,
    WORKING_OUT,
    CHEERING,
    CELEBRATING
}

/**
 * Mascotte officielle TriDuofit : La Mouette Triathlète (Andernos Triathlon).
 * Personnage cartoon énergique avec lunettes de natation sur le front,
 * bec orange courbé caractéristique, trifonction orange et plumage blanc & bleu marine.
 */
@Composable
fun MascotView(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    mood: MascotMood = MascotMood.IDLE,
    showBadge: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_anim")

    // Rebondissement rythmique de la mascotte
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = when (mood) {
            MascotMood.CHEERING, MascotMood.CELEBRATING -> -12f
            MascotMood.WORKING_OUT -> -6f
            MascotMood.IDLE -> -4f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (mood) {
                    MascotMood.CHEERING -> 350
                    MascotMood.CELEBRATING -> 420
                    MascotMood.WORKING_OUT -> 500
                    MascotMood.IDLE -> 750
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    // Battement / mouvement de l'aile lors des célébrations
    val wingAngle by infiniteTransition.animateFloat(
        initialValue = when (mood) {
            MascotMood.CHEERING, MascotMood.CELEBRATING -> -14f
            MascotMood.WORKING_OUT -> -5f
            MascotMood.IDLE -> 0f
        },
        targetValue = when (mood) {
            MascotMood.CHEERING, MascotMood.CELEBRATING -> 16f
            MascotMood.WORKING_OUT -> 6f
            MascotMood.IDLE -> 3f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (mood == MascotMood.CHEERING || mood == MascotMood.CELEBRATING) 320 else 750,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wing_flap"
    )

    // Animation de goutte de sueur (effort / working out)
    val sweatOffset by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweat"
    )

    // Étoiles de célébration
    val starScale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_scale"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .offset(y = bounceOffset.dp)
        ) {
            val w = this.size.width
            val h = this.size.height

            // -------------------------------------------------------------
            // 0. BADGE CIRCULAIRE ARRIÈRE-PLAN (Style Andernos / Arcachon)
            // -------------------------------------------------------------
            if (showBadge) {
                // Disque turquoise / cyan océan
                val badgeCenter = Offset(w * 0.50f, h * 0.50f)
                val badgeRadius = w * 0.45f
                drawCircle(
                    color = Color(0xFF22C1DC),
                    radius = badgeRadius,
                    center = badgeCenter
                )
                // Anneau externe orange chaud
                drawCircle(
                    color = DuoOrange,
                    radius = badgeRadius,
                    center = badgeCenter,
                    style = Stroke(width = w * 0.045f)
                )
                // Liseré bleu marine discret intérieur
                drawCircle(
                    color = Color(0xFF0F2B48).copy(alpha = 0.25f),
                    radius = badgeRadius - (w * 0.022f),
                    center = badgeCenter,
                    style = Stroke(width = w * 0.015f)
                )
            }

            // -------------------------------------------------------------
            // 1. PATTES FINES DE LA MOUETTE (Bas du corps)
            // -------------------------------------------------------------
            val legColor = Color(0xFF0F1E36)
            val footColor = Color(0xFFFF851B)

            // Patte gauche
            drawLine(
                color = legColor,
                start = Offset(w * 0.47f, h * 0.76f),
                end = Offset(w * 0.47f, h * 0.88f),
                strokeWidth = w * 0.025f,
                cap = StrokeCap.Round
            )
            // Pied palmé gauche
            val leftFoot = Path().apply {
                moveTo(w * 0.47f, h * 0.88f)
                lineTo(w * 0.41f, h * 0.91f)
                lineTo(w * 0.47f, h * 0.91f)
                lineTo(w * 0.52f, h * 0.91f)
                close()
            }
            drawPath(leftFoot, color = footColor)

            // Patte droite
            drawLine(
                color = legColor,
                start = Offset(w * 0.56f, h * 0.76f),
                end = Offset(w * 0.56f, h * 0.88f),
                strokeWidth = w * 0.025f,
                cap = StrokeCap.Round
            )
            // Pied palmé droit
            val rightFoot = Path().apply {
                moveTo(w * 0.56f, h * 0.88f)
                lineTo(w * 0.51f, h * 0.91f)
                lineTo(w * 0.56f, h * 0.91f)
                lineTo(w * 0.61f, h * 0.91f)
                close()
            }
            drawPath(rightFoot, color = footColor)

            // -------------------------------------------------------------
            // 2. CORPS ET PLUMAGE BLANC (Silhouette arrondie et athlétique)
            // -------------------------------------------------------------
            // Base blanche du torse et du cou
            val bodyPath = Path().apply {
                moveTo(w * 0.35f, h * 0.38f)
                cubicTo(w * 0.22f, h * 0.48f, w * 0.22f, h * 0.70f, w * 0.36f, h * 0.78f)
                cubicTo(w * 0.46f, h * 0.83f, w * 0.62f, h * 0.83f, w * 0.72f, h * 0.74f)
                cubicTo(w * 0.78f, h * 0.66f, w * 0.76f, h * 0.48f, w * 0.65f, h * 0.38f)
                close()
            }
            // Ombre 3D subtile sous le plumage
            drawPath(bodyPath, color = Color(0xFFE2E8F0))
            // Plumage blanc principal
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(w * 0.25f, h * 0.36f),
                size = Size(w * 0.50f, h * 0.42f),
                cornerRadius = CornerRadius(w * 0.25f, h * 0.21f)
            )

            // -------------------------------------------------------------
            // 3. TRIFONCTION ATHLÉTIQUE ORANGE (Maillot de triathlon)
            // -------------------------------------------------------------
            val jerseyPath = Path().apply {
                moveTo(w * 0.30f, h * 0.54f)
                // Échancrure du col montrant le cou blanc
                cubicTo(w * 0.36f, h * 0.46f, w * 0.44f, h * 0.46f, w * 0.48f, h * 0.50f)
                cubicTo(w * 0.52f, h * 0.46f, w * 0.60f, h * 0.46f, w * 0.66f, h * 0.54f)
                // Flancs du maillot
                lineTo(w * 0.68f, h * 0.73f)
                cubicTo(w * 0.58f, h * 0.77f, w * 0.38f, h * 0.77f, w * 0.28f, h * 0.73f)
                close()
            }
            drawPath(jerseyPath, color = DuoOrange)

            // Ombre inférieure sur la trifonction pour le relief
            val jerseyShadow = Path().apply {
                moveTo(w * 0.29f, h * 0.70f)
                cubicTo(w * 0.40f, h * 0.75f, w * 0.58f, h * 0.75f, w * 0.67f, h * 0.70f)
                lineTo(w * 0.68f, h * 0.73f)
                cubicTo(w * 0.58f, h * 0.77f, w * 0.38f, h * 0.77f, w * 0.28f, h * 0.73f)
                close()
            }
            drawPath(jerseyShadow, color = DuoOrangeDark)

            // Col blanc athlétique du maillot
            drawCircle(
                color = Color.White,
                radius = w * 0.035f,
                center = Offset(w * 0.48f, h * 0.48f)
            )

            // -------------------------------------------------------------
            // 4. AILE BLEU MARINE DE LA MOUETTE (Avec animation d'humeur)
            // -------------------------------------------------------------
            rotate(
                degrees = wingAngle,
                pivot = Offset(w * 0.62f, h * 0.46f)
            ) {
                // Aile repliée le long du flanc droit (bleu nuit Andernos)
                val wingPath = Path().apply {
                    moveTo(w * 0.58f, h * 0.44f)
                    cubicTo(w * 0.74f, h * 0.44f, w * 0.85f, h * 0.56f, w * 0.85f, h * 0.68f)
                    cubicTo(w * 0.83f, h * 0.76f, w * 0.72f, h * 0.80f, w * 0.64f, h * 0.76f)
                    cubicTo(w * 0.58f, h * 0.72f, w * 0.54f, h * 0.58f, w * 0.58f, h * 0.44f)
                    close()
                }
                drawPath(wingPath, color = Color(0xFF0F1E36))

                // Pointes de plumes noires caractéristiques des ailes de mouette
                val wingFeathers = Path().apply {
                    moveTo(w * 0.74f, h * 0.62f)
                    lineTo(w * 0.85f, h * 0.68f)
                    lineTo(w * 0.78f, h * 0.75f)
                    close()
                }
                drawPath(wingFeathers, color = Color(0xFF080D18))

                // Liseré blanc sur la plume pour le contraste
                drawLine(
                    color = Color.White.copy(alpha = 0.9f),
                    start = Offset(w * 0.62f, h * 0.54f),
                    end = Offset(w * 0.74f, h * 0.64f),
                    strokeWidth = w * 0.02f,
                    cap = StrokeCap.Round
                )
            }

            // -------------------------------------------------------------
            // 5. TÊTE BLANCHE RONDE DE LA MOUETTE
            // -------------------------------------------------------------
            drawCircle(
                color = Color.White,
                radius = w * 0.22f,
                center = Offset(w * 0.50f, h * 0.32f)
            )

            // -------------------------------------------------------------
            // 6. LUNETTES DE NATATION BLEUES SUR LE FRONT (Accessoire Triathlon)
            // -------------------------------------------------------------
            // Sangle élastique bleu nuit faisant le tour de la tête
            drawLine(
                color = Color(0xFF0F1E36),
                start = Offset(w * 0.29f, h * 0.17f),
                end = Offset(w * 0.71f, h * 0.17f),
                strokeWidth = w * 0.038f,
                cap = StrokeCap.Round
            )

            // Verre gauche des lunettes
            rotate(degrees = -10f, pivot = Offset(w * 0.41f, h * 0.16f)) {
                // Monture cyan / bleu vif
                drawRoundRect(
                    color = DuoBlue,
                    topLeft = Offset(w * 0.33f, h * 0.10f),
                    size = Size(w * 0.17f, h * 0.13f),
                    cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
                )
                // Verre teinté bleu profond
                drawRoundRect(
                    color = Color(0xFF0D47A1),
                    topLeft = Offset(w * 0.35f, h * 0.12f),
                    size = Size(w * 0.13f, h * 0.09f),
                    cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
                )
                // Reflet blanc sur le verre
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(w * 0.44f, h * 0.13f),
                    end = Offset(w * 0.38f, h * 0.18f),
                    strokeWidth = w * 0.016f,
                    cap = StrokeCap.Round
                )
            }

            // Verre droit des lunettes
            rotate(degrees = 10f, pivot = Offset(w * 0.57f, h * 0.16f)) {
                // Monture cyan / bleu vif
                drawRoundRect(
                    color = DuoBlue,
                    topLeft = Offset(w * 0.49f, h * 0.10f),
                    size = Size(w * 0.17f, h * 0.13f),
                    cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
                )
                // Verre teinté bleu profond
                drawRoundRect(
                    color = Color(0xFF0D47A1),
                    topLeft = Offset(w * 0.51f, h * 0.12f),
                    size = Size(w * 0.13f, h * 0.09f),
                    cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
                )
                // Reflet blanc sur le verre
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(w * 0.60f, h * 0.13f),
                    end = Offset(w * 0.54f, h * 0.18f),
                    strokeWidth = w * 0.016f,
                    cap = StrokeCap.Round
                )
            }

            // Pont central entre les deux verres
            drawRoundRect(
                color = DuoBlueDark,
                topLeft = Offset(w * 0.46f, h * 0.15f),
                size = Size(w * 0.07f, h * 0.03f),
                cornerRadius = CornerRadius(w * 0.015f, w * 0.015f)
            )

            // -------------------------------------------------------------
            // 7. YEUX CARTOON EXPRESSIFS
            // -------------------------------------------------------------
            val eyeY = h * 0.28f
            val eyeRadius = w * 0.065f

            if (mood == MascotMood.CELEBRATING) {
                // Yeux rieurs en arc de cercle (^ ^)
                val leftHappyEye = Path().apply {
                    moveTo(w * 0.36f, eyeY + (w * 0.02f))
                    cubicTo(w * 0.38f, eyeY - (w * 0.035f), w * 0.44f, eyeY - (w * 0.035f), w * 0.46f, eyeY + (w * 0.02f))
                }
                drawPath(leftHappyEye, color = Color(0xFF0F1E36), style = Stroke(width = w * 0.035f, cap = StrokeCap.Round))

                val rightHappyEye = Path().apply {
                    moveTo(w * 0.54f, eyeY + (w * 0.02f))
                    cubicTo(w * 0.56f, eyeY - (w * 0.035f), w * 0.62f, eyeY - (w * 0.035f), w * 0.64f, eyeY + (w * 0.02f))
                }
                drawPath(rightHappyEye, color = Color(0xFF0F1E36), style = Stroke(width = w * 0.035f, cap = StrokeCap.Round))
            } else {
                // Sourcils expressifs
                drawLine(
                    color = Color(0xFF0F1E36),
                    start = Offset(w * 0.36f, eyeY - eyeRadius * 1.3f),
                    end = Offset(w * 0.45f, eyeY - eyeRadius * 1.1f),
                    strokeWidth = w * 0.018f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFF0F1E36),
                    start = Offset(w * 0.53f, eyeY - eyeRadius * 1.1f),
                    end = Offset(w * 0.62f, eyeY - eyeRadius * 1.3f),
                    strokeWidth = w * 0.018f,
                    cap = StrokeCap.Round
                )

                // Oeil gauche
                drawCircle(
                    color = Color.White,
                    radius = eyeRadius,
                    center = Offset(w * 0.41f, eyeY)
                )
                drawCircle(
                    color = Color(0xFF0F1E36),
                    radius = eyeRadius,
                    center = Offset(w * 0.41f, eyeY),
                    style = Stroke(width = w * 0.012f)
                )
                // Pupille gauche
                drawCircle(
                    color = Color(0xFF0F172A),
                    radius = eyeRadius * 0.55f,
                    center = Offset(w * 0.41f, eyeY)
                )
                // Éclat de lumière blanc
                drawCircle(
                    color = Color.White,
                    radius = eyeRadius * 0.22f,
                    center = Offset(w * 0.43f, eyeY - eyeRadius * 0.22f)
                )

                // Oeil droit
                drawCircle(
                    color = Color.White,
                    radius = eyeRadius,
                    center = Offset(w * 0.57f, eyeY)
                )
                drawCircle(
                    color = Color(0xFF0F1E36),
                    radius = eyeRadius,
                    center = Offset(w * 0.57f, eyeY),
                    style = Stroke(width = w * 0.012f)
                )
                // Pupille droite
                drawCircle(
                    color = Color(0xFF0F172A),
                    radius = eyeRadius * 0.55f,
                    center = Offset(w * 0.56f, eyeY)
                )
                // Éclat de lumière blanc
                drawCircle(
                    color = Color.White,
                    radius = eyeRadius * 0.22f,
                    center = Offset(w * 0.58f, eyeY - eyeRadius * 0.22f)
                )
            }

            // -------------------------------------------------------------
            // 8. LE BEC DE MOUETTE ICONIQUE (Fidèle à la mascotte Andernos Triathlon)
            // -------------------------------------------------------------
            // Grand bec orange courbé et crochu vers l'avant-gauche
            val beakTopY = h * 0.31f
            val beakTipX = w * 0.10f
            val beakTipY = h * 0.39f
            val beakBaseX = w * 0.50f
            val beakBottomY = h * 0.41f

            val fullBeak = Path().apply {
                moveTo(beakBaseX, beakTopY)
                // Courbe supérieure plongeante vers l'avant-gauche
                cubicTo(w * 0.36f, beakTopY - (h * 0.018f), w * 0.20f, beakTopY + (h * 0.015f), beakTipX, beakTipY)
                // Crochet crochu au bout du bec (signature de la mouette)
                cubicTo(w * 0.10f, beakTipY + (h * 0.05f), w * 0.16f, beakTipY + (h * 0.06f), w * 0.22f, beakTipY + (h * 0.05f))
                // Courbe inférieure du bec remontant vers la joue
                cubicTo(w * 0.32f, beakTipY + (h * 0.035f), w * 0.42f, beakBottomY + (h * 0.01f), beakBaseX, beakBottomY)
                close()
            }
            drawPath(fullBeak, color = DuoOrange)

            if (mood == MascotMood.CHEERING || mood == MascotMood.CELEBRATING) {
                // Bec entrouvert de joie avec langue rouge
                val mouthOpen = Path().apply {
                    moveTo(w * 0.24f, beakTipY + (h * 0.02f))
                    cubicTo(w * 0.32f, beakTipY + (h * 0.045f), w * 0.42f, beakTipY + (h * 0.035f), beakBaseX * 0.95f, beakBottomY - (h * 0.01f))
                    cubicTo(w * 0.36f, beakTipY + (h * 0.015f), w * 0.28f, beakTipY + (h * 0.01f), w * 0.24f, beakTipY + (h * 0.02f))
                    close()
                }
                drawPath(mouthOpen, color = Color(0xFF8B0000))

                // Langue rouge joyeuse
                drawCircle(
                    color = DuoRed,
                    radius = w * 0.032f,
                    center = Offset(w * 0.35f, beakTipY + (h * 0.028f))
                )
            }

            // Ligne de commissure / séparation du bec avec grand sourire
            val beakSmile = Path().apply {
                moveTo(beakTipX + (w * 0.04f), beakTipY + (h * 0.025f))
                cubicTo(w * 0.24f, beakTipY + (h * 0.02f), w * 0.38f, beakBottomY - (h * 0.015f), beakBaseX * 0.98f, beakBottomY - (h * 0.015f))
            }
            drawPath(beakSmile, color = Color(0xFFC75100), style = Stroke(width = w * 0.020f, cap = StrokeCap.Round))

            // Contour extérieur noir / bleu nuit net du bec
            drawPath(fullBeak, color = Color(0xFF0F1E36), style = Stroke(width = w * 0.018f, join = StrokeJoin.Round))

            // Narine discrète de mouette
            drawCircle(
                color = Color(0xFF9E3D00),
                radius = w * 0.013f,
                center = Offset(w * 0.36f, beakTopY + (h * 0.016f))
            )

            // -------------------------------------------------------------
            // 9. ACCESSOIRES & EFFETS SELON L'HUMEUR
            // -------------------------------------------------------------
            if (mood == MascotMood.WORKING_OUT) {
                // Goutte de sueur d'effort qui perle près de la tempe
                val dropY = (h * 0.22f) + sweatOffset
                val sweatDrop = Path().apply {
                    moveTo(w * 0.72f, dropY - (h * 0.025f))
                    cubicTo(w * 0.75f, dropY - (h * 0.01f), w * 0.75f, dropY + (h * 0.02f), w * 0.72f, dropY + (h * 0.025f))
                    cubicTo(w * 0.69f, dropY + (h * 0.02f), w * 0.69f, dropY - (h * 0.01f), w * 0.72f, dropY - (h * 0.025f))
                    close()
                }
                drawPath(sweatDrop, color = DuoBlue)
                drawPath(sweatDrop, color = Color.White.copy(alpha = 0.6f), style = Stroke(width = w * 0.008f))
            } else if (mood == MascotMood.CELEBRATING) {
                // Étoiles dorées de victoire autour de la mouette
                val starCenter1 = Offset(w * 0.18f, h * 0.18f)
                val starCenter2 = Offset(w * 0.82f, h * 0.22f)

                drawCircle(color = DuoYellow, radius = (w * 0.045f) * starScale, center = starCenter1)
                drawCircle(color = Color.White, radius = (w * 0.018f) * starScale, center = starCenter1)

                drawCircle(color = DuoYellow, radius = (w * 0.038f) * starScale, center = starCenter2)
                drawCircle(color = Color.White, radius = (w * 0.015f) * starScale, center = starCenter2)
            }
        }
    }
}
