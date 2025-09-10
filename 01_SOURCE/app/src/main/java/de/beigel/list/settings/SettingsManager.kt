package de.beigel.list.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        // Smart Focus Settings
        private val DAILY_TASK_LIMIT = intPreferencesKey("daily_task_limit")
        private val SMART_FOCUS_ENABLED = booleanPreferencesKey("smart_focus_enabled")
        private val SMART_SCORING_ENABLED = booleanPreferencesKey("smart_scoring_enabled")
        private val NATURAL_LANGUAGE_INPUT = booleanPreferencesKey("natural_language_input")

        // Notifications
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        private val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")
        private val DAILY_REMINDERS = booleanPreferencesKey("daily_reminders")

        // Productivity
        private val PRODUCTIVITY_TRACKING = booleanPreferencesKey("productivity_tracking")
        private val DAILY_GOAL = intPreferencesKey("daily_goal")
        private val WEEKLY_GOAL = intPreferencesKey("weekly_goal")
        private val MOTIVATIONAL_MESSAGES = booleanPreferencesKey("motivational_messages")

        // Appearance
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")

        // Working Hours
        private val WORK_START_HOUR = intPreferencesKey("work_start_hour")
        private val WORK_START_MINUTE = intPreferencesKey("work_start_minute")
        private val WORK_END_HOUR = intPreferencesKey("work_end_hour")
        private val WORK_END_MINUTE = intPreferencesKey("work_end_minute")

        // User Preferences
        private val PREFERRED_ENERGY_TIMES = stringPreferencesKey("preferred_energy_times")
        private val WORK_STYLE = stringPreferencesKey("work_style")
        private val PREFERRED_CONTEXTS = stringPreferencesKey("preferred_contexts")

        // Onboarding
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val LAST_RESET_DATE = stringPreferencesKey("last_reset_date")
    }

    // === Smart Focus Settings ===

    val dailyTaskLimit: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DAILY_TASK_LIMIT] ?: 5
    }

    suspend fun setDailyTaskLimit(limit: Int) {
        context.dataStore.edit { preferences ->
            preferences[DAILY_TASK_LIMIT] = limit.coerceIn(1, 20)
        }
    }

    val smartFocusEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SMART_FOCUS_ENABLED] ?: true
    }

    suspend fun setSmartFocusEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SMART_FOCUS_ENABLED] = enabled
        }
    }

    val smartScoringEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SMART_SCORING_ENABLED] ?: true
    }

    suspend fun setSmartScoringEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SMART_SCORING_ENABLED] = enabled
        }
    }

    val naturalLanguageInputEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NATURAL_LANGUAGE_INPUT] ?: true
    }

    suspend fun setNaturalLanguageInputEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NATURAL_LANGUAGE_INPUT] = enabled
        }
    }

    // === Notification Settings ===

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED] ?: false
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    val notificationHour: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATION_HOUR] ?: 9
    }

    val notificationMinute: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATION_MINUTE] ?: 0
    }

    suspend fun setNotificationTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_HOUR] = hour
            preferences[NOTIFICATION_MINUTE] = minute
        }
    }

    // === Productivity Settings ===

    val productivityTrackingEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PRODUCTIVITY_TRACKING] ?: true
    }

    suspend fun setProductivityTrackingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PRODUCTIVITY_TRACKING] = enabled
        }
    }

    val dailyGoal: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DAILY_GOAL] ?: 3
    }

    suspend fun setDailyGoal(goal: Int) {
        context.dataStore.edit { preferences ->
            preferences[DAILY_GOAL] = goal.coerceIn(1, 50)
        }
    }

    val weeklyGoal: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[WEEKLY_GOAL] ?: 20
    }

    suspend fun setWeeklyGoal(goal: Int) {
        context.dataStore.edit { preferences ->
            preferences[WEEKLY_GOAL] = goal.coerceIn(1, 200)
        }
    }

    val motivationalMessagesEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[MOTIVATIONAL_MESSAGES] ?: true
    }

    suspend fun setMotivationalMessagesEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MOTIVATIONAL_MESSAGES] = enabled
        }
    }

    // === Theme Settings ===

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: "system"
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    val dynamicColorsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DYNAMIC_COLORS] ?: true
    }

    suspend fun setDynamicColorsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_COLORS] = enabled
        }
    }

    // === Working Hours ===

    val workStartHour: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[WORK_START_HOUR] ?: 9
    }

    val workStartMinute: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[WORK_START_MINUTE] ?: 0
    }

    val workEndHour: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[WORK_END_HOUR] ?: 17
    }

    val workEndMinute: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[WORK_END_MINUTE] ?: 0
    }

    suspend fun setWorkingHours(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        context.dataStore.edit { preferences ->
            preferences[WORK_START_HOUR] = startHour
            preferences[WORK_START_MINUTE] = startMinute
            preferences[WORK_END_HOUR] = endHour
            preferences[WORK_END_MINUTE] = endMinute
        }
    }

    // === User Profile ===

    val workStyle: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[WORK_STYLE] ?: "flexible"
    }

    suspend fun setWorkStyle(style: String) {
        context.dataStore.edit { preferences ->
            preferences[WORK_STYLE] = style
        }
    }

    val preferredContexts: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val contextsString = preferences[PREFERRED_CONTEXTS] ?: "WORK,HOME"
        contextsString.split(",").filter { it.isNotBlank() }
    }

    suspend fun setPreferredContexts(contexts: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[PREFERRED_CONTEXTS] = contexts.joinToString(",")
        }
    }

    // === Onboarding ===

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }

    // === Reset Tracking ===

    val lastResetDate: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LAST_RESET_DATE]
    }

    suspend fun setLastResetDate(date: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_RESET_DATE] = date
        }
    }

    // === Convenience Properties (for synchronous access) ===

    // Diese sollten nur in seltenen Fällen verwendet werden, wenn Flow nicht möglich ist
    // Hier verwenden wir SharedPreferences als Fallback für synchronen Zugriff
    suspend fun getNotificationsEnabledSync(): Boolean {
        return try {
            notificationsEnabled.first()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getNotificationHourSync(): Int {
        return try {
            notificationHour.first()
        } catch (e: Exception) {
            9
        }
    }

    suspend fun getNotificationMinuteSync(): Int {
        return try {
            notificationMinute.first()
        } catch (e: Exception) {
            0
        }
    }
}