package com.anodyne.app.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import kotlin.math.abs

@Serializable
data class AudioResult(
    val all: List<AudioFile>,
    val folders: List<AudioFolder>
)

class MediaRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val cacheFile: File
        get() = File(context.cacheDir, "audio_cache.json")

    companion object {
        private const val TAG = "MediaRepository"
        @Volatile
        private var cache: AudioResult? = null

        fun invalidateCache() {
            cache = null
        }
    }

    suspend fun getFullAudioData(forceRefresh: Boolean = false): AudioResult = withContext(ioDispatcher) {
        if (!forceRefresh && cache != null) return@withContext cache!!

        if (!forceRefresh) {
            loadFromDisk()?.let { return@withContext it }
        }

        val treeUris = com.anodyne.app.utils.ScannedDirectoriesState.getSavedTreeUris(context)
        val list = ArrayList<AudioFile>()

        for (treeUri in treeUris) {
            try {
                scanTreeUri(treeUri, list)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to scan tree URI: $treeUri", e)
            }
        }

        val groupedByFolder = list.groupBy { audio ->
            val p = audio.path ?: "Other"
            val parts = p.replace('\\', '/').split('/')
            if (parts.size >= 2) parts[parts.size - 2] else "Other"
        }

        val folderList = groupedByFolder.map { (name, files) ->
            AudioFolder(
                id = abs(name.hashCode()).toLong(),
                name = name,
                path = files.firstOrNull()?.path ?: "",
                audioCount = files.size,
                totalDuration = files.sumOf { it.duration }
            )
        }.sortedBy { it.name.lowercase() }

        val result = AudioResult(list, folderList)
        cache = result
        saveToDisk(result)
        result
    }

    private fun saveToDisk(result: AudioResult) {
        try {
            val jsonStr = json.encodeToString(result)
            cacheFile.parentFile?.mkdirs()
            cacheFile.writeText(jsonStr)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save cache to disk", e)
        }
    }

    private fun loadFromDisk(): AudioResult? {
        return try {
            if (!cacheFile.exists()) return null
            val jsonStr = cacheFile.readText()
            json.decodeFromString<AudioResult>(jsonStr)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load cache from disk", e)
            null
        }
    }

    fun invalidateDiskCache() {
        cache = null
        try {
            if (cacheFile.exists()) cacheFile.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete disk cache", e)
        }
    }

    private fun scanTreeUri(treeUri: Uri, out: ArrayList<AudioFile>) {
        val resolver = context.contentResolver
        val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )
        walkDirectory(treeUri, rootDocUri, out)
    }

    private fun walkDirectory(treeUri: Uri, dirDocUri: Uri, out: ArrayList<AudioFile>) {
        val resolver = context.contentResolver
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getDocumentId(dirDocUri)
        )

        resolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
            ),
            null, null, null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

            while (cursor.moveToNext()) {
                val docId = cursor.getString(idIdx) ?: continue
                val displayName = cursor.getString(nameIdx) ?: ""
                val mime = cursor.getString(mimeIdx) ?: ""
                val size = cursor.getLong(sizeIdx)

                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)

                when {
                    mime == DocumentsContract.Document.MIME_TYPE_DIR -> {
                        walkDirectory(treeUri, docUri, out)
                    }
                    isAudioMime(mime) -> {
                        val audioFile = buildAudioFile(docUri, docId, displayName, size)
                        if (audioFile != null) out.add(audioFile)
                    }
                }
            }
        }
    }

    private fun isAudioMime(mime: String): Boolean = mime.startsWith("audio/")

    private fun buildAudioFile(
        docUri: Uri,
        docId: String,
        displayName: String,
        size: Long
    ): AudioFile? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, docUri)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: displayName.substringBeforeLast('.')
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 0L
            retriever.release()

            AudioFile(
                id = abs(docId.hashCode()).toLong(),
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                uri = docUri.toString(),
                size = size,
                path = docId
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build audio file from $docUri", e)
            null
        }
    }

    suspend fun scanAudio(): List<AudioFile> = getFullAudioData().all

    suspend fun audiosInMusicFolder(): List<AudioFile> = withContext(ioDispatcher) { scanAudio() }

    suspend fun audioFolders(): List<AudioFolder> = getFullAudioData().folders
}
