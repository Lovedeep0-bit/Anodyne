package com.anodyne.app.ui.screens

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

enum class AppPlaylistShape(val displayName: String) {
    Square("Square"),
    Rounded("Rounded"),
    Circle("Circle"),
    Squircle("Squircle"),
    Clover("Clover"),
    Hexagon("Hexagon"),
    Star("Star"),
    Slanted("Slanted Oval")
}

object ShapeState {
    private const val PREFS_NAME = "shape_prefs"
    private const val KEY_PLAYLIST_SHAPE = "playlist_shape"
    
    private var _currentPlaylistShape by mutableStateOf(AppPlaylistShape.Rounded)
    var currentPlaylistShape: AppPlaylistShape
        get() = _currentPlaylistShape
        set(value) {
            _currentPlaylistShape = value
        }
    
    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val shapeName = prefs.getString(KEY_PLAYLIST_SHAPE, AppPlaylistShape.Rounded.name) ?: AppPlaylistShape.Rounded.name
        _currentPlaylistShape = try {
            AppPlaylistShape.valueOf(shapeName)
        } catch (e: IllegalArgumentException) {
            AppPlaylistShape.Rounded
        }
    }
    
    fun setPlaylistShape(context: Context, shape: AppPlaylistShape) {
        _currentPlaylistShape = shape
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PLAYLIST_SHAPE, shape.name).apply()
    }
    
    fun getPlaylistShape(context: Context): AppPlaylistShape {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val shapeName = prefs.getString(KEY_PLAYLIST_SHAPE, AppPlaylistShape.Rounded.name) ?: AppPlaylistShape.Rounded.name
        return try {
            AppPlaylistShape.valueOf(shapeName)
        } catch (e: IllegalArgumentException) {
            AppPlaylistShape.Rounded
        }
    }
}

@Composable
fun rememberShapeState(): AppPlaylistShape {
    val context = LocalContext.current
    return remember {
        mutableStateOf(ShapeState.getPlaylistShape(context))
    }.value
}
