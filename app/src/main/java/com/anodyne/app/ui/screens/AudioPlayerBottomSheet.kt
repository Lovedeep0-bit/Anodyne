package com.anodyne.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.anodyne.app.player.PlayerConnection

import com.anodyne.app.ui.components.SquigglySlider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.isActive

@Composable
fun AudioPlayerControls() {
    val context = LocalContext.current
    val isModernUI = ModernUIState.isModernUiEnabled
    val controller = PlayerConnection.controller.collectAsState(initial = null).value
    
    if (controller == null) {
        Text("Idle", color = Color.White)
        return
    }
    
    var sliderValue by remember { androidx.compose.runtime.mutableStateOf(0f) }
    var currentDurationMs by remember { androidx.compose.runtime.mutableStateOf(0L) }
    var currentPositionMs by remember { androidx.compose.runtime.mutableStateOf(0L) }
    var isPlaying by remember { androidx.compose.runtime.mutableStateOf(false) }

    // Use DisposableEffect to track player position and duration dynamically
    androidx.compose.runtime.DisposableEffect(controller) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) { isPlaying = isPlayingNow }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                isPlaying = controller.isPlaying
            }
        }
        controller.addListener(listener)
        isPlaying = controller.isPlaying
        onDispose { controller.removeListener(listener) }
    }
    
    androidx.compose.runtime.LaunchedEffect(controller, isPlaying) {
        while (controller != null && isActive) {
            currentPositionMs = controller.currentPosition
            val d = controller.duration
            if (d > 0) currentDurationMs = d
            sliderValue = if (currentDurationMs > 0) currentPositionMs.toFloat() / currentDurationMs.toFloat() else 0f
            kotlinx.coroutines.delay(250)
        }
    }

    Column(Modifier.fillMaxWidth().padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isModernUI) {
                FilledTonalIconButton(
                    onClick = { controller.seekToPreviousMediaItem() },
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = null)
                }
                FilledIconButton(
                    onClick = {
                        if (controller.isPlaying) controller.pause() else controller.play()
                    },
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                }
                FilledTonalIconButton(
                    onClick = { controller.seekToNextMediaItem() },
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = null)
                }
            } else {
                IconButton(onClick = { controller.seekToPreviousMediaItem() }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = Color.White)
                }
                IconButton(onClick = {
                    if (controller.isPlaying) controller.pause() else controller.play()
                }) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                IconButton(onClick = { controller.seekToNextMediaItem() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White)
                }
            }
        }
        
        if (isModernUI) {
            SquigglySlider(
                value = sliderValue,
                onValueChange = { frac ->
                    sliderValue = frac
                    if (currentDurationMs > 0) {
                        controller.seekTo((frac * currentDurationMs).toLong())
                    }
                },
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Slider(
                value = sliderValue,
                onValueChange = { frac ->
                    sliderValue = frac
                    if (currentDurationMs > 0) {
                        controller.seekTo((frac * currentDurationMs).toLong())
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


