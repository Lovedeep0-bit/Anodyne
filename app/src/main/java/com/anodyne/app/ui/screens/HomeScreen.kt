package com.anodyne.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anodyne.app.viewmodel.HomeViewModel
import com.anodyne.app.viewmodel.HomeUiState
import com.anodyne.app.data.AudioFolder
import com.anodyne.app.util.ArtworkProvider
import com.anodyne.app.util.playlistColor
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onMusicTab: () -> Unit,
    onOpenFolder: (AudioFolder) -> Unit,
    onOpenNowPlaying: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme
    val isModern = ModernUIState.isModernUiEnabled

    var showingFolderInline by remember { mutableStateOf(false) }
    LaunchedEffect(uiState) {
        if (!showingFolderInline && uiState is HomeUiState.Success) {
            val folders = (uiState as HomeUiState.Success).folders
            if (folders.size == 1) {
                showingFolderInline = true
            }
        }
    }

    val folders = (uiState as? HomeUiState.Success)?.folders
    if (showingFolderInline && folders?.size == 1) {
        val folder = folders[0]
        val folderFiles = remember(folder.name, uiState) {
            (uiState as? HomeUiState.Success)?.allAudios?.filter { audio ->
                audio.path?.contains(folder.name, ignoreCase = true) == true
            } ?: emptyList()
        }
        AudioListScreen(
            title = folder.name,
            itemsList = folderFiles,
            onItemClick = { },
            onOpenNowPlaying = onOpenNowPlaying,
            onBack = { showingFolderInline = false }
        )
        return
    }

    Scaffold(
        containerColor = colorScheme.background,
        bottomBar = {
            if (isModern) {
                NavigationBar(
                    containerColor = colorScheme.surfaceVariant,
                    modifier = Modifier.background(colorScheme.surfaceVariant)
                ) {
                    val activeColor = colorScheme.primary
                    val inactiveColor = colorScheme.onSurfaceVariant
                    NavigationBarItem(
                        selected = true,
                        onClick = onMusicTab,
                        icon = { Icon(Icons.Default.MusicNote, contentDescription = null, tint = activeColor) },
                        label = { Text("Music", color = activeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = activeColor,
                            unselectedIconColor = inactiveColor,
                            selectedTextColor = activeColor,
                            unselectedTextColor = inactiveColor,
                            indicatorColor = colorScheme.primaryContainer,
                        )
                    )
                }
            } else {
                NavigationBar(
                    containerColor = Color(0xFF121212),
                    modifier = Modifier.background(Color(0xFF121212))
                ) {
                    val activeColor = Color.White
                    val inactiveColor = Color(0xFFA0A0A0)
                    NavigationBarItem(
                        selected = true,
                        onClick = onMusicTab,
                        icon = { Icon(Icons.Default.MusicNote, contentDescription = null, tint = activeColor) },
                        label = { Text("Music", color = activeColor, fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = activeColor,
                            unselectedIconColor = inactiveColor,
                            selectedTextColor = activeColor,
                            unselectedTextColor = inactiveColor,
                            indicatorColor = Color.Transparent,
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Music",
                color = colorScheme.onBackground,
                fontSize = if (isModern) 36.sp else 28.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(24.dp))

            // Expressive search bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable { /* TODO: Open search */ },
                shape = RoundedCornerShape(28.dp),
                color = colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "Search your music",
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 16.sp,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Debug info
            Text(
                text = "Found ${(uiState as? HomeUiState.Success)?.folders?.size ?: 0} folders",
                color = colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 14.sp
            )

            Spacer(Modifier.height(16.dp))

            AnimatedContent(
                targetState = uiState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "home_state"
            ) { state ->
                when (state) {
                    is HomeUiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colorScheme.primary)
                        }
                    }
                    is HomeUiState.Empty, is HomeUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = if (state is HomeUiState.Error)
                                        "Error: ${(state as HomeUiState.Error).message}"
                                    else "No music folders found",
                                    color = colorScheme.onBackground,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Add some music files to get started",
                                    color = colorScheme.onBackground.copy(alpha = 0.6f),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    is HomeUiState.Success -> {
                        if (state.isRefreshing) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                color = colorScheme.primary,
                                trackColor = colorScheme.surfaceVariant
                            )
                        }
                        if (state.folders.isNotEmpty()) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(150.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(state.folders, key = { it.id }) { folder ->
                                    AlbumCard(
                                        folder = folder,
                                        onClick = { onOpenFolder(folder) },
                                        isModern = isModern,
                                        colorScheme = colorScheme
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumCard(
    folder: AudioFolder,
    onClick: () -> Unit,
    isModern: Boolean,
    colorScheme: ColorScheme
) {
    val context = LocalContext.current

    var artwork by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(folder.id) {
        isLoading = true
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .background(if (isModern) colorScheme.surfaceVariant else Color.Transparent)
            .padding(if (isModern) 8.dp else 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(colorScheme.surfaceVariant)
        ) {
            if (artwork != null && !isLoading) {
                Image(
                    bitmap = artwork!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val placeholderColor = playlistColor(folder.name, isModern, colorScheme)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(placeholderColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = if (isModern) colorScheme.onPrimary else Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                if (isModern) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayCircle,
                            contentDescription = "Play",
                            tint = colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = "Play",
                        tint = Color(0xFF1DB954),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = folder.name,
            color = colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(horizontal = if(isModern) 8.dp else 0.dp)
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = "${folder.audioCount} songs",
            color = colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = if(isModern) 8.dp else 0.dp)
        )
    }
}
