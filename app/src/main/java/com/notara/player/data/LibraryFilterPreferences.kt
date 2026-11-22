package com.notara.player.data

import android.content.Context

enum class TrackSortMode {
    DATE_ADDED,
    TITLE_AZ,
    DURATION_DESC,
    FAVORITES_FIRST
}

data class LibraryFilterConfig(
    val hiddenFolders: Set<String> = emptySet(),
    val minDurationSec: Int = 3,
    val sortMode: TrackSortMode = TrackSortMode.DATE_ADDED
)

object LibraryFilterPreferences {
    private const val PREFS = "notara_filter_prefs"
    private const val KEY_HIDDEN = "hidden_folders"
    private const val KEY_MIN_DUR = "min_duration_sec"
    private const val KEY_SORT = "sort_mode"

    fun load(context: Context): LibraryFilterConfig {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val hidden = sp.getStringSet(KEY_HIDDEN, emptySet())?.toSet() ?: emptySet()
        val minDur = sp.getInt(KEY_MIN_DUR, 3)
        val sortRaw = sp.getString(KEY_SORT, TrackSortMode.DATE_ADDED.name)
        val sort = TrackSortMode.entries.firstOrNull { it.name == sortRaw } ?: TrackSortMode.DATE_ADDED
        return LibraryFilterConfig(hiddenFolders = hidden, minDurationSec = minDur, sortMode = sort)
    }

    fun save(context: Context, config: LibraryFilterConfig) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_HIDDEN, config.hiddenFolders)
            .putInt(KEY_MIN_DUR, config.minDurationSec)
            .putString(KEY_SORT, config.sortMode.name)
            .apply()
    }
}
