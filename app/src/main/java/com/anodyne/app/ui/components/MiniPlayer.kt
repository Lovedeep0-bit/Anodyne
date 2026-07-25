package com.anodyne.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.anodyne.app.player.PlayerConnection
import com.anodyne.app.util.ArtworkProvider
import android.net.Uri
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Immutable
private data class MiniPlayerUiState(
    val title: String = "",
    val effectiveUri: String? = null,
    val isPlaying: Boolean = false,
    val hasPrev: Boolean = false,
    val hasNext: Boolean = false
)

@Composable
fun MiniPlayer(
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp),
    enableFullBorder: Boolean = true
) {
    val controller = PlayerConnection.controller.collectAsState(initial = null).value

    var uiState by remember { mutableStateOf(MiniPlayerUiState()) }

    val onOpenNowPlayingStable by rememberUpdatedState(onOpenNowPlaying)

    val pillShape = remember { RoundedCornerShape(22.dp) }

    DisposableEffect(controller) {
        if (controller != null) {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                    uiState = uiState.copy(isPlaying = isPlayingNow)
                }
                override fun onEvents(player: Player, events: Player.Events) {
                    uiState = uiState.copy(
                        title = player.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty(),
                        effectiveUri = player.currentMediaItem?.localConfiguration?.uri?.toString(),
                        hasPrev = player.hasPreviousMediaItem(),
                        hasNext = player.hasNextMediaItem(),
                        isPlaying = player.isPlaying
                    )
                }
            }
            controller.addListener(listener)
            uiState = uiState.copy(
                title = controller.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty(),
                effectiveUri = controller.currentMediaItem?.localConfiguration?.uri?.toString(),
                hasPrev = controller.hasPreviousMediaItem(),
                hasNext = controller.hasNextMediaItem(),
                isPlaying = controller.isPlaying
            )
            onDispose { controller.removeListener(listener) }
        } else {
            onDispose { }
        }
    }

    // Let AnimatedVisibility handle whether this is shown or not.
    // Early return here breaks the exit animation.

    val context = LocalContext.current
    var artwork by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(uiState.effectiveUri) {
        artwork = null
        val uriString = uiState.effectiveUri
        if (!uriString.isNullOrBlank()) {
            try {
                val uri = Uri.parse(uriString)
                val bmp = withContext(Dispatchers.IO) {
                    ArtworkProvider.loadAudioArtwork(context, uri)
                }
                artwork = bmp
            } catch (_: Exception) { }
        }
    }

    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .background(
                color = colorScheme.surfaceVariant.copy(alpha = 0.95f),
                shape = pillShape
            )
            .border(
                width = 1.dp,
                color = colorScheme.outline.copy(alpha = 0.2f),
                shape = pillShape
            )
            .clip(pillShape)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 20) {
                        // Swipe down: kill the song
                        controller?.stop()
                        controller?.clearMediaItems()
                    } else if (dragAmount < -20) {
                        // Swipe up: open Now Playing
                        onOpenNowPlayingStable()
                    }
                }
            }
            .clickable(onClick = onOpenNowPlayingStable)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (artwork != null) {
                    Image(
                        bitmap = artwork!!.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                    )
                } else {
                    val placeholderColor = remember(uiState.title) { generateColorFromString(uiState.title) }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(placeholderColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (uiState.title.isBlank()) "Now Playing" else uiState.title,
                    color = colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = { controller?.seekToPrevious() },
                    enabled = uiState.hasPrev,
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = if (uiState.hasPrev) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                FilledIconButton(
                    onClick = {
                        if (controller != null) {
                            if (uiState.isPlaying) controller.pause() else controller.play()
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = colorScheme.primary
                    )
                ) {
                    Icon(
                        if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                        tint = colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = { controller?.seekToNext() },
                    enabled = uiState.hasNext,
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = if (uiState.hasNext) colorScheme.onSurface else colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

private fun generateColorFromString(input: String): Color {
    if (input.isBlank()) return Color(0xFF1DB954)
    val colors = listOf(
        Color(0xFF1DB954),
        Color(0xFF1ED760),
        Color(0xFF191414),
        Color(0xFF535353),
        Color(0xFFB3B3B3),
        Color(0xFFE91429),
        Color(0xFF056952),
        Color(0xFF509BF5),
        Color(0xFFBC5900),
        Color(0xFFE8115B)
    )
    val hash = input.hashCode()
    val index = kotlin.math.abs(hash) % colors.size
    return colors[index]
}
