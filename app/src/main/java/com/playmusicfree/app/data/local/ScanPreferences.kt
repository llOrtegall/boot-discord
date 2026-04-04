package com.playmusicfree.app.data.local

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject

class ScanPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("scan_prefs", Context.MODE_PRIVATE)

    var minDurationSeconds: Int
        get() = prefs.getInt(KEY_MIN_DURATION, 0)
        set(value) = prefs.edit { putInt(KEY_MIN_DURATION, value) }

    // Empty set = all folders included
    var excludedFolders: Set<String>
        get() = prefs.getStringSet(KEY_EXCLUDED_FOLDERS, emptySet()) ?: emptySet()
        set(value) = prefs.edit { putStringSet(KEY_EXCLUDED_FOLDERS, value) }

    fun getCustomSongTitles(): Map<Long, String> {
        val raw = prefs.getString(KEY_CUSTOM_SONG_TITLES, null).orEmpty()
        if (raw.isBlank()) return emptyMap()

        return runCatching {
            val json = JSONObject(raw)
            json.keys().asSequence()
                .mapNotNull { key ->
                    val id = key.toLongOrNull() ?: return@mapNotNull null
                    val title = json.optString(key).trim()
                    if (title.isBlank()) null else id to title
                }
                .toMap()
        }.getOrDefault(emptyMap())
    }

    fun setCustomSongTitle(songId: Long, title: String) {
        val current = getCustomSongTitles().toMutableMap()
        val clean = title.trim()
        if (clean.isBlank()) {
            current.remove(songId)
        } else {
            current[songId] = clean
        }
        val json = JSONObject()
        current.forEach { (id, customTitle) ->
            json.put(id.toString(), customTitle)
        }
        prefs.edit {
            putString(KEY_CUSTOM_SONG_TITLES, json.toString())
        }
    }

    companion object {
        private const val KEY_MIN_DURATION = "min_duration_seconds"
        private const val KEY_EXCLUDED_FOLDERS = "excluded_folders"
        private const val KEY_CUSTOM_SONG_TITLES = "custom_song_titles"
    }
}
