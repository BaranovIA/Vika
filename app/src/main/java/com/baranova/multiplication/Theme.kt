package com.baranova.multiplication

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TrainerColors = lightColorScheme(
    primary = Color(0xFF6D4AFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E2FF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFFFF6F91),
    secondaryContainer = Color(0xFFFFE3E8),
    background = Color(0xFFFAF7FF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF5E5B66),
    error = Color(0xFFE53950)
)

@Composable
fun TrainerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TrainerColors, content = content)
}
