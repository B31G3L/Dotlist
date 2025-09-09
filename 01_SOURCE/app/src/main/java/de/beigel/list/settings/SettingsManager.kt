package de.beigel.list.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.beigel.list.data.TaskContext
import de.beigel.list.data.EnergyLevel
import de.beigel.list.utils.UserContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    // Basic Settings
    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications_enabled", false)
        set(value) = prefs.edit().putBoolean("notifications_enabled", value).apply()

    var notificationHour: Int
        get() = prefs.getInt("notification_hour", 9)
        set(value) = prefs.edit().putInt("notification_hour", value).apply()

    var notificationMinute: Int
        get() = prefs.getInt("notification_minute", 0)
        set(value) = prefs.edit().putInt("notification_minute", value).apply()

    // Smart Focus Settings
    var useSmartFocus: Boolean
        get() = prefs.getBoolean("use_smart_focus", true)
        set(value) = prefs.edit().putBoolean("use_smart_focus", value).apply()

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean("is_first_launch", true)
        set(value) = prefs.edit().putBoolean("is_first_launch", value).apply()

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean("has_completed_onboarding", false)
        set(value) = prefs.edit().putBoolean("has_completed_onboarding", value).apply()

    // UI Preferences
    var showMotivationalMessages: Boolean
        get() = prefs.getBoolean("show_motivational_messages", true)
        set(value) = prefs.edit().putBoolean("show_motivational_messages", value).apply()

    var enableAutoRefresh: Boolean
        get() = prefs.getBoolean("enable_auto_refresh", true)
        set(value) = prefs.edit().putBoolean("enable_auto_refresh", value).apply()

    var defaultTaskDuration: Int
        get() = prefs.getInt("default_task_duration", 30)
        set(value) = prefs.edit().putInt("default_task_duration", value).apply()

    var defaultEnergyLevel: EnergyLevel
        get() = EnergyLevel.valueOf(prefs.getString("default_energy_level", EnergyLevel.MEDIUM.name)!!)
        set(value) = prefs.edit().putString("default_energy_level", value.name).apply()

    var preferredTaskCount: Int
        get() = prefs.getInt("preferred_task_count", 5)
        set(value) = prefs.edit().putInt("preferred_task_count", value.coerceIn(3, 10)).apply()

    // Advanced Settings
    var enableSmartScoring: Boolean
        get() = prefs.getBoolean("enable_smart_scoring", true)
        set(value) = prefs.edit().putBoolean("enable_smart_scoring", value).apply()

    var scoringAggressiveness: Float
        get() = prefs.getFloat("scoring_aggressiveness", 1.0f)
        set(value) = prefs.edit().putFloat("scoring_aggressiveness", value.coerceIn(0.5f, 2.0f)).apply()

    var enableNaturalLanguageInput: Boolean
        get() = prefs.getBoolean("enable_natural_language_input", true)
        set(value) = prefs.edit().putBoolean("enable_natural_language_input", value).apply()

    // Productivity Settings
    var productivityTrackingEnabled: Boolean
        get() = prefs.getBoolean("productivity_tracking_enabled", true)
        set(value) = prefs.edit().putBoolean("productivity_tracking_enabled", value).apply()

    var weeklyGoal: Int
        get() = prefs.getInt("weekly_goal", 25)
        set(value) = prefs.edit().putInt("weekly_goal", value.coerceIn(5, 100)).apply()

    var dailyGoal: Int
        get() = prefs.getInt("daily_goal", 5)
        set(value) = prefs.edit().putInt("daily_goal", value.coerceIn(1, 20)).apply()

    // Theme Settings
    var useDynamicColors: Boolean
        get() = prefs.getBoolean("use_dynamic_colors", true)
        set(value) = prefs.edit().putBoolean("use_dynamic_colors", value).apply()

    var themeMode: ThemeMode
        get() = ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name)!!)
        set(value) = prefs.edit().putString("theme_mode", value.name).apply()

    // Work Schedule Settings
    fun getWorkingHours(): Pair<Int, Int> {
        val startHour = prefs.getInt("work_start_hour", 9)
        val endHour = prefs.getInt("work_end_hour", 17)
        return startHour to endHour
    }

    fun setWorkingHours(startHour: Int, endHour: Int) {
        prefs.edit()
            .putInt("work_start_hour", startHour.coerceIn(0, 23))
            .putInt("work_end_hour", endHour.coerceIn(0, 23))
            .apply()
    }

    fun getWorkingDays(): Set<Int> {
        val defaultDays = setOf(1, 2, 3, 4, 5) // Montag bis Freitag
        val workingDaysString = prefs.getString("working_days", "1,2,3,4,5")
        return workingDaysString?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?.filter { it in 1..7 }
            ?.toSet() ?: defaultDays
    }

    fun setWorkingDays(days: Set<Int>) {
        val validDays = days.filter { it in 1..7 }
        prefs.edit()
            .putString("working_days", validDays.joinToString(","))
            .apply()
    }

    // Context Preferences
    fun getPreferredContexts(): List<TaskContext> {
        val contextsString = prefs.getString("preferred_contexts", "")
        return if (contextsString.isNullOrBlank()) {
            TaskContext.values().toList()
        } else {
            contextsString.split(",")
                .mapNotNull { contextName ->
                    try {
                        TaskContext.valueOf(contextName)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }
        }
    }

    fun setPreferredContexts(contexts: List<TaskContext>) {
        prefs.edit()
            .putString("preferred_contexts", contexts.map { it.name }.joinToString(","))
            .apply()
    }

    // Energy Level Schedule
    fun getEnergySchedule(): Map<Int, EnergyLevel> {
        val schedule = mutableMapOf<Int, EnergyLevel>()

        // Default energy schedule
        val defaultSchedule = mapOf(
            6 to EnergyLevel.MEDIUM,
            7 to EnergyLevel.MEDIUM,
            8 to EnergyLevel.HIGH,
            9 to EnergyLevel.HIGH,
            10 to EnergyLevel.HIGH,
            11 to EnergyLevel.HIGH,
            12 to EnergyLevel.MEDIUM,
            13 to EnergyLevel.MEDIUM,
            14 to EnergyLevel.MEDIUM,
            15 to EnergyLevel.HIGH,
            16 to EnergyLevel.HIGH,
            17 to EnergyLevel.MEDIUM,
            18 to EnergyLevel.MEDIUM,
            19 to EnergyLevel.LOW,
            20 to EnergyLevel.LOW,
            21 to EnergyLevel.LOW,
            22 to EnergyLevel.LOW
        )

        for (hour in 0..23) {
            val energyLevelName = prefs.getString("energy_schedule_$hour", null)
            if (energyLevelName != null) {
                try {
                    schedule[hour] = EnergyLevel.valueOf(energyLevelName)
                } catch (e: IllegalArgumentException) {
                    schedule[hour] = defaultSchedule[hour] ?: EnergyLevel.MEDIUM
                }
            } else {
                schedule[hour] = defaultSchedule[hour] ?: EnergyLevel.MEDIUM
            }
        }

        return schedule
    }

    fun setEnergySchedule(schedule: Map<Int, EnergyLevel>) {
        val editor = prefs.edit()
        schedule.forEach { (hour, energyLevel) ->
            if (hour in 0..23) {
                editor.putString("energy_schedule_$hour", energyLevel.name)
            }
        }
        editor.apply()
    }

    // User Context Generation
    fun getUserContext(): UserContext {
        return UserContext(
            preferredEnergyTimes = getEnergySchedule(),
            contextSchedule = getContextSchedule(),
            productivityPeaks = getProductivityPeaks(),
            preferredTaskDuration = defaultTaskDuration,
            workingDays = getWorkingDays()
        )
    }

    private fun getContextSchedule(): Map<Int, Map<Int, TaskContext>> {
        // Vereinfachte Implementierung - kann erweitert werden
        val workingDays = getWorkingDays()
        val (workStart, workEnd) = getWorkingHours()

        val schedule = mutableMapOf<Int, Map<Int, TaskContext>>()

        for (dayOfWeek in 1..7) {
            val daySchedule = mutableMapOf<Int, TaskContext>()

            if (dayOfWeek in workingDays) {
                for (hour in workStart until workEnd) {
                    daySchedule[hour] = TaskContext.WORK
                }
                // Mittagspause
                if (workStart <= 12 && workEnd > 13) {
                    daySchedule[12] = TaskContext.HOME
                }
            }

            // Abendzeit für Zuhause
            for (hour in 18..21) {
                daySchedule[hour] = TaskContext.HOME
            }

            schedule[dayOfWeek] = daySchedule
        }

        return schedule
    }

    private fun getProductivityPeaks(): List<Int> {
        val peaksString = prefs.getString("productivity_peaks", "9,10,15,16")
        return peaksString?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?.filter { it in 0..23 } ?: listOf(9, 10, 15, 16)
    }

    fun setProductivityPeaks(peaks: List<Int>) {
        val validPeaks = peaks.filter { it in 0..23 }
        prefs.edit()
            .putString("productivity_peaks", validPeaks.joinToString(","))
            .apply()
    }

    // Smart Suggestions Settings
    var enableSmartSuggestions: Boolean
        get() = prefs.getBoolean("enable_smart_suggestions", true)
        set(value) = prefs.edit().putBoolean("enable_smart_suggestions", value).apply()

    var enableContextSuggestions: Boolean
        get() = prefs.getBoolean("enable_context_suggestions", true)
        set(value) = prefs.edit().putBoolean("enable_context_suggestions", value).apply()

    var enableTimeSuggestions: Boolean
        get() = prefs.getBoolean("enable_time_suggestions", true)
        set(value) = prefs.edit().putBoolean("enable_time_suggestions", value).apply()

    // Data & Privacy Settings
    var dataCollectionEnabled: Boolean
        get() = prefs.getBoolean("data_collection_enabled", false)
        set(value) = prefs.edit().putBoolean("data_collection_enabled", value).apply()

    var crashReportingEnabled: Boolean
        get() = prefs.getBoolean("crash_reporting_enabled", true)
        set(value) = prefs.edit().putBoolean("crash_reporting_enabled", value).apply()

    // Backup Settings
    var autoBackupEnabled: Boolean
        get() = prefs.getBoolean("auto_backup_enabled", false)
        set(value) = prefs.edit().putBoolean("auto_backup_enabled", value).apply()

    var backupFrequency: BackupFrequency
        get() = BackupFrequency.valueOf(prefs.getString("backup_frequency", BackupFrequency.WEEKLY.name)!!)
        set(value) = prefs.edit().putString("backup_frequency", value.name).apply()

    var lastBackupTime: Long
        get() = prefs.getLong("last_backup_time", 0)
        set(value) = prefs.edit().putLong("last_backup_time", value).apply()

    // Experimental Features
    var enableExperimentalFeatures: Boolean
        get() = prefs.getBoolean("enable_experimental_features", false)
        set(value) = prefs.edit().putBoolean("enable_experimental_features", value).apply()

    var enableVoiceInput: Boolean
        get() = prefs.getBoolean("enable_voice_input", false)
        set(value) = prefs.edit().putBoolean("enable_voice_input", value).apply()

    var enableGestureNavigation: Boolean
        get() = prefs.getBoolean("enable_gesture_navigation", false)
        set(value) = prefs.edit().putBoolean("enable_gesture_navigation", value).apply()

    // Helper Methods
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }

    fun exportSettings(): Map<String, Any?> {
        return prefs.all
    }

    fun importSettings(settings: Map<String, Any?>) {
        val editor = prefs.edit()
        settings.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Float -> editor.putFloat(key, value)
                is Long -> editor.putLong(key, value)
                is String -> editor.putString(key, value)
            }
        }
        editor.apply()
    }

    fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun getTotalTasksCreated(): Int {
        return prefs.getInt("total_tasks_created", 0)
    }

    fun incrementTasksCreated() {
        val current = getTotalTasksCreated()
        prefs.edit().putInt("total_tasks_created", current + 1).apply()
    }

    fun getTotalTasksCompleted(): Int {
        return prefs.getInt("total_tasks_completed", 0)
    }

    fun incrementTasksCompleted() {
        val current = getTotalTasksCompleted()
        prefs.edit().putInt("total_tasks_completed", current + 1).apply()
    }
}

// Enums für Settings
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

enum class BackupFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    NEVER
}

// Settings Kategorien für UI
enum class SettingsCategory(val displayName: String, val icon: String) {
    GENERAL("Allgemein", "⚙️"),
    SMART_FOCUS("Smart Focus", "🎯"),
    NOTIFICATIONS("Benachrichtigungen", "🔔"),
    PRODUCTIVITY("Produktivität", "📊"),
    APPEARANCE("Erscheinungsbild", "🎨"),
    PRIVACY("Privatsphäre", "🔒"),
    BACKUP("Sicherung", "💾"),
    EXPERIMENTAL("Experimentell", "🧪"),
    ABOUT("Über die App", "ℹ️")
}

// Settings Events für Reactive Programming
sealed class SettingsEvent {
    object ThemeChanged : SettingsEvent()
    object NotificationSettingsChanged : SettingsEvent()
    object SmartFocusToggled : SettingsEvent()
    object ProductivitySettingsChanged : SettingsEvent()
    data class BackupCompleted(val success: Boolean) : SettingsEvent()
    data class SettingsImported(val success: Boolean) : SettingsEvent()
}