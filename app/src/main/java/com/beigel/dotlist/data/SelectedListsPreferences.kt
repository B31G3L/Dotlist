package com.beigel.dotlist.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.beigel.dotlist.ui.theme.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object SelectedListsPreferences {

    private val SELECTED_IDS = stringPreferencesKey("selected_list_ids")

    fun getSelectedIds(context: Context): Flow<Set<String>> =
        context.dataStore.data.map { prefs ->
            val raw = prefs[SELECTED_IDS] ?: ""
            if (raw.isBlank()) emptySet()
            else raw.split(",").filter { it.isNotBlank() }.toSet()
        }

    suspend fun setSelectedIds(context: Context, ids: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_IDS] = ids.joinToString(",")
        }
    }
}