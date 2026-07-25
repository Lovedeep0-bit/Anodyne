package com.anodyne.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun SquigglySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    isPlaying: Boolean = true,
    colors: SliderColors = SliderDefaults.colors()
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        thumb = {
            Box(
                modifier = Modifier.size(0.dp)
            )
        },
        track = { sliderState ->
            SquigglyTrack(
                value = value,
                colors = colors,
                enabled = enabled,
                isPlaying = isPlaying
            )
        }
    )
}

@Composable
private fun SquigglyTrack(
    value: Float,
    colors: SliderColors,
    enabled: Boolean,
    isPlaying: Boolean
) {
    val activeTrackColor = colors.activeTrackColor
    val inactiveTrackColor = colors.inactiveTrackColor

    val infiniteTransition = rememberInfiniteTransition(label = "squiggly")
    val targetPhase = if (isPlaying) (Math.PI * 2).toFloat() else 0f

    val phaseOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = targetPhase,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val currentPhase = if (isPlaying) phaseOffset else 0f

    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        val thumbRadiusPx = 10.dp.toPx()
        val trackStart = thumbRadiusPx
        val trackEnd = width - thumbRadiusPx
        val trackWidth = trackEnd - trackStart

        val fraction = value.coerceIn(0f, 1f)
        val thumbX = trackStart + (trackWidth * fraction)

        val strokeWidth = 8.dp.toPx()
        val amplitude = 8.dp.toPx()
        val wavelength = 36.dp.toPx()

        val rampPx = wavelength * 0.75f

        if (fraction > 0f) {
            val activeLen = thumbX - trackStart
            val path = Path()

            var x = trackStart
            val step = 2f
            var first = true
            while (x <= thumbX) {
                val relativeX = x - trackStart
                val wavePhase = (relativeX / wavelength) * Math.PI * 2 - currentPhase

                val rampUp = (relativeX / rampPx).coerceIn(0f, 1f)
                val distToEnd = activeLen - relativeX
                val rampDown = (distToEnd / rampPx).coerceIn(0f, 1f)

                val localAmplitude = amplitude * rampUp * rampDown
                val y = centerY + sin(wavePhase).toFloat() * localAmplitude

                if (first) {
                    path.moveTo(x, y)
                    first = false
                } else {
                    path.lineTo(x, y)
                }
                x += step
            }

            drawPath(
                path = path,
                color = activeTrackColor,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        if (thumbX < trackEnd) {
            drawLine(
                color = inactiveTrackColor,
                start = Offset(thumbX, centerY),
                end = Offset(trackEnd, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
