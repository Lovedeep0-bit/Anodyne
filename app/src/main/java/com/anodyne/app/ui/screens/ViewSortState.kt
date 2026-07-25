package com.anodyne.app.ui.screens

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class PlaylistSort { Alphabetical, SongCount }
enum class TrackSort { Alphabetical, Length }

object ViewSortState {
    private const val PREFS_NAME = "view_sort_prefs"
    private const val KEY_IS_GRID = "is_grid"
    private const val KEY_PLAYLIST_SORT = "playlist_sort"
    private const val KEY_TRACK_SORT = "track_sort"
    private const val KEY_PLAYLIST_ASCENDING = "playlist_ascending"
    private const val KEY_TRACK_ASCENDING = "track_ascending"

    var isGrid by mutableStateOf(true)
        private set

    var playlistSort by mutableStateOf(PlaylistSort.Alphabetical)
        private set

    var trackSort by mutableStateOf(TrackSort.Alphabetical)
        private set

    var playlistSortAscending by mutableStateOf(true)
        private set

    var trackSortAscending by mutableStateOf(true)
        private set

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isGrid = prefs.getBoolean(KEY_IS_GRID, true)
        playlistSort = runCatching {
            PlaylistSort.valueOf(prefs.getString(KEY_PLAYLIST_SORT, PlaylistSort.Alphabetical.name)!!)
        }.getOrDefault(PlaylistSort.Alphabetical)
        trackSort = runCatching {
            TrackSort.valueOf(prefs.getString(KEY_TRACK_SORT, TrackSort.Alphabetical.name)!!)
        }.getOrDefault(TrackSort.Alphabetical)
        playlistSortAscending = prefs.getBoolean(KEY_PLAYLIST_ASCENDING, true)
        trackSortAscending = prefs.getBoolean(KEY_TRACK_ASCENDING, true)
    }

    fun setGrid(context: Context, value: Boolean) {
        isGrid = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_IS_GRID, value).apply()
    }

    fun setPlaylistSort(context: Context, value: PlaylistSort) {
        playlistSort = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_PLAYLIST_SORT, value.name).apply()
    }

    fun setTrackSort(context: Context, value: TrackSort) {
        trackSort = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_TRACK_SORT, value.name).apply()
    }

    fun setPlaylistSortAscending(context: Context, value: Boolean) {
        playlistSortAscending = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_PLAYLIST_ASCENDING, value).apply()
    }

    fun setTrackSortAscending(context: Context, value: Boolean) {
        trackSortAscending = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_TRACK_ASCENDING, value).apply()
    }
}
