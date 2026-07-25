package com.anodyne.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.launch
import com.anodyne.app.ui.components.MiniPlayer
import com.anodyne.app.data.AudioFolder
import com.anodyne.app.data.AlbumCoverStore
import com.anodyne.app.data.AudioFile
import com.anodyne.app.viewmodel.HomeUiState
import com.anodyne.app.util.AnodyneMotion
import com.anodyne.app.viewmodel.AudioListViewModel
import com.anodyne.app.util.DurationFormatter
import com.anodyne.app.player.PlayerConnection
import android.net.Uri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anodyne.app.ui.screens.ThemeState
import com.anodyne.app.ui.screens.AppTheme
import com.anodyne.app.ui.components.ModernLoadingIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.Crossfade
import com.anodyne.app.BuildConfig


@Composable
fun TabsRootScreen(
    homeViewModel: com.anodyne.app.viewmodel.HomeViewModel,
    topContentPadding: Dp = 0.dp,
    bottomNavPadding: Dp = 0.dp,
    onOpenAudio: (AudioFile) -> Unit,
    onOpenAudioFolder: (String) -> Unit,
    onOpenNowPlaying: () -> Unit,
    initialTab: AppTab = AppTab.Music
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val homeVm = homeViewModel
    val uiState by homeVm.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val libraryTopPadding = if (isLandscape) 48.dp else 96.dp
    val searchTopPadding = if (isLandscape) 80.dp else 160.dp
    val settingsTopPadding = if (isLandscape) 48.dp else 96.dp

    var isSearchVisible by rememberSaveable { mutableStateOf(initialTab == AppTab.Search) }
    LaunchedEffect(initialTab) {
        isSearchVisible = (initialTab == AppTab.Search)
    }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    
    // View/sort state — persisted globally via ViewSortState
    val isGrid = ViewSortState.isGrid
    val playlistSort = ViewSortState.playlistSort
    val trackSort = ViewSortState.trackSort
    val playlistSortAscending = ViewSortState.playlistSortAscending
    val trackSortAscending = ViewSortState.trackSortAscending
    
    val focusRequester = remember { FocusRequester() }
    
    // Settings state - use shared state
    val showSettings = SettingsState.isSettingsOpen
    
    // Load audio files for track search
    val audioListViewModel: AudioListViewModel = viewModel()
    LaunchedEffect(Unit) {
        audioListViewModel.loadAudiosInMusicFolder()
    }
    val allAudioFiles by audioListViewModel.items().collectAsStateWithLifecycle(initialValue = emptyList())
    


    Box(modifier = Modifier.fillMaxSize()) {
    val colorScheme = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colorScheme.background,
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
            // Main UI content
            // Main UI content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topContentPadding)
            ) {
                    
                    // Filter and search logic
                    val folders = (uiState as? HomeUiState.Success)?.folders ?: emptyList()
                    val isRefreshing = (uiState as? HomeUiState.Success)?.isRefreshing ?: false
                    val filteredFolders = remember(folders, isSearchVisible, playlistSort, playlistSortAscending) {
                        val filtered = if (isSearchVisible) emptyList() else folders
                        // Apply sorting
                        when (playlistSort) {
                            PlaylistSort.Alphabetical -> {
                                if (playlistSortAscending) {
                                    filtered.sortedBy { it.name.lowercase() }
                                } else {
                                    filtered.sortedByDescending { it.name.lowercase() }
                                }
                            }
                            PlaylistSort.SongCount -> {
                                if (playlistSortAscending) {
                                    filtered.sortedBy { it.audioCount }
                                } else {
                                    filtered.sortedByDescending { it.audioCount }
                                }
                            }
                        }
                    }
                    
                    val filteredTracks = remember(searchQuery, isSearchVisible, allAudioFiles, trackSort, trackSortAscending) {
                        val filtered = when {
                            !isSearchVisible -> emptyList()
                            searchQuery.isBlank() -> emptyList()
                            else -> allAudioFiles.filter { 
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                (it.artist?.contains(searchQuery, ignoreCase = true) == true)
                            }
                        }
                        // Apply sorting
                        when (trackSort) {
                            TrackSort.Alphabetical -> {
                                if (trackSortAscending) {
                                    filtered.sortedBy { it.title.lowercase() }
                                } else {
                                    filtered.sortedByDescending { it.title.lowercase() }
                                }
                            }
                            TrackSort.Length -> {
                                if (trackSortAscending) {
                                    filtered.sortedBy { it.duration }
                                } else {
                                    filtered.sortedByDescending { it.duration }
                                }
                            }
                        }
                    }
                    
                    if (isSearchVisible) {
                        if (filteredTracks.isNotEmpty()) {
                            TrackList(
                                tracks = filteredTracks,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = searchTopPadding, bottom = 80.dp),
                                onPlayTrack = { track, index ->
                                    val controller = PlayerConnection.controller.value
                                    if (controller != null) {
                                        val mediaItems = filteredTracks.map { f ->
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
                                        val startIndex = index.coerceIn(0, mediaItems.lastIndex)
                                        controller.setMediaItems(mediaItems, startIndex, 0L)
                                        controller.prepare()
                                        controller.play()
                                    }
                                }
                            )
                        } else if (searchQuery.isNotBlank()) {
                            EmptyHint("No results found matching \"$searchQuery\"")
                        }
                    } else {
                        @OptIn(ExperimentalMaterial3Api::class)
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = { homeVm.refreshCounts() },
                            indicator = {}
                        ) {
                            if (isRefreshing) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                            when (uiState) {
                                is HomeUiState.Loading -> {
                                    LoadingPlaceholder()
                                }
                                is HomeUiState.Empty -> {
                                    EmptyHint("No Music found")
                                }
                                is HomeUiState.Error -> {
                                    EmptyHint("Error: ${(uiState as HomeUiState.Error).message}")
                                }
                                is HomeUiState.Success -> {
                                    if (filteredFolders.isEmpty()) {
                                        EmptyHint("No Music found")
                                    } else {
                                        Crossfade(
                                            targetState = isGrid,
                                            animationSpec = AnodyneMotion.defaultTween(),
                                            label = "grid_list_crossfade"
                                        ) { showGrid ->
                                            if (showGrid) {
                                                AlbumGrid(
                                                    folders = filteredFolders,
                                                    onOpen = { onOpenAudioFolder(it) },
                                                    topPadding = libraryTopPadding
                                                )
                                            } else {
                                                AlbumList(
                                                    folders = filteredFolders,
                                                    onOpen = { onOpenAudioFolder(it) },
                                                    topPadding = libraryTopPadding
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // Header with search bar stays on top of lists when search is visible
                if (isSearchVisible) {
                    PlaylistsHeader(
                        onSettingsClick = { SettingsState.openSettings() },
                        onMenuClick = { /* TODO: Open menu */ },
                        onSearchClick = { isSearchVisible = !isSearchVisible },
                        isSearchVisible = isSearchVisible,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        focusRequester = focusRequester
                    )
                }
            }
        }
        
        if (showSettings) {
            val colorScheme = MaterialTheme.colorScheme
            Surface(
                modifier = Modifier.fillMaxSize().padding(top = topContentPadding),
                color = colorScheme.background,
                tonalElevation = 8.dp
            ) {
                SettingsScreen(
                    onBackClick = { SettingsState.closeSettings() },
                    modifier = Modifier.fillMaxSize(),
                    homeViewModel = homeVm,
                    topPadding = settingsTopPadding
                )
            }
        }
    }
}

@Composable
private fun TrackList(
    tracks: List<AudioFile>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 4.dp),
    onPlayTrack: (AudioFile, Int) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = contentPadding
    ) {
        itemsIndexed(tracks, key = { index, track -> track.id }) { index, track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onPlayTrack(track, index) }
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = track.title,
                        color = colorScheme.onBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!track.artist.isNullOrBlank()) {
                        Text(
                            text = track.artist ?: "",
                            color = colorScheme.onBackground.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = DurationFormatter.format(track.duration),
                    color = colorScheme.onBackground.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PlaylistsHeader(
    onSettingsClick: () -> Unit,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    isSearchVisible: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    focusRequester: FocusRequester
) {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 88.dp)) {
        val colorScheme = MaterialTheme.colorScheme
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Center: App title
            if (isSearchVisible) {
                Spacer(modifier = Modifier.width(96.dp))
            } else {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Search bar overlay (when visible)
        AnimatedVisibility(
            visible = isSearchVisible,
            enter = fadeIn(animationSpec = AnodyneMotion.fastTween()) +
                    slideInHorizontally(animationSpec = AnodyneMotion.fastTween()) { it / 2 },
            exit = fadeOut(animationSpec = AnodyneMotion.fastTween()) +
                   slideOutHorizontally(animationSpec = AnodyneMotion.fastTween()) { it / 2 }
        ) {
            LaunchedEffect(isSearchVisible) {
                if (isSearchVisible) {
                    try { focusRequester.requestFocus() } catch (e: Exception) { }
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    shape = RoundedCornerShape(28.dp),
                    color = colorScheme.surfaceVariant,
                    tonalElevation = 1.dp
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search songs...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = colorScheme.onSurface,
                            unfocusedTextColor = colorScheme.onSurface,
                            cursorColor = colorScheme.primary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistMusicNoteGrid() {
    val colorScheme = MaterialTheme.colorScheme
    // Single centered music note placeholder
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
private fun AlbumGrid(
    folders: List<AudioFolder>, 
    onOpen: (String) -> Unit,
    topPadding: Dp
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topPadding, bottom = 80.dp)
    ) {
        items(folders, key = { it.id }) { folder ->
            AlbumCard(
                folder = folder,
                onClick = { onOpen(folder.name) }
            )
        }
    }
}

@Composable
private fun AlbumList(
    folders: List<AudioFolder>,
    onOpen: (String) -> Unit,
    topPadding: Dp
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topPadding, bottom = 80.dp)
    ) {
        items(folders, key = { it.id }) { folder ->
            AlbumListItem(folder = folder, onClick = { onOpen(folder.name) })
        }
    }
}

@Composable
private fun AlbumListItem(
    folder: AudioFolder,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val albumCoverStore = remember { AlbumCoverStore(context) }
    val customCoverUri by albumCoverStore
        .getCustomCoverFlow(folder.name)
        .collectAsStateWithLifecycle(initialValue = null)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (ModernUIState.isModernUiEnabled) colorScheme.surfaceVariant else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(10.dp))
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
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                color = colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${folder.audioCount} ${if (folder.audioCount == 1) "song" else "songs"}",
                color = colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun AlbumCard(
    folder: AudioFolder,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val albumCoverStore = remember { AlbumCoverStore(context) }
    val scope = rememberCoroutineScope()
    
    // Persisted custom cover (flow-backed so it survives recomposition and restarts)
    val customCoverUri by albumCoverStore
        .getCustomCoverFlow(folder.name)
        .collectAsStateWithLifecycle(initialValue = null)
    
    // Image picker launcher - copies image to app internal storage for robust persistence
    var isSavingCover by remember { mutableStateOf(false) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isSavingCover = true
                // saveCustomCoverFromUri copies the image into app internal storage,
                // making it resilient to app restarts and source URI invalidation.
                albumCoverStore.saveCustomCoverFromUri(folder.name, it)
                isSavingCover = false
            }
        }
    }
    
    // Show dialog for cover options
    var showCoverOptions by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme

    if (showCoverOptions) {
        AlertDialog(
            onDismissRequest = { showCoverOptions = false },
            containerColor = colorScheme.surfaceVariant,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = folder.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Live cover preview
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (customCoverUri != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(customCoverUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Current cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                        if (isSavingCover) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color(0x80000000)),
                                contentAlignment = Alignment.Center
                            ) {
                                ModernLoadingIndicator(size = 36.dp, color = colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        imagePickerLauncher.launch(arrayOf("image/*"))
                        showCoverOptions = false
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Choose Image")
                }
            },
            dismissButton = {
                if (customCoverUri != null) {
                    OutlinedButton(
                        onClick = {
                            scope.launch { albumCoverStore.removeCustomCover(folder.name) }
                            showCoverOptions = false
                        },
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Remove")
                    }
                }
            },
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // Playlist artwork - custom cover or music note placeholder
        val colorScheme = MaterialTheme.colorScheme
        val playlistShape = rememberShapeState()
        val composeShape = com.anodyne.app.ui.components.PlaylistShapes.getShape(playlistShape)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(folder.name) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = {
                            showCoverOptions = true
                        }
                    )
                }
        ) {
            // Background and Image layer (Clipped)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(composeShape)
                    .background(colorScheme.surfaceVariant)
            ) {
                if (customCoverUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(customCoverUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    PlaylistMusicNoteGrid()
                }
            }
            
        }
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = folder.name,
            color = colorScheme.onBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        
        Text(
            text = "${folder.audioCount} songs",
            color = colorScheme.onBackground.copy(alpha = 0.6f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun LoadingPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ModernLoadingIndicator(
            size = 48.dp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun FolderList(items: List<AudioFolder>, onOpen: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = { it.id }) { folder ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpen(folder.name) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(folder.name, color = Color.White, fontSize = 16.sp)
                Text("${folder.audioCount}", color = Color(0xFFA0A0A0), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AudioList(items: List<AudioFile>, onOpen: (AudioFile) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = { it.id }) { item ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpen(item) },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White)
                Column(Modifier.weight(1f)) {
                    Text(item.title, color = Color.White, fontSize = 16.sp)
                    // Duration unavailable here in MP3-only simplified list
                    // Text(DurationFormatter.format(item.duration), color = Color(0xFFA0A0A0), fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Transport controls removed in MP3-only simplified list
                }
            }
        }
    }
}

@Composable
private fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    contentBelow: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                color = colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            content()
        }
        if (description != null) {
            Text(
                text = description,
                color = colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        contentBelow()
    }
}

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    homeViewModel: com.anodyne.app.viewmodel.HomeViewModel? = null,
    topPadding: Dp = 96.dp
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Settings content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topPadding, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    // Theme setting
                    ThemeSettingItem()
                }
                item {
                    PrimaryColorSettingItem()
                }
                item {
                    PlaylistShapeSettingItem()
                }
                item {
                    ImmersiveModeSettingItem()
                }
                item {
                    ViewSettingItem()
                }
                item {
                    SortSettingItem()
                }
                item {
                    DirectoriesSettingItem(homeViewModel = homeViewModel)
                }
                item {
                    GithubSettingItem()
                }
                item {
                    VersionInfo()
                }
            }
        }
    }
}

