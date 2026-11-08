package com.example.triduofit.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DuolingoColorScheme = lightColorScheme(
    primary = DuoGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7FFB8),
    onPrimaryContainer = DuoGreenDark,

    secondary = DuoBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDF4FF),
    onSecondaryContainer = DuoBlueDark,

    tertiary = DuoOrange,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEBD6),
    onTertiaryContainer = DuoOrangeDark,

    background = DuoBackgroundSubtle,
    onBackground = DuoTextDark,
    surface = DuoBackground,
    onSurface = DuoTextDark
)

@Composable
fun TriDuofitTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DuolingoColorScheme,
        typography = Typography,
        content = content
    )
}
