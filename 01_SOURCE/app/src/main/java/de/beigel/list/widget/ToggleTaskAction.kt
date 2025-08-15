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
        val taskId = parameters[ActionParameters.Key<String>("taskId")] ?: return

        val database = TaskDatabase.getDatabase(context)
        val taskDao = database.taskDao()

        // Find the task
        val allTasks = taskDao.getTasksForDateRange(
            startDate = java.time.LocalDate.now().toString(),
            endDate = java.time.LocalDate.now().toString()
        ).first()

        val task = allTasks.find { it.id == taskId } ?: return

        // Toggle completion
        val updatedTask = task.copy(
            isCompleted = !task.isCompleted,
            completedAt = if (!task.isCompleted) LocalDateTime.now().toString() else null
        )

        taskDao.updateTask(updatedTask)

        // Update widget
        TaskWidget().update(context, glanceId)
    }
}