@Composable
private fun ThemeSettingItem() {
    val context = LocalContext.current
    var currentTheme by remember { mutableStateOf(ThemeState.getTheme(context)) }
    var showDropdown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { currentTheme = ThemeState.getTheme(context) }
    val colorScheme = MaterialTheme.colorScheme

    SettingItem(icon = Icons.Default.Palette, title = "Theme", content = {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        Box {
            OutlinedButton(
                onClick = { showDropdown = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text(currentTheme.name, fontSize = 14.sp)
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if (showDropdown) Icons.Default.ArrowDropDown else Icons.Default.ArrowDropDown,
                    contentDescription = "Theme dropdown",
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false },
                modifier = Modifier.background(colorScheme.surfaceVariant)
            ) {
                DropdownMenuItem(
                    text = { Text("Light", color = if (currentTheme == AppTheme.Light) colorScheme.onSurface else colorScheme.onSurfaceVariant) },
                    onClick = {
                        ThemeState.setTheme(context, AppTheme.Light)
                        currentTheme = AppTheme.Light
                        ThemeState.currentTheme = AppTheme.Light
                        showDropdown = false
                        handler.post { (context as? android.app.Activity)?.recreate() }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Dark", color = if (currentTheme == AppTheme.Dark) colorScheme.onSurface else colorScheme.onSurfaceVariant) },
                    onClick = {
                        ThemeState.setTheme(context, AppTheme.Dark)
                        currentTheme = AppTheme.Dark
                        ThemeState.currentTheme = AppTheme.Dark
                        showDropdown = false
                        handler.post { (context as? android.app.Activity)?.recreate() }
                    }
                )
                DropdownMenuItem(
                    text = { Text("OLED", color = if (currentTheme == AppTheme.OLED) colorScheme.onSurface else colorScheme.onSurfaceVariant) },
                    onClick = {
                        ThemeState.setTheme(context, AppTheme.OLED)
                        currentTheme = AppTheme.OLED
                        ThemeState.currentTheme = AppTheme.OLED
                        showDropdown = false
                        handler.post { (context as? android.app.Activity)?.recreate() }
                    }
                )
            }
        }
    })
}

@Composable
fun ModernUISettingItem() {
    val context = LocalContext.current
    val isModernUI = ModernUIState.isModernUiEnabled
    
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Modern UI",
                color = colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Switch(
                checked = isModernUI,
                onCheckedChange = { ModernUIState.setModernUI(context, it) }
            )
        }
    }
}

