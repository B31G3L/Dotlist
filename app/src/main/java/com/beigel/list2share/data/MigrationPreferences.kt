package com.beigel.list2share.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.beigel.list2share.ui.theme.dataStore
import kotlinx.coroutines.flow.first

/**
 * Listen, die beim Anmelden bewusst NICHT in die Cloud übernommen wurden.
 *
 * Ohne dieses Gedächtnis würde die App bei jedem Start erneut fragen. Wer
 * „nein" sagt, meint das dauerhaft – bis er die Liste von Hand über den
 * Teilen-Regler doch hochlädt.
 *
 * Die IDs bleiben auch nach dem Löschen einer Liste stehen. Das ist gewollt
 * einfach gehalten: es sind ein paar Zeichen pro Liste, und ein Aufräumen
 * bräuchte einen Abgleich, der mehr kostet als er spart.
 */
object MigrationPreferences {

    private val DECLINED_IDS = stringPreferencesKey("migration_declined_ids")

    suspend fun getDeclinedIds(context: Context): Set<String> {
        val raw = context.dataStore.data.first()[DECLINED_IDS] ?: ""
        return if (raw.isBlank()) emptySet()
        else raw.split(",").filter { it.isNotBlank() }.toSet()
    }

    suspend fun addDeclined(context: Context, ids: Set<String>) {
        if (ids.isEmpty()) return
        context.dataStore.edit { prefs ->
            val existing = (prefs[DECLINED_IDS] ?: "").split(",").filter { it.isNotBlank() }
            prefs[DECLINED_IDS] = (existing + ids).distinct().joinToString(",")
        }
    }

    /** Nach einer nachträglichen Übernahme: die Liste ist jetzt in der Cloud. */
    suspend fun removeDeclined(context: Context, id: String) {
        context.dataStore.edit { prefs ->
            val existing = (prefs[DECLINED_IDS] ?: "").split(",").filter { it.isNotBlank() }
            prefs[DECLINED_IDS] = existing.filterNot { it == id }.joinToString(",")
        }
    }
}
