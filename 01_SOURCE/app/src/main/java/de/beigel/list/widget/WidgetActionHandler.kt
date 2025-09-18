package de.beigel.list.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import de.beigel.list.MainActivity
import de.beigel.list.data.TaskPriority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Behandelt alle Widget-Aktionen und Interaktionen
 */
object WidgetActionHandler {

    private const val REQUEST_CODE_OPEN_APP = 1001
    private const val REQUEST_CODE_ADD_TASK = 1002
    private const val REQUEST_CODE_TOGGLE_TASK = 1003

    /**
     * Öffnet die Hauptapp
     */
    fun handleOpenApp(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("widget_action", "open_app")
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback für Systeme wo startActivity nicht funktioniert
            val pendingIntent = PendingIntent.getActivity(
                context,
                REQUEST_CODE_OPEN_APP,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                pendingIntent.send()
            } catch (sendException: Exception) {
                showErrorToast(context, "Konnte App nicht öffnen")
            }
        }
    }

    /**
     * Öffnet die App mit Add-Task Dialog
     */
    fun handleAddTask(context: Context, addToDaily: Boolean = true) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("widget_action", "add_task")
            putExtra("add_to_daily", addToDaily)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            showErrorToast(context, "Konnte Add-Dialog nicht öffnen")
        }
    }

    /**
     * Fügt eine Quick-Task hinzu (direkt über Widget)
     */
    fun handleQuickAddTask(context: Context, title: String, priority: TaskPriority) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = WidgetRepository(context)
                val success = repository.addQuickTask(title, priority, addToDaily = true)

                CoroutineScope(Dispatchers.Main).launch {
                    if (success) {
                        showSuccessToast(context, "\"$title\" hinzugefügt")
                        updateAllWidgets(context)
                    } else {
                        showErrorToast(context, "Konnte Aufgabe nicht hinzufügen")
                    }
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    showErrorToast(context, "Fehler beim Hinzufügen")
                }
            }
        }
    }

    /**
     * Togglet eine Aufgabe zwischen erledigt/offen
     */
    fun handleToggleTask(context: Context, taskId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = WidgetRepository(context)
                val success = repository.toggleTaskCompletion(taskId)

                CoroutineScope(Dispatchers.Main).launch {
                    if (success) {
                        showSuccessToast(context, "Aufgabe aktualisiert")
                        updateAllWidgets(context)
                    } else {
                        showErrorToast(context, "Konnte Aufgabe nicht aktualisieren")
                    }
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    showErrorToast(context, "Fehler beim Aktualisieren")
                }
            }
        }
    }

    /**
     * Aktualisiert alle Widgets
     */
    fun updateAllWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val glanceManager = GlanceAppWidgetManager(context)

                // Update Today Tasks Widgets
                TodayTasksWidget().updateAll(context)

                // Update Quick Add Widgets
                QuickAddWidget().updateAll(context)

            } catch (e: Exception) {
                // Stille Fehlerbehandlung - Widgets sollten die App nicht zum Absturz bringen
            }
        }
    }

    /**
     * Erstellt PendingIntent für Widget-Aktionen
     */
    fun createActionPendingIntent(
        context: Context,
        action: WidgetAction,
        requestCode: Int = 0
    ): PendingIntent {
        val intent = when (action) {
            is WidgetAction.OpenApp -> {
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            }
            is WidgetAction.AddTask -> {
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("widget_action", "add_task")
                }
            }
            is WidgetAction.ToggleTask -> {
                Intent(context, WidgetActionReceiver::class.java).apply {
                    action = "TOGGLE_TASK"
                    putExtra(WidgetAction.EXTRA_TASK_ID, action.taskId)
                }
            }
            is WidgetAction.QuickAddTask -> {
                Intent(context, WidgetActionReceiver::class.java).apply {
                    action = "QUICK_ADD_TASK"
                    putExtra(WidgetAction.EXTRA_TASK_TITLE, action.title)
                    putExtra(WidgetAction.EXTRA_TASK_PRIORITY, action.priority.name)
                }
            }
            is WidgetAction.RefreshWidget -> {
                Intent(context, WidgetActionReceiver::class.java).apply {
                    action = "REFRESH_WIDGET"
                }
            }
        }

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Hilfsfunktionen für Toast-Nachrichten
     */
    private fun showSuccessToast(context: Context, message: String) {
        try {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Toast kann in manchen Kontexten fehlschlagen
        }
    }

    private fun showErrorToast(context: Context, message: String) {
        try {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Toast kann in manchen Kontexten fehlschlagen
        }
    }

    /**
     * Utility für Widget-Updates nach Datenänderungen
     */
    fun scheduleWidgetUpdate(context: Context, delayMillis: Long = 1000) {
        CoroutineScope(Dispatchers.Main).launch {
            kotlinx.coroutines.delay(delayMillis)
            updateAllWidgets(context)
        }
    }
}

/**
 * BroadcastReceiver für Widget-Aktionen
 */
class WidgetActionReceiver : android.content.BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "TOGGLE_TASK" -> {
                val taskId = intent.getStringExtra(WidgetAction.EXTRA_TASK_ID)
                if (taskId != null) {
                    WidgetActionHandler.handleToggleTask(context, taskId)
                }
            }
            "QUICK_ADD_TASK" -> {
                val title = intent.getStringExtra(WidgetAction.EXTRA_TASK_TITLE)
                val priorityName = intent.getStringExtra(WidgetAction.EXTRA_TASK_PRIORITY)

                if (title != null && priorityName != null) {
                    try {
                        val priority = TaskPriority.valueOf(priorityName)
                        WidgetActionHandler.handleQuickAddTask(context, title, priority)
                    } catch (e: IllegalArgumentException) {
                        WidgetActionHandler.handleQuickAddTask(context, title, TaskPriority.MEDIUM)
                    }
                }
            }
            "REFRESH_WIDGET" -> {
                WidgetActionHandler.updateAllWidgets(context)
            }
        }
    }
}