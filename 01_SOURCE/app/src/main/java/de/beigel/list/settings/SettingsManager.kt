package de.beigel.list.settings

import android.content.Context
import android.content.SharedPreferences
import de.beigel.list.viewmodel.InteractionMode

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications_enabled", false)
        set(value) = prefs.edit().putBoolean("notifications_enabled", value).apply()

    var notificationHour: Int
        get() = prefs.getInt("notification_hour", 9)
        set(value) = prefs.edit().putInt("notification_hour", value).apply()

    var notificationMinute: Int
        get() = prefs.getInt("notification_minute", 0)
        set(value) = prefs.edit().putInt("notification_minute", value).apply()

    // Maximale Anzahl täglicher Aufgaben
    var maxDailyTasks: Int
        get() = prefs.getInt("max_daily_tasks", 5)
        set(value) = prefs.edit().putInt("max_daily_tasks", value).apply()

    // Auto-Backlog aktiviert
    var autoBacklogEnabled: Boolean
        get() = prefs.getBoolean("auto_backlog_enabled", true)
        set(value) = prefs.edit().putBoolean("auto_backlog_enabled", value).apply()

    // Interaktionsmodus
    var interactionMode: InteractionMode
        get() {
            val modeString = prefs.getString("interaction_mode", InteractionMode.MINIMAL.name)
            return try {
                InteractionMode.valueOf(modeString ?: InteractionMode.MINIMAL.name)
            } catch (e: IllegalArgumentException) {
                InteractionMode.MINIMAL
            }
        }
        set(value) = prefs.edit().putString("interaction_mode", value.name).apply()

    // Theme Einstellungen
    var isDarkMode: Boolean
        get() = prefs.getBoolean("dark_mode", false)
        set(value) = prefs.edit().putBoolean("dark_mode", value).apply()

    var useSystemTheme: Boolean
        get() = prefs.getBoolean("use_system_theme", true)
        set(value) = prefs.edit().putBoolean("use_system_theme", value).apply()

    // Widget Einstellungen
    var widgetShowCompleted: Boolean
        get() = prefs.getBoolean("widget_show_completed", false)
        set(value) = prefs.edit().putBoolean("widget_show_completed", value).apply()

    var widgetMaxItems: Int
        get() = prefs.getInt("widget_max_items", 5)
        set(value) = prefs.edit().putInt("widget_max_items", value).apply()

    // Erweiterte Einstellungen
    var enableAnimations: Boolean
        get() = prefs.getBoolean("enable_animations", true)
        set(value) = prefs.edit().putBoolean("enable_animations", value).apply()

    var enableHapticFeedback: Boolean
        get() = prefs.getBoolean("enable_haptic_feedback", true)
        set(value) = prefs.edit().putBoolean("enable_haptic_feedback", value).apply()

    var defaultPriority: String
        get() = prefs.getString("default_priority", "MEDIUM") ?: "MEDIUM"
        set(value) = prefs.edit().putString("default_priority", value).apply()

    // Reset alle Einstellungen
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }

    // Export/Import Einstellungen
    fun exportSettings(): Map<String, Any?> {
        return prefs.all
    }

    fun importSettings(settings: Map<String, Any?>) {
        val editor = prefs.edit()
        settings.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is String -> editor.putString(key, value)
                is Float -> editor.putFloat(key, value)
                is Long -> editor.putLong(key, value)
            }
        }
        editor.apply()
    }
}