package com.anodyne.app.data

import kotlinx.serialization.Serializable

// Basic data classes for media playback
@Serializable
data class AudioTrack(
    val id: String,
    val language: String? = null,
    val label: String? = null,
    val channelCount: Int = 0,
    val sampleRate: Int = 0,
    val bitrate: Long = 0L,
    val isSelected: Boolean = false,
    val isDefault: Boolean = false
)

@Serializable
data class SubtitleTrack(
    val id: String,
    val language: String? = null,
    val label: String? = null,
    val isEmbedded: Boolean = false,
    val isSelected: Boolean = false,
    val isDefault: Boolean = false
)

@Serializable
enum class AspectRatio {
    FIT,
    FILL,
    STRETCH,
    ORIGINAL,
    CUSTOM_16_9,
    CUSTOM_4_3
}

@Serializable
data class AudioFile(
    val id: Long,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val duration: Long = 0L,
    val uri: String,
    val size: Long = 0L,
    val path: String? = null
)

@Serializable
data class AudioFolder(
    val id: Long,
    val name: String,
    val path: String,
    val audioCount: Int = 0,
    val totalDuration: Long = 0L
)

@Serializable
enum class AudioCategory {
    SONGS,
    ALBUMS,
    ARTISTS,
    FOLDERS,
    PLAYLISTS
}

@Serializable
data class AdvancedPlaybackSettings(
    val selectedAudioTrackId: String? = null,
    val selectedSubtitleTrackId: String? = null,
    val subtitlesEnabled: Boolean = false,
    val audioTrackAutoSelect: Boolean = true,
    val subtitleLanguage: String? = null
)

