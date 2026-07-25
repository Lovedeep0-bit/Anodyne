package com.anodyne.app.viewmodel

import com.anodyne.app.data.AdvancedSettings
import com.anodyne.app.data.SimplePlaybackSettings
import com.anodyne.app.data.SimpleProgressData

data class PlayerState(
    val errorMessage: String? = null,
    val showControls: Boolean = true,
    val isPlayerReady: Boolean = false,
    val showResumeDialog: Boolean = false,
    val currentProgress: SimpleProgressData = SimpleProgressData(),
    val currentSettings: SimplePlaybackSettings = SimplePlaybackSettings(),
    val currentAdvancedSettings: AdvancedSettings = AdvancedSettings(),
    val showSettings: Boolean = false,
    val showAdvancedSettings: Boolean = false
)
