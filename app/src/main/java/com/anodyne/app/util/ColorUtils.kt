package com.anodyne.app.util

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

@Composable
fun playlistColor(input: String, isModern: Boolean = false, colorScheme: ColorScheme? = null): Color {
    if (isModern && colorScheme != null) return colorScheme.primary
    if (input.isBlank()) return Color(0xFF1DB954)

    val colors = listOf(
        Color(0xFF1DB954), Color(0xFF1ED760), Color(0xFF191414),
        Color(0xFF535353), Color(0xFFB3B3B3), Color(0xFFE91429),
        Color(0xFF056952), Color(0xFF509BF5), Color(0xFFBC5900),
        Color(0xFFE8115B), Color(0xFF8C67AB), Color(0xFFBA5D07),
        Color(0xFF1E3264), Color(0xFF148A08)
    )
    return colors[abs(input.hashCode()) % colors.size]
}
