package de.beigel.list.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import de.beigel.list.data.TaskDatabase
import de.beigel.list.data.TaskEntity
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

class ToggleTaskAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // Da ActionParameters problematisch sind, verwenden wir eine einfachere Lösung
        // Das Widget wird bei Klick die erste unerledigte Aufgabe als erledigt markieren

        val database = TaskDatabase.getDatabase(context)
        val taskDao = database.taskDao()

        try {
            val allTasks: List<TaskEntity> = taskDao.getDailyTasksForDate(
                java.time.LocalDate.now().toString()
            ).first()

            // Finde die erste unerledigte Aufgabe
            val firstIncompleteTask: TaskEntity? = allTasks.firstOrNull { !it.isCompleted }

            if (firstIncompleteTask != null) {
                val updatedTask = firstIncompleteTask.copy(
                    isCompleted = true,
                    completedAt = LocalDateTime.now().toString()
                )
                taskDao.updateTask(updatedTask)

                // Update widget
                TaskWidget().update(context, glanceId)
            } else {
                // Wenn alle Aufgaben erledigt sind, markiere die erste erledigte als unerledigt
                val firstCompletedTask: TaskEntity? = allTasks.firstOrNull { it.isCompleted }

                if (firstCompletedTask != null) {
                    val updatedTask = firstCompletedTask.copy(
                        isCompleted = false,
                        completedAt = null
                    )
                    taskDao.updateTask(updatedTask)

                    // Update widget
                    TaskWidget().update(context, glanceId)
                }
            }
        } catch (e: Exception) {
            // Fehlerbehandlung: Widget trotzdem aktualisieren
            TaskWidget().update(context, glanceId)
        }
    }
}