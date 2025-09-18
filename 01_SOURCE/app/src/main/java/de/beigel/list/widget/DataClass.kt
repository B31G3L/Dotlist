package de.beigel.list.widget

import android.content.Context
import de.beigel.list.data.TaskEntity
import de.beigel.list.data.TaskPriority

/**
 * Widget-Typen für die verschiedenen Widget-Varianten
 */
enum class WidgetType(
    val displayName: String,
    val minWidth: Int,
    val minHeight: Int,
    val description: String
) {
    TODAY_TASKS(
        displayName = "Heutige Aufgaben",
        minWidth = 250,
        minHeight = 200,
        description = "Zeigt deine heutigen Aufgaben mit Fortschritt"
    ),
    QUICK_ADD(
        displayName = "Schnell hinzufügen",
        minWidth = 250,
        minHeight = 120,
        description = "Neue Aufgaben sofort vom Homescreen hinzufügen"
    )
}

/**
 * Widget-Daten für Today Tasks Widget
 */
data class TodayWidgetData(
    val tasks: List<TaskEntity> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val maxTasks: Int = 5,
    val progress: Float = 0f,
    val isLoading: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val isOverLimit: Boolean get() = totalCount > maxTasks && maxTasks > 0
    val remainingTasks: List<TaskEntity> get() = tasks.filter { !it.isCompleted }
    val displayTasks: List<TaskEntity> get() = tasks.take(4) // Nur die ersten 4 für Widget
}

/**
 * Widget-Aktionen für Interaktionen
 */
sealed class WidgetAction(val action: String) {
    object OpenApp : WidgetAction("OPEN_APP")
    object AddTask : WidgetAction("ADD_TASK")
    object RefreshWidget : WidgetAction("REFRESH_WIDGET")
    data class ToggleTask(val taskId: String) : WidgetAction("TOGGLE_TASK")
    data class QuickAddTask(val title: String, val priority: TaskPriority) : WidgetAction("QUICK_ADD_TASK")

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_PRIORITY = "extra_task_priority"
        const val EXTRA_WIDGET_ID = "extra_widget_id"
    }
}

/**
 * Widget-Einstellungen erweitert um neue Optionen
 */
class WidgetPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)

    var showCompleted: Boolean
        get() = prefs.getBoolean("widget_show_completed", false)
        set(value) = prefs.edit().putBoolean("widget_show_completed", value).apply()

    var maxItems: Int
        get() = prefs.getInt("widget_max_items", 4)
        set(value) = prefs.edit().putInt("widget_max_items", value).apply()

    var compactMode: Boolean
        get() = prefs.getBoolean("widget_compact_mode", false)
        set(value) = prefs.edit().putBoolean("widget_compact_mode", value).apply()

    var autoRefresh: Boolean
        get() = prefs.getBoolean("widget_auto_refresh", true)
        set(value) = prefs.edit().putBoolean("widget_auto_refresh", value).apply()

    var refreshIntervalMinutes: Int
        get() = prefs.getInt("widget_refresh_interval", 30)
        set(value) = prefs.edit().putInt("widget_refresh_interval", value).apply()

    fun getWidgetType(widgetId: Int): WidgetType {
        val typeName = prefs.getString("widget_type_$widgetId", WidgetType.TODAY_TASKS.name)
        return try {
            WidgetType.valueOf(typeName ?: WidgetType.TODAY_TASKS.name)
        } catch (e: IllegalArgumentException) {
            WidgetType.TODAY_TASKS
        }
    }

    fun setWidgetType(widgetId: Int, type: WidgetType) {
        prefs.edit().putString("widget_type_$widgetId", type.name).apply()
    }

    fun removeWidget(widgetId: Int) {
        prefs.edit().remove("widget_type_$widgetId").apply()
    }
}