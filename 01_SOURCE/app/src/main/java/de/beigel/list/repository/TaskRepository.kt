package de.beigel.list.repository

import de.beigel.list.data.TaskDao
import de.beigel.list.data.TaskEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class TaskRepository(private val taskDao: TaskDao) {

    fun getDailyTasksForToday(): Flow<List<TaskEntity>> {
        return taskDao.getDailyTasksForDate(LocalDate.now().toString())
    }

    fun getBacklogTasks(): Flow<List<TaskEntity>> {
        return taskDao.getBacklogTasks()
    }

    fun getDailyTasksForDate(date: LocalDate): Flow<List<TaskEntity>> {
        return taskDao.getDailyTasksForDate(date.toString())
    }

    fun getAllTasksForDate(date: LocalDate): Flow<List<TaskEntity>> {
        return taskDao.getAllTasksForDate(date.toString())
    }

    fun getTasksForLast7Days(): Flow<List<TaskEntity>> {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(6)
        return taskDao.getTasksForDateRange(startDate.toString(), endDate.toString())
    }

    suspend fun insertTask(task: TaskEntity, addToDaily: Boolean = true) {
        val taskToInsert = if (addToDaily) {
            val dailyCount = taskDao.getTotalDailyTasksCount(LocalDate.now().toString())
            task.copy(
                isInDailyList = true,
                position = dailyCount,
                date = LocalDate.now().toString()
            )
        } else {
            val maxBacklogPos = taskDao.getMaxBacklogPosition() ?: -1
            task.copy(
                isInDailyList = false,
                backlogPosition = maxBacklogPos + 1
            )
        }
        taskDao.insertTask(taskToInsert)
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    suspend fun updateTaskPositions(tasks: List<TaskEntity>) {
        tasks.forEachIndexed { index, task ->
            if (task.isInDailyList) {
                taskDao.updateTaskPosition(task.id, index)
            } else {
                taskDao.updateBacklogPosition(task.id, index)
            }
        }
    }

    suspend fun moveTaskToDaily(task: TaskEntity): Boolean {
        val currentDailyCount = taskDao.getTotalDailyTasksCount(LocalDate.now().toString())
        taskDao.moveTaskToDailyList(
            taskId = task.id,
            date = LocalDate.now().toString(),
            position = currentDailyCount
        )
        return true
    }

    suspend fun moveTaskToBacklog(task: TaskEntity) {
        val maxBacklogPos = taskDao.getMaxBacklogPosition() ?: -1
        taskDao.moveTaskToBacklog(task.id, maxBacklogPos + 1)
    }

    suspend fun fillDailyListFromBacklog(maxDailyTasks: Int) {
        val currentDailyCount = taskDao.getTotalDailyTasksCount(LocalDate.now().toString())
        val slotsToFill = maxDailyTasks - currentDailyCount

        if (slotsToFill > 0) {
            val backlogTasks = taskDao.getTopBacklogTasks(slotsToFill)
            backlogTasks.forEachIndexed { index, task ->
                taskDao.moveTaskToDailyList(
                    taskId = task.id,
                    date = LocalDate.now().toString(),
                    position = currentDailyCount + index
                )
            }
        }
    }

    suspend fun getCompletionRate(date: LocalDate): Float {
        val dateString = date.toString()
        val completed = taskDao.getCompletedDailyTasksCount(dateString)
        val total = taskDao.getTotalDailyTasksCount(dateString)
        return if (total > 0) completed.toFloat() / total.toFloat() else 0f
    }
}