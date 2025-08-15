package de.beigel.list.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY position ASC")
    fun getTasksForDate(date: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC, position ASC")
    fun getTasksForDateRange(startDate: String, endDate: String): Flow<List<TaskEntity>>

    @Insert
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("UPDATE tasks SET position = :position WHERE id = :taskId")
    suspend fun updateTaskPosition(taskId: String, position: Int)

    @Query("SELECT COUNT(*) FROM tasks WHERE date = :date AND isCompleted = 1")
    suspend fun getCompletedTasksCount(date: String): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE date = :date")
    suspend fun getTotalTasksCount(date: String): Int
}