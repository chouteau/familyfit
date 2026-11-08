package com.example.triduofit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.triduofit.theme.DuoGray

@Composable
fun DuoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    borderColor: Color = DuoGray,
    shadowColor: Color = Color(0xFFD0D0D0),
    cornerRadius: Dp = 20.dp,
    shadowHeight: Dp = 4.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
    ) {
        // Ombre basse 3D
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = shadowHeight)
                .clip(RoundedCornerShape(cornerRadius))
                .background(shadowColor)
        )

        // Conteneur principal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(cornerRadius))
                .background(backgroundColor)
                .border(2.dp, borderColor, RoundedCornerShape(cornerRadius))
                .padding(16.dp),
            content = content
        )
    }
}
