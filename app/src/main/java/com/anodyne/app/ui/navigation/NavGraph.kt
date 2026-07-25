package com.anodyne.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.animation.core.tween
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.anodyne.app.data.AudioCategory
import com.anodyne.app.data.AudioFolder
import com.anodyne.app.ui.screens.HomeScreen
import com.anodyne.app.ui.screens.AudioListScreen
import com.anodyne.app.ui.screens.AudioPlayerScreen
import com.anodyne.app.viewmodel.AudioListViewModel
import com.anodyne.app.viewmodel.HomeViewModel

object Destinations {
    const val HOME = "home"
    const val AUDIO_LIST = "audio_list/{category}"
    const val AUDIO_PLAYER = "audio_player?title={title}&uri={uri}"
    const val AUDIO_PLAYER_CURRENT = "audio_player_current"
    const val MUSIC = "music"
}

@Composable
fun AppNavGraph(navController: NavHostController, homeViewModel: HomeViewModel) {
    NavHost(
        navController = navController,
        startDestination = Destinations.HOME,
        enterTransition = { fadeIn(animationSpec = tween(0)) },
        exitTransition = { fadeOut(animationSpec = tween(0)) },
        popEnterTransition = { fadeIn(animationSpec = tween(0)) },
        popExitTransition = { fadeOut(animationSpec = tween(0)) }
    ) {
        composable(Destinations.HOME) {
            HomeScreen(
                viewModel = homeViewModel,
                onMusicTab = { navController.navigate(Destinations.MUSIC) },
                onOpenFolder = { folder ->
                    val name = java.net.URLEncoder.encode(folder.name, "UTF-8")
                    navController.navigate("audio_list/${name}")
                },
                onOpenNowPlaying = { navController.navigate(Destinations.AUDIO_PLAYER_CURRENT) }
            )
        }
        composable(Destinations.AUDIO_LIST) { backStackEntry ->
            val arg = backStackEntry.arguments?.getString("category") ?: return@composable
            val folderName = java.net.URLDecoder.decode(arg, "UTF-8")
            val vm: AudioListViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val inFolder = vm.itemsForFolder(folderName).collectAsState(initial = emptyList()).value
            AudioListScreen(
                title = folderName,
                itemsList = inFolder,
                onItemClick = { /* no-op navigate; playback handled without opening player */ },
                onOpenNowPlaying = { navController.navigate(Destinations.AUDIO_PLAYER_CURRENT) }
            )
        }
        composable(Destinations.MUSIC) {
            val vm: AudioListViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val items = vm.itemsForFolder("Music").collectAsState(initial = emptyList()).value
            AudioListScreen(
                title = "Music",
                itemsList = items,
                onItemClick = { /* no-op navigate; playback handled without opening player */ },
                onOpenNowPlaying = { navController.navigate(Destinations.AUDIO_PLAYER_CURRENT) }
            )
        }
        composable(Destinations.AUDIO_PLAYER) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: ""
            val uri = backStackEntry.arguments?.getString("uri") ?: return@composable
            AudioPlayerScreen(title = java.net.URLDecoder.decode(title, "UTF-8"), uri = java.net.URLDecoder.decode(uri, "UTF-8"))
        }
        composable(Destinations.AUDIO_PLAYER_CURRENT) {
            AudioPlayerScreen()
        }
    }
}


