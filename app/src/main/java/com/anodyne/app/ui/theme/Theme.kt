package com.anodyne.app.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anodyne.app.ui.screens.AppTheme
import com.anodyne.app.ui.screens.ThemeState

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun AnodyneTheme(
    appTheme: AppTheme = ThemeState.currentTheme,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val primaryColor = ThemeState.currentPrimaryColor
    val baseColorScheme = expressiveBasedDarkColorScheme()

    val appBackground = when (appTheme) {
        AppTheme.Light -> Color.White
        AppTheme.Dark -> Color(0xFF121212) // dark greyish
        AppTheme.OLED -> Color.Black
    }
    
    val appOnBackground = when (appTheme) {
        AppTheme.Light -> Color.Black
        AppTheme.Dark -> Color.White
        AppTheme.OLED -> Color.White
    }

    val selectedPrimary = primaryColor.darkColor

    val tintedSurface = lerp(Color(0xFF121212), selectedPrimary, 0.12f)
    val tintedSurfaceVariant = lerp(Color(0xFF121212), selectedPrimary, 0.2f)

    val colorScheme = baseColorScheme.copy(
        primary = selectedPrimary,
        primaryContainer = selectedPrimary.copy(alpha = 0.3f),
        onPrimaryContainer = Color.White,
        secondaryContainer = selectedPrimary.copy(alpha = 0.15f),
        onSecondaryContainer = Color.White,
        surface = tintedSurface,
        surfaceVariant = tintedSurfaceVariant,
        background = appBackground,
        onBackground = appOnBackground
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ExpressiveShapes,
        content = content
    )
}
