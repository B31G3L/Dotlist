package de.beigel.list.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import de.beigel.list.data.TaskDatabase
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

class ToggleTaskAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // Da ActionParameters problematisch sind, verwenden wir eine einfachere Lösung
        // Das Widget wird bei Klick einfach alle unerledigten Aufgaben durchschalten

        val database = TaskDatabase.getDatabase(context)
        val taskDao = database.taskDao()

        // Hole alle Aufgaben für heute
        val allTasks = taskDao.getTasksForDate(
            java.time.LocalDate.now().toString()
        ).first()

        // Finde die erste unerledigte Aufgabe und markiere sie als erledigt
        val firstIncompleteTask = allTasks.firstOrNull { !it.isCompleted }

        if (firstIncompleteTask != null) {
            val updatedTask = firstIncompleteTask.copy(
                isCompleted = true,
                completedAt = LocalDateTime.now().toString()
            )
            taskDao.updateTask(updatedTask)
        }

        // Update widget
        TaskWidget().update(context, glanceId)
    }
}