package com.playmusicfree.app.data.local

import android.content.Context
import androidx.core.content.edit

class ScanPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("scan_prefs", Context.MODE_PRIVATE)

    var minDurationSeconds: Int
        get() = prefs.getInt(KEY_MIN_DURATION, 0)
        set(value) = prefs.edit { putInt(KEY_MIN_DURATION, value) }

    // Empty set = all folders included
    var excludedFolders: Set<String>
        get() = prefs.getStringSet(KEY_EXCLUDED_FOLDERS, emptySet()) ?: emptySet()
        set(value) = prefs.edit { putStringSet(KEY_EXCLUDED_FOLDERS, value) }

    companion object {
        private const val KEY_MIN_DURATION = "min_duration_seconds"
        private const val KEY_EXCLUDED_FOLDERS = "excluded_folders"
    }
}
