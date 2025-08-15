package de.beigel.list.settings

import android.content.Context
import android.content.SharedPreferences

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
}