package de.beigel.list.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Heute-Aufgaben Widget Receiver
 */
class TodayTasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayTasksWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        // Aktualisiere Widget-Einstellungen
        val preferences = WidgetPreferences(context)
        appWidgetIds.forEach { widgetId ->
            preferences.setWidgetType(widgetId, WidgetType.TODAY_TASKS)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)

        // Cleanup Widget-Einstellungen
        val preferences = WidgetPreferences(context)
        appWidgetIds.forEach { widgetId ->
            preferences.removeWidget(widgetId)
        }
    }
}

/**
 * Quick-Add Widget Receiver
 */
class QuickAddWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        // Aktualisiere Widget-Einstellungen
        val preferences = WidgetPreferences(context)
        appWidgetIds.forEach { widgetId ->
            preferences.setWidgetType(widgetId, WidgetType.QUICK_ADD)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)

        // Cleanup Widget-Einstellungen
        val preferences = WidgetPreferences(context)
        appWidgetIds.forEach { widgetId ->
            preferences.removeWidget(widgetId)
        }
    }
}

/**
 * Widget Manager für zentrale Widget-Verwaltung
 */
object WidgetManager {

    /**
     * Aktualisiert alle Widgets nach Datenänderungen
     */
    fun updateAllWidgets(context: Context) {
        WidgetActionHandler.updateAllWidgets(context)
    }

    /**
     * Initialisiert Widgets beim App-Start
     */
    fun initializeWidgets(context: Context) {
        WidgetActionHandler.scheduleWidgetUpdate(context, 500)
    }

    /**
     * Cleanup beim App-Update oder Reset
     */
    fun cleanupWidgets(context: Context) {
        val preferences = WidgetPreferences(context)
        // Hier könnte Cleanup-Logik stehen
    }

    /**
     * Holt Widget-Informationen für Debugging
     */
    fun getWidgetInfo(context: Context): List<WidgetInfo> {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val preferences = WidgetPreferences(context)
        val widgetInfos = mutableListOf<WidgetInfo>()

        try {
            // Today Tasks Widgets
            val todayWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, TodayTasksWidgetReceiver::class.java)
            )

            todayWidgetIds.forEach { widgetId ->
                widgetInfos.add(
                    WidgetInfo(
                        id = widgetId,
                        type = WidgetType.TODAY_TASKS,
                        isActive = true,
                        lastUpdate = System.currentTimeMillis()
                    )
                )
            }

            // Quick Add Widgets
            val quickAddWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, QuickAddWidgetReceiver::class.java)
            )

            quickAddWidgetIds.forEach { widgetId ->
                widgetInfos.add(
                    WidgetInfo(
                        id = widgetId,
                        type = WidgetType.QUICK_ADD,
                        isActive = true,
                        lastUpdate = System.currentTimeMillis()
                    )
                )
            }

        } catch (e: Exception) {
            // Fehlerbehandlung
        }

        return widgetInfos
    }
}

/**
 * Widget-Informationen für Management
 */
data class WidgetInfo(
    val id: Int,
    val type: WidgetType,
    val isActive: Boolean,
    val lastUpdate: Long
)