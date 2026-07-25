package com.anodyne.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anodyne.app.util.DurationFormatter
import com.anodyne.app.data.AudioFile
import com.anodyne.app.util.ArtworkProvider
import com.anodyne.app.R
import com.anodyne.app.ui.components.MiniPlayer
import com.anodyne.app.data.AlbumCoverStore
import androidx.compose.ui.graphics.asImageBitmap
import com.anodyne.app.ui.components.ModernLoadingIndicator
import androidx.compose.ui.res.painterResource
import com.anodyne.app.ui.components.PlaylistShapes
import kotlinx.coroutines.flow.debounce


@Composable
fun AudioListScreen(
    title: String,
    itemsList: List<AudioFile>,
    onItemClick: (AudioFile) -> Unit,
    onOpenNowPlaying: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    var isShuffleOn by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchVisible by rememberSaveable { mutableStateOf(false) }
    
    var filterQuery by remember { mutableStateOf(searchQuery) }
    LaunchedEffect(Unit) {
        snapshotFlow { searchQuery }
            .debounce(80)
            .collect { filterQuery = it }
    }
    
    val filteredItems = remember(filterQuery, itemsList) {
        if (filterQuery.isBlank()) {
            itemsList
        } else {
            itemsList.filter { audioFile ->
                audioFile.title.contains(filterQuery, ignoreCase = true) ||
                (audioFile.artist?.contains(filterQuery, ignoreCase = true) == true) ||
                (audioFile.album?.contains(filterQuery, ignoreCase = true) == true)
            }
        }
    }
    
    // Remove: val uiState by collectAsState()
    // Remove: val currentUri = uiState.currentUri
    
     val controller = com.anodyne.app.player.PlayerConnection.controller.collectAsState(initial = null).value
     var currentUri by remember { mutableStateOf(controller?.currentMediaItem?.localConfiguration?.uri?.toString()) }
     var isPlaying by remember { mutableStateOf(controller?.isPlaying ?: false) }
     DisposableEffect(controller) {
         if (controller != null) {
             val listener = object : androidx.media3.common.Player.Listener {
                 override fun onEvents(player: androidx.media3.common.Player, events: androidx.media3.common.Player.Events) {
                     currentUri = player.currentMediaItem?.localConfiguration?.uri?.toString()
                     isPlaying = player.isPlaying
                 }
                 override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                     currentUri = mediaItem?.localConfiguration?.uri?.toString()
                 }
                 override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                     isPlaying = isPlayingNow
                 }
             }
             controller.addListener(listener)
             currentUri = controller.currentMediaItem?.localConfiguration?.uri?.toString()
             isPlaying = controller.isPlaying
             onDispose { controller.removeListener(listener) }
         } else onDispose { }
     }
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
            // Spotify-like header
            PlaylistHeader(
                title = title,
                itemCount = itemsList.size,
                 onShuffleToggle = { 
                     isShuffleOn = !isShuffleOn
                 },
                isShuffleOn = isShuffleOn,
                onPlayAll = {
                    val listToPlay = if (isShuffleOn) itemsList.shuffled() else itemsList
                    if (controller != null && listToPlay.isNotEmpty()) {
                        val mediaItems = listToPlay.map { f ->
                            androidx.media3.common.MediaItem.Builder()
                                .setUri(f.uri)
                                .setMediaId(f.id.toString())
                                .setMediaMetadata(
                                    androidx.media3.common.MediaMetadata.Builder()
                                        .setTitle(f.title)
                                        .setArtist(f.artist ?: "")
                                        .setAlbumTitle(f.album ?: "")
                                        .build()
                                )
                                .build()
                        }
                        controller.setMediaItems(mediaItems, 0, 0L)
                        controller.prepare()
                        controller.play()
                    }
                },
                 searchQuery = searchQuery,
                 onSearchQueryChange = { searchQuery = it },
                 isSearchVisible = isSearchVisible,
                 onSearchVisibilityToggle = { isSearchVisible = !isSearchVisible },
                 isPlaying = isPlaying,
                 controller = controller,
                 onBack = onBack
             )
            
            // Song list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (filteredItems.isEmpty() && searchQuery.isNotBlank()) {
                    item {
                        EmptySearchResult(searchQuery = searchQuery)
                    }
                } else {
                    items(filteredItems, key = { it.id }) { item ->
                        // Remove: val isCurrent = currentUri != null && currentUri == item.uri.toString()
                        SongItem(
                            audioFile = item,
                            onClick = {
                                // Start playback via MediaController but do not navigate to player
                                val controller = com.anodyne.app.player.PlayerConnection.controller.value
                                if (controller != null) {
                                    val mediaItems = filteredItems.map { f ->
                                        androidx.media3.common.MediaItem.Builder()
                                            .setUri(f.uri)
                                            .setMediaId(f.id.toString())
                                            .setMediaMetadata(
                                                androidx.media3.common.MediaMetadata.Builder()
                                                    .setTitle(f.title)
                                                    .setArtist(f.artist ?: "")
                                                    .setAlbumTitle(f.album ?: "")
                                                    .build()
                                            )
                                            .build()
                                    }
                                    val index = filteredItems.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                                    controller.setMediaItems(mediaItems, index, 0L)
                                    controller.prepare()
                                    controller.play()
                                }
                                onItemClick(item)
                            },
                            isCurrent = (currentUri != null && currentUri == item.uri)
                        )
                    }
                }
            }
        }
    }

