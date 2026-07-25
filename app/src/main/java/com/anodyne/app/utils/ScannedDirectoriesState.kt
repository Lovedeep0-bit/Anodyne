package com.anodyne.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Manages the set of SAF tree URIs the user has granted access to.
 * All storage access is via these persisted URIs — no READ_MEDIA_AUDIO
 * or READ_EXTERNAL_STORAGE permissions needed.
 */
object ScannedDirectoriesState {
    private const val PREFS_NAME = "scanned_dirs_prefs"
    private const val KEY_URIS = "scanned_uris"

    /** Return all saved tree URIs as [Uri] objects. */
    fun getSavedTreeUris(context: Context): List<Uri> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getStringSet(KEY_URIS, null) ?: return emptyList()
        return raw.filter { it.isNotBlank() }.map { Uri.parse(it) }
    }

    /** Return saved URIs as display strings (decoded human-readable folder names). */
    fun getDirectories(context: Context): List<String> {
        return getSavedTreeUris(context).map { uriToDisplayName(it) }
    }

    /**
     * Persist a new tree URI and take a persistable URI permission so the access
     * survives app restarts.
     */
    fun addTreeUri(context: Context, treeUri: Uri) {
        // Persist the URI permission
        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) { /* ignore if not persistable */ }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_URIS, null)?.toMutableSet() ?: mutableSetOf()
        current.add(treeUri.toString())
        prefs.edit().putStringSet(KEY_URIS, current).apply()
    }

    /**
     * Remove a tree URI by its display name and release the URI permission.
     */
    fun removeByDisplayName(context: Context, displayName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_URIS, null)?.toMutableSet() ?: return
        val toRemove = current.find { uriToDisplayName(Uri.parse(it)) == displayName }
        if (toRemove != null) {
            current.remove(toRemove)
            prefs.edit().putStringSet(KEY_URIS, current).apply()
            // Release the persisted permission
            try {
                context.contentResolver.releasePersistableUriPermission(
                    Uri.parse(toRemove),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { /* ignore */ }
        }
    }

    /** Decode a SAF tree URI to a human-readable display name. */
    fun uriToDisplayName(uri: Uri): String {
        return try {
            val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
            // e.g. "primary:Music/MyAlbum" → "Music/MyAlbum"
            val parts = docId.split(":")
            parts.getOrNull(1)?.ifBlank { parts[0] } ?: docId
        } catch (_: Exception) {
            uri.lastPathSegment ?: uri.toString()
        }
    }

    // ---- Legacy compatibility helpers (used by MediaRepository) ----

    /** @deprecated Use [addTreeUri] directly. Kept for Settings directory picker. */
    fun addDirectory(context: Context, uriString: String) {
        addTreeUri(context, Uri.parse(uriString))
    }

    /** @deprecated Use [removeByDisplayName] directly. */
    fun removeDirectory(context: Context, displayName: String) {
        removeByDisplayName(context, displayName)
    }
}
