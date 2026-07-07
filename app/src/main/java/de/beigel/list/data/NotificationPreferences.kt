package de.beigel.list.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import de.beigel.list.ui.theme.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Speichert, ob Push-artige System-Benachrichtigungen zusätzlich zu den
 * In-App-Benachrichtigungen angezeigt werden sollen.
 */
object NotificationPreferences {
    private val PUSH_ENABLED = booleanPreferencesKey("push_notifications_enabled")

    fun getPushEnabled(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[PUSH_ENABLED] ?: true }

    suspend fun setPushEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[PUSH_ENABLED] = enabled }
    }
}