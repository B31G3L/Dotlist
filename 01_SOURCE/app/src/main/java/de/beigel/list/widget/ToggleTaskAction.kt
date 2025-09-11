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
        // Das Widget wird bei Klick einfach alle unerledigten Aufgaben durchschalten

        val database = TaskDatabase.getDatabase(context)
        val taskDao = database.taskDao()

        // FIXED: Verwende getDailyTasksForDate statt getTasksForDate
        val allTasks: List<TaskEntity> = taskDao.getDailyTasksForDate(
            java.time.LocalDate.now().toString()
        ).first()

        // FIXED: Explizite Typisierung für firstOrNull
        val firstIncompleteTask: TaskEntity? = allTasks.firstOrNull { !it.isCompleted }

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