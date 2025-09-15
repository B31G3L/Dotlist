package de.beigel.list.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import de.beigel.list.data.TaskDatabase
import de.beigel.list.data.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime

class ToggleTaskAction : ActionCallback {

    companion object {
        // Mutex to prevent concurrent widget updates
        private val updateMutex = Mutex()
        private var lastUpdateTime = 0L
        private const val MIN_UPDATE_INTERVAL = 500L // 500ms between updates
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // Prevent rapid successive clicks
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime < MIN_UPDATE_INTERVAL) {
            return
        }

        updateMutex.withLock {
            // Double-check timing inside the lock
            if (currentTime - lastUpdateTime < MIN_UPDATE_INTERVAL) {
                return
            }

            lastUpdateTime = currentTime

            val database = TaskDatabase.getDatabase(context)
            val taskDao = database.taskDao()

            try {
                val allTasks: List<TaskEntity> = taskDao.getDailyTasksForDate(
                    java.time.LocalDate.now().toString()
                ).first()

                if (allTasks.isEmpty()) {
                    // No tasks to toggle, just update widget
                    TaskWidget().update(context, glanceId)
                    return
                }

                // Find the first incomplete task
                val firstIncompleteTask: TaskEntity? = allTasks.firstOrNull { !it.isCompleted }

                if (firstIncompleteTask != null) {
                    // Mark first incomplete task as completed
                    val updatedTask = firstIncompleteTask.copy(
                        isCompleted = true,
                        completedAt = LocalDateTime.now().toString()
                    )
                    taskDao.updateTask(updatedTask)
                } else {
                    // All tasks completed, mark first completed task as incomplete
                    val firstCompletedTask: TaskEntity? = allTasks.firstOrNull { it.isCompleted }

                    if (firstCompletedTask != null) {
                        val updatedTask = firstCompletedTask.copy(
                            isCompleted = false,
                            completedAt = null
                        )
                        taskDao.updateTask(updatedTask)
                    }
                }

                // Update widget with new data
                TaskWidget().update(context, glanceId)

            } catch (e: Exception) {
                // On error, still update the widget to prevent stuck state
                try {
                    TaskWidget().update(context, glanceId)
                } catch (updateError: Exception) {
                    // If even widget update fails, log it (in a real app)
                    // For now, just silently fail
                }
            }
        }
    }
}