@Composable
private fun PlaylistHeader(
    title: String,
    itemCount: Int,
    onShuffleToggle: () -> Unit,
    isShuffleOn: Boolean,
    onPlayAll: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearchVisible: Boolean,
    onSearchVisibilityToggle: () -> Unit,
    isPlaying: Boolean = false,
    controller: androidx.media3.common.Player?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val albumCoverStore = remember { AlbumCoverStore(context) }
    val customCoverUri by albumCoverStore
        .getCustomCoverFlow(title)
        .collectAsStateWithLifecycle(initialValue = null)

    val isModernUI = ModernUIState.isModernUiEnabled
    val colorScheme = MaterialTheme.colorScheme
    val playlistShape = rememberShapeState()
    val artworkShape = PlaylistShapes.getShape(playlistShape)

    com.anodyne.app.ui.components.GlassContainer(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        cornerRadius = 24.dp,
        borderWidth = 1.dp,
        containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
        // Expressive search bar
        if (isSearchVisible) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isModernUI) {
                    FilledIconButton(
                        onClick = onSearchVisibilityToggle,
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                } else {
                    IconButton(
                        onClick = onSearchVisibilityToggle,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(Modifier.width(8.dp))
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                                Text(
                                    text = "Search in $title",
                                    color = colorScheme.onSurfaceVariant
                                )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isModernUI) Color.Transparent else colorScheme.primary,
                        unfocusedBorderColor = if (isModernUI) Color.Transparent else colorScheme.outline,
                        focusedTextColor = colorScheme.onSurface,
                        unfocusedTextColor = colorScheme.onSurface,
                        cursorColor = colorScheme.primary,
                        focusedContainerColor = if (isModernUI) colorScheme.surface else colorScheme.surfaceVariant,
                        unfocusedContainerColor = if (isModernUI) colorScheme.surface else colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.weight(1f),
                    shape = if (isModernUI) RoundedCornerShape(28.dp) else OutlinedTextFieldDefaults.shape,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        
        // Playlist info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Playlist artwork
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(artworkShape)
                        .background(colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                if (customCoverUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(customCoverUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(artworkShape)
                    )
                } else {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = colorScheme.onSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$itemCount songs",
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
            
            // Search button
            if (isModernUI) {
                val containerColor = if (isSearchVisible) colorScheme.primaryContainer else colorScheme.surface
                FilledIconButton(
                    onClick = onSearchVisibilityToggle,
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = containerColor)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = if (isSearchVisible) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant)
                }
            } else {
                IconButton(
                    onClick = onSearchVisibilityToggle,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isSearchVisible) colorScheme.onSurface else colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(1.dp, colorScheme.outline, RoundedCornerShape(20.dp))
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isSearchVisible) colorScheme.surfaceVariant else colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            if (isModernUI) {
                FilledIconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = colorScheme.surface)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colorScheme.onSurfaceVariant)
                }
            } else {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(1.dp, colorScheme.outline, RoundedCornerShape(20.dp))
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

             // Play button
            if (isModernUI) {
                FilledIconButton(
                     onClick = {
                         if (isPlaying) controller?.pause() else onPlayAll()
                     },
                     modifier = Modifier.weight(1f).height(52.dp),
                     shape = RoundedCornerShape(26.dp),
                     colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (isPlaying) colorScheme.primary else colorScheme.primaryContainer)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = if (isPlaying) colorScheme.onPrimary else colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isPlaying) "Pause" else "Play",
                            color = if (isPlaying) colorScheme.onPrimary else colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                val playShape = RoundedCornerShape(50)
                Button(
                     onClick = {
                         if (isPlaying) {
                             // Pause the current playback
                             controller?.pause()
                         } else {
                             // Start playing
                             onPlayAll()
                         }
                     },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) colorScheme.onSurface else colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                    ,
                    shape = playShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.outline)
                ) {
                     Icon(
                         if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                         contentDescription = if (isPlaying) "Pause" else "Play",
                         tint = if (isPlaying) colorScheme.surfaceVariant else colorScheme.onSurface,
                         modifier = Modifier.size(20.dp)
                     )
                     Spacer(Modifier.width(8.dp))
                     Text(
                         text = if (isPlaying) "Pause" else "Play",
                         color = if (isPlaying) colorScheme.surfaceVariant else colorScheme.onSurface,
                         fontWeight = FontWeight.Bold
                     )
                 }
             }
            
            // Shuffle button
            if (isModernUI) {
                val containerColor = if (isShuffleOn) colorScheme.primaryContainer else colorScheme.surface
                FilledIconButton(
                    onClick = onShuffleToggle,
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = containerColor)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = if (isShuffleOn) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant)
                }
            } else {
                IconButton(
                    onClick = onShuffleToggle,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isShuffleOn) colorScheme.onSurface else colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(1.dp, colorScheme.outline, RoundedCornerShape(20.dp))
                ) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffleOn) colorScheme.surfaceVariant else colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun EmptySearchResult(searchQuery: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No results found",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Try searching for something else",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun SongItem(
    audioFile: AudioFile,
    onClick: () -> Unit,
    isCurrent: Boolean = false
) {
    val context = LocalContext.current
    val songArtworkShape = RoundedCornerShape(8.dp)
    com.anodyne.app.ui.components.GlassContainer(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        cornerRadius = 16.dp,
        borderWidth = if (isCurrent) 1.dp else 0.dp,
        containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                         else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        val art by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = audioFile.uri) {
            value = ArtworkProvider.loadAudioArtwork(context, Uri.parse(audioFile.uri))
        }

        // Song artwork
        if (art != null) {
            Image(
                bitmap = art!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(songArtworkShape)
            )
        } else {
            val colorScheme = MaterialTheme.colorScheme
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(songArtworkShape)
                    .background(colorScheme.surfaceVariant)
                    .border(1.dp, colorScheme.outline, songArtworkShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Song info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = audioFile.title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Duration
        Text(
            text = DurationFormatter.format(audioFile.duration),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            fontSize = 14.sp
        )
        }
    }
}
