package com.anodyne.app.util

import android.content.Context
import coil.ImageLoader
import coil.util.DebugLogger

object ImageLoaderProvider {
    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        val existing = instance
        if (existing != null) return existing
        return synchronized(this) {
            instance ?: ImageLoader.Builder(context.applicationContext)
                .crossfade(true)
                .respectCacheHeaders(false)
                .logger(DebugLogger())
                .build()
                .also { instance = it }
        }
    }
}


