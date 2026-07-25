package com.anodyne.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anodyne.app.player.PlayerConnection
import com.anodyne.app.ui.navigation.AppNavigator
import com.anodyne.app.viewmodel.HomeViewModel
import com.anodyne.app.viewmodel.HomeUiState
import com.anodyne.app.util.AnodyneMotion
import kotlinx.coroutines.flow.collectLatest
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.Row
import com.anodyne.app.ui.components.FloatingNavBar
import com.anodyne.app.ui.components.FloatingNavItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings

enum class AppTab { Music, Search, Settings }

@Composable
fun RootApp(startExpanded: Boolean = false) {
    val context = LocalContext.current
    val isSetupComplete by com.anodyne.app.utils.AppSetupState.isSetupComplete.collectAsStateWithLifecycle()
    val homeViewModel: HomeViewModel = viewModel()
    
    if (!isSetupComplete) {
        SetupScreen(
            onSetupComplete = {
                com.anodyne.app.utils.AppSetupState.setSetupComplete(context, true)
                homeViewModel.invalidateCache()
                homeViewModel.refreshCounts(forceRefresh = true)
            }
        )
        return
    }

    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    LaunchedEffect(Unit) { PlayerConnection.connect(context.applicationContext) }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val isSettingsOpen = SettingsState.isSettingsOpen
    
    val colorScheme = MaterialTheme.colorScheme
    val isImmersive = com.anodyne.app.ui.screens.ImmersiveModeState.isImmersiveMode

    val pillTopMargin = 12.dp
    val contentTopMargin = 0.dp

    ImmersiveModeController(isImmersive = isImmersive)

    Scaffold(
        containerColor = colorScheme.background,
        contentWindowInsets = WindowInsets(0)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (!isImmersive) Modifier.windowInsetsPadding(WindowInsets.systemBars) else Modifier)
        ) {
            var isPlayerExpanded by rememberSaveable { mutableStateOf(startExpanded) }

            LaunchedEffect(Unit) {
                AppNavigator.openNowPlaying.collectLatest {
                    isPlayerExpanded = true
                }
            }

            androidx.activity.compose.BackHandler(enabled = isPlayerExpanded) {
                isPlayerExpanded = false
            }

            val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
            var currentTab by rememberSaveable { mutableStateOf(AppTab.Music) }

            // Sync legacy settings state to currentTab for backwards compatibility
            LaunchedEffect(isSettingsOpen) {
                if (isSettingsOpen && currentTab != AppTab.Settings) {
                    currentTab = AppTab.Settings
                } else if (!isSettingsOpen && currentTab == AppTab.Settings) {
                    currentTab = AppTab.Music
                }
            }
            LaunchedEffect(currentTab) {
                if (currentTab == AppTab.Settings && !isSettingsOpen) {
                    SettingsState.openSettings()
                } else if (currentTab != AppTab.Settings && isSettingsOpen) {
                    SettingsState.closeSettings()
                }
            }

            val navItems = listOf(
                FloatingNavItem(AppTab.Music, Icons.Default.MusicNote, "Music"),
                FloatingNavItem(AppTab.Search, Icons.Default.Search, "Search"),
                FloatingNavItem(AppTab.Settings, Icons.Default.Settings, "Settings")
            )

            Row(Modifier.fillMaxSize()) {
                if (isLandscape) {
                    androidx.compose.material3.NavigationRail(
                        containerColor = colorScheme.surfaceVariant
                    ) {
                        navItems.forEach { item ->
                            androidx.compose.material3.NavigationRailItem(
                                selected = currentTab == item.tab,
                                onClick = { currentTab = item.tab as AppTab },
                                icon = { androidx.compose.material3.Icon(item.icon, contentDescription = item.label) },
                                label = { androidx.compose.material3.Text(item.label) }
                            )
                        }
                    }
                }

                Box(Modifier.weight(1f).fillMaxHeight()) {
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable("home") {
                            TabsRootScreen(
                                homeViewModel = homeViewModel,
                                topContentPadding = contentTopMargin,
                                onOpenAudio = { },
                                onOpenAudioFolder = { folderName ->
                                    val name = java.net.URLEncoder.encode(folderName, "UTF-8")
                                    navController.navigate("audio_list/$name")
                                },
                                onOpenNowPlaying = {
                                    isPlayerExpanded = true
                                },
                                initialTab = currentTab
                            )
                        }

                        composable("audio_list/{folder}") { backStackEntry ->
                            val arg = backStackEntry.arguments?.getString("folder") ?: return@composable
                            val folder = java.net.URLDecoder.decode(arg, "UTF-8")
                            
                            val allAudios = (uiState as? HomeUiState.Success)?.allAudios ?: emptyList()
                            val folderItems = remember(allAudios, folder) {
                                allAudios.filter { audio ->
                                    val p = audio.path ?: ""
                                    val parts = p.replace('\\', '/').split('/')
                                    parts.getOrNull(parts.size - 2) == folder
                                }
                            }
                            
                            AudioListScreen(
                                title = folder, 
                                itemsList = folderItems,
                                onItemClick = { },
                                onOpenNowPlaying = {
                                    isPlayerExpanded = true
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        
                        composable(
                            route = "audio_player?title={title}&uri={uri}",
                            enterTransition = { slideInVertically(initialOffsetY = { it }) },
                            exitTransition = { slideOutVertically(targetOffsetY = { it }) },
                            popEnterTransition = { slideInVertically(initialOffsetY = { it }) },
                            popExitTransition = { slideOutVertically(targetOffsetY = { it }) }
                        ) { backStackEntry ->
                            val rawTitle = backStackEntry.arguments?.getString("title") ?: ""
                            val title = try { java.net.URLDecoder.decode(rawTitle, "UTF-8") } catch (_: Exception) { rawTitle }
                            val rawUri = backStackEntry.arguments?.getString("uri") ?: return@composable
                            val uri = try { java.net.URLDecoder.decode(rawUri, "UTF-8") } catch (_: Exception) { rawUri }
                            AudioPlayerScreen(
                                title = title, 
                                uri = uri,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }

                    var hasActiveMedia by remember { mutableStateOf(false) }
                    val controller = PlayerConnection.controller.collectAsState().value
                    
                    androidx.compose.runtime.DisposableEffect(controller) {
                        if (controller != null) {
                            val listener = object : androidx.media3.common.Player.Listener {
                                override fun onEvents(player: androidx.media3.common.Player, events: androidx.media3.common.Player.Events) {
                                    hasActiveMedia = player.currentMediaItem != null
                                }
                            }
                            controller.addListener(listener)
                            hasActiveMedia = controller.currentMediaItem != null
                            onDispose { controller.removeListener(listener) }
                        } else {
                            hasActiveMedia = false
                            onDispose {}
                        }
                    }

                    val isMiniPlayerVisible = currentTab == AppTab.Music &&
                        (currentRoute == "home" || currentRoute?.startsWith("audio") == true) && 
                        currentRoute?.startsWith("audio_player") != true &&
                        !isSettingsOpen &&
                        hasActiveMedia
                    Column(
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        AnimatedVisibility(
                            visible = isMiniPlayerVisible,
                            enter = slideInVertically(
                                initialOffsetY = { it },
                        animationSpec = AnodyneMotion.mediumSoftSpring()
                            ) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            com.anodyne.app.ui.components.MiniPlayer(
                                onOpenNowPlaying = { isPlayerExpanded = true },
                                modifier = Modifier
                                    .padding(horizontal = if (isImmersive) 0.dp else 12.dp)
                                    .padding(bottom = if (isImmersive || isLandscape) 0.dp else 4.dp),
                                shape = if (isImmersive) androidx.compose.ui.graphics.RectangleShape else androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                enableFullBorder = !isImmersive
                            )
                        }
                    }

                    if (!isLandscape) {
                        val isNavBarVisible = currentRoute == "home"
                        Box(modifier = Modifier.align(Alignment.TopCenter)) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isNavBarVisible,
                                enter = slideInVertically(
                                    initialOffsetY = { -it },
                animationSpec = AnodyneMotion.mediumSoftSpring()
            ) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                            ) {
                                FloatingNavBar(
                                    items = navItems,
                                    selectedItem = currentTab,
                                    onItemSelected = { currentTab = it as AppTab },
                                    modifier = Modifier
                                        .wrapContentWidth()
                                        .padding(top = pillTopMargin)
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isPlayerExpanded,
                enter = slideInVertically(
                    initialOffsetY = { it },
        animationSpec = AnodyneMotion.softSpring()
        ) + fadeIn(animationSpec = AnodyneMotion.defaultTween()),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = AnodyneMotion.effectsSpring()
        )
            ) {
                AudioPlayerScreen(
                    onBack = { isPlayerExpanded = false }
                )
            }


        }
    }
}

@Composable
private fun ImmersiveModeController(isImmersive: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as androidx.activity.ComponentActivity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowInsetsControllerCompat(window, view)

            if (isImmersive) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}
