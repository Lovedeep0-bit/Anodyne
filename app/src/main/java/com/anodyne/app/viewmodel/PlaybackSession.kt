package com.anodyne.app.viewmodel

data class PlaybackSession(
    val uri: String = "",
    val position: Long = 0L,
    val isPlaying: Boolean = false
)
