package de.beigel.list.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE date = :date AND isInDailyList = 1 ORDER BY position ASC")
    fun getDailyTasksForDate(date: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isInDailyList = 0 ORDER BY backlogPosition ASC")
    fun getBacklogTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY position ASC")
    fun getAllTasksForDate(date: String): Flow<List<TaskEntity>>

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

    @Query("UPDATE tasks SET backlogPosition = :position WHERE id = :taskId")
    suspend fun updateBacklogPosition(taskId: String, position: Int)

    @Query("SELECT COUNT(*) FROM tasks WHERE date = :date AND isCompleted = 1 AND isInDailyList = 1")
    suspend fun getCompletedDailyTasksCount(date: String): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE date = :date AND isInDailyList = 1")
    suspend fun getTotalDailyTasksCount(date: String): Int

    // NEU: Backlog-spezifische Queries
    @Query("SELECT * FROM tasks WHERE isInDailyList = 0 AND isCompleted = 0 ORDER BY priority DESC, backlogPosition ASC LIMIT :limit")
    suspend fun getTopBacklogTasks(limit: Int): List<TaskEntity>

    @Query("UPDATE tasks SET isInDailyList = 1, date = :date, position = :position WHERE id = :taskId")
    suspend fun moveTaskToDailyList(taskId: String, date: String, position: Int)

    @Query("UPDATE tasks SET isInDailyList = 0, backlogPosition = :position WHERE id = :taskId")
    suspend fun moveTaskToBacklog(taskId: String, position: Int)

    @Query("SELECT MAX(backlogPosition) FROM tasks WHERE isInDailyList = 0")
    suspend fun getMaxBacklogPosition(): Int?
}