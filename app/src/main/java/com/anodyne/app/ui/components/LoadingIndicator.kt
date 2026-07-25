package com.anodyne.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun ModernLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    ExpressiveLoadingDots(
        modifier = modifier,
        dotSize = size / 3f,
        color = color
    )
}

@Composable
fun ExpressiveLoadingDots(
    modifier: Modifier = Modifier,
    dotSize: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dotPhases = List(3) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = LinearEasing)
            ),
            label = "dotPhase_$index"
        )
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        dotPhases.forEachIndexed { index, phase ->
            val offset = index * 0.25f
            val normalized = ((phase.value + offset) % 1f)
            val alpha = 0.3f + 0.7f * (sin(normalized * 2 * kotlin.math.PI) * 0.5f + 0.5f).toFloat()

            Box(
                modifier = Modifier
                    .size(dotSize)
                    .alpha(alpha),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(dotSize)) {
                    drawCircle(color = color)
                }
            }

            if (index < 2) {
                Spacer(Modifier.width(dotSize / 2))
            }
        }
    }
}
