package de.beigel.list.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.beigel.list.repository.TaskRepository
import de.beigel.list.notification.NotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver für Aktionen aus Benachrichtigungen
 * Ermöglicht es, Tasks direkt aus der Benachrichtigung heraus zu bearbeiten
 */
class TaskActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: TaskRepository

    @Inject
    lateinit var notificationManager: NotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "COMPLETE_TASK" -> {
                val taskId = intent.getLongExtra("task_id", -1)
                if (taskId != -1L) {
                    handleCompleteTask(taskId)
                }
            }
            "SNOOZE_TASK" -> {
                val taskId = intent.getLongExtra("task_id", -1)
                if (taskId != -1L) {
                    handleSnoozeTask(taskId)
                }
            }
            "OPEN_APP" -> {
                handleOpenApp(context)
            }
        }
    }

    private fun handleCompleteTask(taskId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val wasCompleted = repository.toggleTaskCompleted(taskId)

                if (wasCompleted) {
                    // Entferne Benachrichtigung
                    notificationManager.cancelTaskNotification(taskId)

                    // Zeige Erfolgs-Benachrichtigung
                    val task = repository.getTaskById(taskId)
                    task?.let {
                        notificationManager.sendTaskCompletedCelebration(it.title)
                    }
                }
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    private fun handleSnoozeTask(taskId: Long) {
        // Entferne aktuelle Benachrichtigung
        notificationManager.cancelTaskNotification(taskId)

        // Plane neue Benachrichtigung in 1 Stunde
        // TODO: Implementiere Snooze-Funktionalität
    }

    private fun handleOpenApp(context: Context) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        launchIntent?.let {
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(it)
        }
    }
}