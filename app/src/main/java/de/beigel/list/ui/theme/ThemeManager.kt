package de.beigel.list.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

object ThemePreferences {
    private val THEME_MODE = stringPreferencesKey("theme_mode")

    fun getThemeMode(context: Context): Flow<ThemeMode> =
        context.dataStore.data.map { prefs ->
            try { ThemeMode.valueOf(prefs[THEME_MODE] ?: ThemeMode.SYSTEM.name) }
            catch (e: Exception) { ThemeMode.SYSTEM }
        }

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        context.dataStore.edit { it[THEME_MODE] = mode.name }
    }
}

object AccentColorPreferences {
    private val ACCENT_COLOR = stringPreferencesKey("accent_color")

    fun getAccentColor(context: Context): Flow<AccentColor> =
        context.dataStore.data.map { prefs ->
            try { AccentColor.valueOf(prefs[ACCENT_COLOR] ?: AccentColor.VIOLET.name) }
            catch (e: Exception) { AccentColor.VIOLET }
        }

    suspend fun setAccentColor(context: Context, accent: AccentColor) {
        context.dataStore.edit { it[ACCENT_COLOR] = accent.name }
    }
}