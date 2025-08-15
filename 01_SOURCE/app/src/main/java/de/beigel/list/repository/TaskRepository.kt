package de.beigel.list.repository

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class TaskRepository(private val taskDao: TaskDao) {

    fun getTasksForToday(): Flow<List<TaskEntity>> {
        return taskDao.getTasksForDate(LocalDate.now().toString())
    }

    fun getTasksForDate(date: LocalDate): Flow<List<TaskEntity>> {
        return taskDao.getTasksForDate(date.toString())
    }

    fun getTasksForLast7Days(): Flow<List<TaskEntity>> {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(6)
        return taskDao.getTasksForDateRange(startDate.toString(), endDate.toString())
    }

    suspend fun insertTask(task: TaskEntity) {
        taskDao.insertTask(task)
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    suspend fun updateTaskPositions(tasks: List<TaskEntity>) {
        tasks.forEachIndexed { index, task ->
            taskDao.updateTaskPosition(task.id, index)
        }
    }

    suspend fun getCompletionRate(date: LocalDate): Float {
        val dateString = date.toString()
        val completed = taskDao.getCompletedTasksCount(dateString)
        val total = taskDao.getTotalTasksCount(dateString)
        return if (total > 0) completed.toFloat() / total.toFloat() else 0f
    }
}