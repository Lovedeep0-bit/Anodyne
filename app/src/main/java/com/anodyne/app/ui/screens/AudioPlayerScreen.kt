package com.anodyne.app.ui.screens

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anodyne.app.ui.components.ModernLoadingIndicator
import androidx.compose.ui.res.painterResource
import com.anodyne.app.R
import com.anodyne.app.viewmodel.AudioListViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anodyne.app.util.DurationFormatter
import com.anodyne.app.util.AnodyneMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin

import androidx.compose.foundation.ExperimentalFoundationApi
import com.anodyne.app.ui.components.SquigglySlider
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.shape.CircleShape

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudioPlayerScreen(
    title: String = "", 
    uri: String = "",
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val listVm: AudioListViewModel = viewModel()
    val allAudios by listVm.items().collectAsStateWithLifecycle(initialValue = emptyList())
    val controller = com.anodyne.app.player.PlayerConnection.controller.collectAsState(initial = null).value
    var isPlaying by remember { mutableStateOf(false) }
    var currentDurationMs by remember { mutableStateOf(0L) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var coverBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    
    val isModernUI = ModernUIState.isModernUiEnabled

    // Use current player state if no specific title/uri provided
    var currentTitle by remember { mutableStateOf(title) }
    var currentUri by remember { mutableStateOf(uri) }

    // React to controller updates for correct icons/state
    DisposableEffect(controller) {
        if (controller != null) {
            val listener = object : androidx.media3.common.Player.Listener {
                override fun onIsPlayingChanged(isPlayingNow: Boolean) { isPlaying = isPlayingNow }
                override fun onEvents(player: androidx.media3.common.Player, events: androidx.media3.common.Player.Events) {
                    currentTitle = if (title.isNotBlank()) title else player.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty()
                    currentUri = if (uri.isNotBlank()) uri else player.currentMediaItem?.localConfiguration?.uri?.toString().orEmpty()
                    isPlaying = player.isPlaying
                    val d = player.duration
                    if (d > 0) currentDurationMs = d
                }
                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                    val d = controller.duration
                    if (d > 0) currentDurationMs = d else currentDurationMs = 0L
                    currentPositionMs = 0L
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    val d = controller.duration
                    if (d > 0) currentDurationMs = d
                }
            }
            controller.addListener(listener)
            // initialize
            currentTitle = if (title.isNotBlank()) title else controller.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty()
            currentUri = if (uri.isNotBlank()) uri else controller.currentMediaItem?.localConfiguration?.uri?.toString().orEmpty()
            isPlaying = controller.isPlaying
            val d = controller.duration
            if (d > 0) currentDurationMs = d else currentDurationMs = 0L
            onDispose { controller.removeListener(listener) }
        } else { onDispose { } }
    }

    // Only play if we have a specific URI and it's different from current
    LaunchedEffect(uri) { 
        if (uri.isNotBlank() && controller != null) {
            val item = androidx.media3.common.MediaItem.Builder()
                .setUri(uri)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(title)
                        .build()
                ).build()
            controller.setMediaItem(item)
            controller.prepare()
            controller.play()
            currentPositionMs = 0L
            currentDurationMs = 0L
        }
    }

    // Try to extract embedded cover art (best-effort)
    LaunchedEffect(currentUri) {
        if (currentUri.isNotBlank()) {
            runCatching {
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(context, Uri.parse(currentUri))
                val art = mmr.embeddedPicture
                if (art != null) coverBitmap = BitmapFactory.decodeByteArray(art, 0, art.size).asImageBitmap()
                mmr.release()
            }
        }
    }

    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    // If dragged down significantly (positive y), trigger onBack
                    // We can use a threshold, e.g., 20 pixels
                    if (dragAmount > 20) {
                        onBack()
                    }
                }
            }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(80.dp))
        com.anodyne.app.ui.components.GlassContainer(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f),
            cornerRadius = 32.dp,
            borderWidth = 1.dp
        ) {
            val art = coverBitmap
            if (art != null) {
                Image(
                    bitmap = art,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(72.dp)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))

        // Song name with crossfade on track change
        AnimatedContent(
            targetState = currentTitle.ifBlank { "Now Playing" },
            transitionSpec = {
                fadeIn(animationSpec = AnodyneMotion.defaultTween()) togetherWith fadeOut(animationSpec = AnodyneMotion.defaultTween())
            },
            label = "song_title_anim"
        ) { titleText ->
            Text(
                text = titleText,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(),
                color = colorScheme.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Progress bar
        var sliderValue by remember { mutableStateOf(0f) }
        var isScrubbing by remember { mutableStateOf(false) }
        
        // Periodically tick position for smooth updates
        LaunchedEffect(controller, isPlaying) {
            while (controller != null && this.isActive) {
                currentPositionMs = controller.currentPosition
                val d = controller.duration
                if (d > 0) currentDurationMs = d
                delay(250)
            }
        }
        val duration = currentDurationMs
        val position = currentPositionMs
        val frac = if (duration > 0) (position.coerceAtLeast(0L).coerceAtMost(duration)).toFloat() / duration.toFloat() else 0f
        // Keep slider synced to playback when not actively scrubbing
        LaunchedEffect(frac, isScrubbing) {
            if (!isScrubbing) {
                sliderValue = frac
            }
        }
        Column(Modifier.fillMaxWidth()) {
            if (isModernUI) {
                SquigglySlider(
                    value = if (isScrubbing) sliderValue else frac,
                    onValueChange = { v ->
                        isScrubbing = true
                        sliderValue = v
                    },
                    onValueChangeFinished = {
                        if (controller != null) {
                            val target = (sliderValue * (duration.coerceAtLeast(0L)).toFloat()).toLong()
                            controller.seekTo(target)
                        }
                        isScrubbing = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = colorScheme.primaryContainer,
                        activeTrackColor = colorScheme.primaryContainer,
                        inactiveTrackColor = colorScheme.outlineVariant
                    ),
                    enabled = duration > 0L,
                    isPlaying = isPlaying,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Slider(
                    value = if (isScrubbing) sliderValue else frac,
                    onValueChange = { v ->
                        isScrubbing = true
                        sliderValue = v
                    },
                    onValueChangeFinished = {
                        if (controller != null) {
                            val target = (sliderValue * (duration.coerceAtLeast(0L)).toFloat()).toLong()
                            controller.seekTo(target)
                        }
                        isScrubbing = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = colorScheme.onSurface,
                        activeTrackColor = colorScheme.onSurface,
                        inactiveTrackColor = colorScheme.outlineVariant
                    ),
                    enabled = duration > 0L,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    DurationFormatter.format(position.coerceAtLeast(0L)),
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Text(
                    DurationFormatter.format(duration.coerceAtLeast(0L)),
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Main controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button
                val prevInteractionSource = remember { MutableInteractionSource() }
                val isPrevPressed by prevInteractionSource.collectIsPressedAsState()
                val prevScale by animateFloatAsState(
                    targetValue = if (isPrevPressed) 0.88f else 1f,
                    animationSpec = AnodyneMotion.fastSpring(),
                    label = "prevScale"
                )
                FilledTonalIconButton(
                    onClick = { controller?.seekToPrevious() },
                    enabled = controller?.hasPreviousMediaItem() == true,
                    modifier = Modifier
                        .size(88.dp)
                        .graphicsLayer(
                            scaleX = prevScale,
                            scaleY = prevScale,
                            transformOrigin = TransformOrigin.Center
                        ),
                    shape = RoundedCornerShape(20.dp),
                    interactionSource = prevInteractionSource
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(44.dp),
                        tint = colorScheme.onSurface
                    )
                }
                
                // Play/Pause button
                val playInteractionSource = remember { MutableInteractionSource() }
                val isPlayPressed by playInteractionSource.collectIsPressedAsState()
                val playScale by animateFloatAsState(
                    targetValue = if (isPlayPressed) 0.88f else 1f,
                    animationSpec = AnodyneMotion.fastSpring(),
                    label = "playScale"
                )
                FilledIconButton(
                    onClick = {
                        if (controller != null) {
                            if (controller.isPlaying) controller.pause() else controller.play()
                        }
                    },
                    modifier = Modifier
                        .size(88.dp)
                        .graphicsLayer(
                            scaleX = playScale,
                            scaleY = playScale,
                            transformOrigin = TransformOrigin.Center
                        ),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = colorScheme.primary),
                    interactionSource = playInteractionSource
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = colorScheme.onPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                // Next button
                val nextInteractionSource = remember { MutableInteractionSource() }
                val isNextPressed by nextInteractionSource.collectIsPressedAsState()
                val nextScale by animateFloatAsState(
                    targetValue = if (isNextPressed) 0.88f else 1f,
                    animationSpec = AnodyneMotion.fastSpring(),
                    label = "nextScale"
                )
                FilledTonalIconButton(
                    onClick = { controller?.seekToNext() },
                    enabled = controller?.hasNextMediaItem() == true,
                    modifier = Modifier
                        .size(88.dp)
                        .graphicsLayer(
                            scaleX = nextScale,
                            scaleY = nextScale,
                            transformOrigin = TransformOrigin.Center
                        ),
                    shape = RoundedCornerShape(20.dp),
                    interactionSource = nextInteractionSource
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(44.dp),
                        tint = colorScheme.onSurface
                    )
                }
            }
        }

        error?.let { 
            Text(
                it,
                color = colorScheme.error,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