@Composable
private fun PrimaryColorSettingItem() {
    val context = LocalContext.current
    var currentColor by remember { mutableStateOf(ThemeState.getPrimaryColor(context)) }
    var showDropdown by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme

    SettingItem(icon = Icons.Default.Colorize, title = "Primary Color", content = {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        Box {
            OutlinedButton(
                onClick = { showDropdown = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text(currentColor.displayName, fontSize = 14.sp)
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Color dropdown",
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false },
                modifier = Modifier.background(colorScheme.surfaceVariant)
            ) {
                com.anodyne.app.ui.screens.AppPrimaryColor.entries.forEach { colorOption ->
                    DropdownMenuItem(
                        text = { Text(colorOption.displayName, color = if (currentColor == colorOption) colorScheme.onSurface else colorScheme.onSurfaceVariant) },
                        onClick = {
                            ThemeState.setPrimaryColor(context, colorOption)
                            currentColor = colorOption
                            ThemeState.currentPrimaryColor = colorOption
                            showDropdown = false
                            handler.post { (context as? android.app.Activity)?.recreate() }
                        }
                    )
                }
            }
        }
    })
}

@Composable
fun PlaylistShapeSettingItem() {
    val context = LocalContext.current
    var currentShape by remember { mutableStateOf(ShapeState.getPlaylistShape(context)) }
    var showDropdown by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme

    SettingItem(
        icon = Icons.Default.ViewQuilt,
        title = "Playlist Shape",
        content = {
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            Box {
                OutlinedButton(
                    onClick = { showDropdown = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text(currentShape.displayName, fontSize = 14.sp)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Shape dropdown",
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false },
                    modifier = Modifier.background(colorScheme.surfaceVariant)
                ) {
                    AppPlaylistShape.entries.forEach { shapeOpt ->
                            DropdownMenuItem(
                                text = { Text(shapeOpt.displayName, color = if (currentShape == shapeOpt) colorScheme.onSurface else colorScheme.onSurfaceVariant) },
                                onClick = {
                                    ShapeState.setPlaylistShape(context, shapeOpt)
                                    currentShape = shapeOpt
                                    showDropdown = false
                                    handler.post { (context as? android.app.Activity)?.recreate() }
                                }
                            )
                    }
                }
            }
        }
    )
}

