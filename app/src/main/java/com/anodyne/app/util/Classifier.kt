package com.anodyne.app.util

import com.anodyne.app.data.AudioCategory

object Classifier {
    private val audiobookKeywords = listOf("audiobook", "audio book", "books")
    private val podcastKeywords = listOf("podcast", "podcasts")
    private val lectureKeywords = listOf("lecture", "lectures", "course", "class", "lesson")

    fun classifyFromPath(pathLower: String): AudioCategory {
        return when {
            audiobookKeywords.any { pathLower.contains(it) } -> AudioCategory.SONGS
            podcastKeywords.any { pathLower.contains(it) } -> AudioCategory.SONGS
            lectureKeywords.any { pathLower.contains(it) } -> AudioCategory.SONGS
            else -> AudioCategory.SONGS
        }
    }
}


