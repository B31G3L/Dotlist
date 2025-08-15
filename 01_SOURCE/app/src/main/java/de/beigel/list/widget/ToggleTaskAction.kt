
package de.beigel.list.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import de.beigel.list.data.TaskDatabase
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

class ToggleTaskAction : ActionCallback {

    companion object {
        private val TASK_ID_KEY = ActionParameters.Key<String>("taskId")

        fun createParameters(taskId: String): ActionParameters {
            return ActionParameters.Builder()
                .add(TASK_ID_KEY, taskId)
                .build()
        }
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[TASK_ID_KEY] ?: return

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
