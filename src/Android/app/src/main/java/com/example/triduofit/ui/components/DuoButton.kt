package com.example.triduofit.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.triduofit.theme.DuoGreen
import com.example.triduofit.theme.DuoGreenDark

@Composable
fun DuoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = DuoGreen,
    shadowColor: Color = DuoGreenDark,
    textColor: Color = Color.White,
    height: Dp = 48.dp,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerRadius = 14.dp
    val shadowHeight = 4.dp

    val pressedOffset by animateDpAsState(
        targetValue = if (isPressed && enabled) shadowHeight else 0.dp,
        label = "button_press_offset"
    )

    Box(
        modifier = modifier
            .height(height + shadowHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        // Ombre 3D du bouton (partie basse)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = shadowHeight)
                .clip(RoundedCornerShape(cornerRadius))
                .background(if (enabled) shadowColor else Color(0xFFC0C0C0))
        )

        // Face tactile supérieure
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = pressedOffset)
                .clip(RoundedCornerShape(cornerRadius))
                .background(if (enabled) color else Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                val textSize = when {
                    text.length > 10 -> 10.5.sp
                    text.length > 7 -> 11.5.sp
                    else -> 13.sp
                }
                Text(
                    text = text.uppercase(),
                    color = if (enabled) textColor else Color(0xFF888888),
                    fontSize = textSize,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.3.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