@Composable
fun ImmersiveModeSettingItem() {
    val context = LocalContext.current
    var isImmersive by remember { mutableStateOf(ImmersiveModeState.isImmersiveMode) }

    SettingItem(icon = Icons.Default.Fullscreen, title = "Immersive Mode", content = {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        Switch(
            checked = isImmersive,
            onCheckedChange = { 
                ImmersiveModeState.setImmersiveMode(context, it)
                isImmersive = it
                handler.post { (context as? android.app.Activity)?.recreate() }
            }
        )
    })
}

@Composable
fun DefaultTabSettingItem() {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(DefaultTabState.getDefaultTab(context)) }
    var showDropdown by remember { mutableStateOf(false) }
    
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Startup Tab",
                color = colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Box {
                Row(
                    modifier = Modifier
                        .clickable { showDropdown = true }
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentTab.name,
                        color = colorScheme.onSurface,
                        fontSize = 16.sp
                    )
                    Icon(
                        imageVector = if (showDropdown) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = "Default Tab Dropdown",
                        tint = colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false },
                    modifier = Modifier.background(colorScheme.surfaceVariant)
                ) {
                    DefaultTab.values().forEach { tab ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    tab.name,
                                    color = if (currentTab == tab) colorScheme.onSurface else colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = {
                                DefaultTabState.setDefaultTab(context, tab)
                                currentTab = tab
                                showDropdown = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GithubSettingItem() {
    val context = LocalContext.current
    SettingItem(
        icon = Icons.Default.Code,
        title = "GitHub Repository",
        modifier = Modifier.clickable {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/Lovedeep0-bit/Anodyne"))
            context.startActivity(intent)
        }
    )
}

@Composable
private fun VersionInfo() {
    val colorScheme = MaterialTheme.colorScheme
    Text(
        text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        color = colorScheme.onSurface.copy(alpha = 0.4f),
        fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

@Composable
fun ViewSettingItem() {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val isGrid = ViewSortState.isGrid

    SettingItem(icon = Icons.Default.GridView, title = "View Style", contentBelow = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(true to "Grid", false to "List").forEach { (grid, label) ->
                val selected = isGrid == grid
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.small)
                        .clickable { ViewSortState.setGrid(context, grid) },
                    color = if (selected) colorScheme.primary else colorScheme.surface,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (grid) Icons.Default.GridView else Icons.Default.ViewList,
                            contentDescription = label,
                            tint = if (selected) colorScheme.onPrimary else colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = label,
                            color = if (selected) colorScheme.onPrimary else colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    })
}

@Composable
fun SortSettingItem() {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val playlistSort = ViewSortState.playlistSort
    val playlistAsc = ViewSortState.playlistSortAscending

    SettingItem(icon = Icons.Default.Sort, title = "Sort", contentBelow = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Alphabetical", "Song Count").forEachIndexed { idx, opt ->
                val sel = (idx == 0 && playlistSort == PlaylistSort.Alphabetical) || (idx == 1 && playlistSort == PlaylistSort.SongCount)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.small)
                        .clickable { ViewSortState.setPlaylistSort(context, if (idx == 0) PlaylistSort.Alphabetical else PlaylistSort.SongCount) },
                    color = if (sel) colorScheme.primary else colorScheme.surface,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = opt,
                        color = if (sel) colorScheme.onPrimary else colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            // Asc/Desc toggle
            Surface(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable { ViewSortState.setPlaylistSortAscending(context, !playlistAsc) },
                color = colorScheme.surface,
                shape = MaterialTheme.shapes.small
            ) {
                Icon(
                    imageVector = if (playlistAsc) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = if (playlistAsc) "Ascending" else "Descending",
                    tint = colorScheme.onSurface,
                    modifier = Modifier.padding(8.dp).size(18.dp)
                )
            }
        }
    })
}

@Composable
fun DirectoriesSettingItem(
    homeViewModel: com.anodyne.app.viewmodel.HomeViewModel? = null
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    // Human-readable display names of added SAF folders
    var directories by remember { mutableStateOf(com.anodyne.app.utils.ScannedDirectoriesState.getDirectories(context)) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // addTreeUri handles takePersistableUriPermission internally
            com.anodyne.app.utils.ScannedDirectoriesState.addTreeUri(context, uri)
            directories = com.anodyne.app.utils.ScannedDirectoriesState.getDirectories(context)
            homeViewModel?.let { it.invalidateCache(); it.refreshCounts(forceRefresh = true) }
        }
    }

    SettingItem(
        icon = Icons.Default.Folder,
        title = "Media Directories",
        content = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { launcher.launch(null) }) {
                    Text("+", color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                }
                IconButton(
                    onClick = {
                        homeViewModel?.let { it.invalidateCache(); it.refreshCounts(forceRefresh = true) }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh directories",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        contentBelow = {
            if (directories.isEmpty()) {
                Text(
                    text = "No directories added",
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    directories.forEach { displayName ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayName,
                                color = colorScheme.onSurface,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                onClick = {
                                    com.anodyne.app.utils.ScannedDirectoriesState.removeByDisplayName(context, displayName)
                                    directories = com.anodyne.app.utils.ScannedDirectoriesState.getDirectories(context)
                                    homeViewModel?.let { it.invalidateCache(); it.refreshCounts(forceRefresh = true) }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove directory",
                                    tint = colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

