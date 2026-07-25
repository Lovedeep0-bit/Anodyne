package com.anodyne.app.ui.screens

import android.content.Context

object ModernUIState {
    // Modern UI is permanently enabled in Anodyne
    val isModernUiEnabled: Boolean = true
    
    fun initialize(context: Context) { /* Modern UI always enabled */ }
    
    fun setModernUI(context: Context, enabled: Boolean) { /* no-op: always enabled */ }
}
