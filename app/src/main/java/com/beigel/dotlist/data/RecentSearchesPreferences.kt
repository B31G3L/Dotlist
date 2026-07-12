package com.beigel.dotlist.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.beigel.dotlist.ui.theme.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Speichert die zuletzt genutzten Suchbegriffe (max. 6, neueste zuerst).
 */
object RecentSearchesPreferences {

    private val RECENT = stringPreferencesKey("recent_searches")
    private const val MAX_ENTRIES = 6

    fun getRecent(context: Context): Flow<List<String>> =
        context.dataStore.data.map { prefs ->
            val raw = prefs[RECENT] ?: ""
            if (raw.isBlank()) emptyList()
            else raw.split("|").filter { it.isNotBlank() }
        }

    suspend fun addRecent(context: Context, query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = (prefs[RECENT] ?: "").split("|").filter { it.isNotBlank() }
            val updated = (listOf(trimmed) + current.filter { !it.equals(trimmed, ignoreCase = true) })
                .take(MAX_ENTRIES)
            prefs[RECENT] = updated.joinToString("|")
        }
    }

    suspend fun clearRecent(context: Context) {
        context.dataStore.edit { prefs -> prefs[RECENT] = "" }
    }
}