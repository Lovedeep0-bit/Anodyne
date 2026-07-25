package com.anodyne.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Anodyne Modern Theme - Neon Purple and Dark Gray
val NeonPurple = Color(0xFFB524FF)
val NeonPurpleLight = Color(0xFFD47CFF)
val NeonPurpleDark = Color(0xFF6B1199)

val DarkGrayBackground = Color(0xFF0F0F13)
val DarkGraySurface = Color(0xFF16161B)
val LightGraySurface = Color(0xFFE5E5E9)

// OLED Theme - Pure black backgrounds
val OLEDBackground = Color(0xFF000000)
val OLEDSurface = Color(0xFF000000)
val OLEDPrimary = NeonPurple
val OLEDSecondary = Color(0xFFB3B3B3)
val OLEDTertiary = Color(0xFF808080)

// Expressive tonal palette extensions for deeper color roles
val ExpressiveSurfaceContainerLow = Color(0xFF121217)
val ExpressiveSurfaceContainerHigh = Color(0xFF1E1E24)
val ExpressiveSurfaceBright = Color(0xFF2A2A30)
val ExpressiveSurfaceDim = Color(0xFF0A0A0E)

fun expressiveBasedDarkColorScheme() = darkColorScheme(
    primary = NeonPurple,
    onPrimary = Color(0xFF1A0029),
    primaryContainer = NeonPurple.copy(alpha = 0.25f),
    onPrimaryContainer = Color(0xFFE8B4FF),
    secondary = NeonPurpleLight,
    onSecondary = Color(0xFF0A0A1A),
    secondaryContainer = NeonPurpleLight.copy(alpha = 0.2f),
    onSecondaryContainer = Color(0xFFC4C4D4),
    tertiary = Pink80,
    onTertiary = Color(0xFF0A0A14),
    tertiaryContainer = Pink80.copy(alpha = 0.2f),
    onTertiaryContainer = Color(0xFFD0D0DC),
    error = Color(0xFFEF9A9A),
    onError = Color(0xFF2A0000),
    errorContainer = Color(0xFF3A0A0A),
    onErrorContainer = Color(0xFFFFB3B3),
    background = DarkGrayBackground,
    onBackground = Color(0xFFE5E5E9),
    surface = DarkGraySurface,
    onSurface = Color(0xFFE5E5E9),
    surfaceVariant = Color(0xFF26262B),
    onSurfaceVariant = Color(0xFFAEAEB2),
    outline = Color(0xFF545458),
    outlineVariant = Color(0xFF3A3A3C),
    inverseSurface = Color(0xFFE5E5E9),
    inverseOnSurface = DarkGrayBackground,
    inversePrimary = NeonPurpleLight,
    surfaceTint = NeonPurple
)

fun expressiveBasedLightColorScheme() = lightColorScheme(
    primary = NeonPurpleDark,
    onPrimary = Color.White,
    primaryContainer = NeonPurpleLight.copy(alpha = 0.3f),
    onPrimaryContainer = Color(0xFF1A0029),
    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = NeonPurple.copy(alpha = 0.15f),
    onSecondaryContainer = Color(0xFF0A0A1A),
    tertiary = Pink40,
    onTertiary = Color.White,
    tertiaryContainer = Pink40.copy(alpha = 0.15f),
    onTertiaryContainer = Color(0xFF0A0A14),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFF2F2F7),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE8E8EF),
    onSurfaceVariant = Color(0xFF555555),
    outline = Color(0xFFCCCCCC),
    outlineVariant = Color(0xFFDDDDDD),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = NeonPurpleDark,
    surfaceTint = NeonPurpleDark
)

fun oledColorScheme() = darkColorScheme(
    primary = OLEDPrimary,
    onPrimary = Color(0xFF1A0029),
    primaryContainer = OLEDPrimary.copy(alpha = 0.25f),
    onPrimaryContainer = Color(0xFFE8B4FF),
    secondary = OLEDSecondary,
    onSecondary = Color(0xFF0A0A1A),
    secondaryContainer = Color(0xFF1A1A1A),
    onSecondaryContainer = Color(0xFFD4D4D4),
    tertiary = OLEDTertiary,
    onTertiary = Color(0xFF0A0A14),
    tertiaryContainer = Color(0xFF141414),
    onTertiaryContainer = Color(0xFFB0B0B0),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF0A0A0A),
    onSurfaceVariant = Color(0xFFB3B3B3),
    outline = Color.White,
    outlineVariant = Color(0xFF3A3A3A),
    error = Color(0xFFEF9A9A),
    onError = Color(0xFF2A0000),
    errorContainer = Color(0xFF3A0A0A),
    onErrorContainer = Color(0xFFFFB3B3),
    inverseSurface = Color(0xFFE5E5E9),
    inverseOnSurface = Color.Black,
    inversePrimary = OLEDPrimary,
    surfaceTint = OLEDPrimary
)
