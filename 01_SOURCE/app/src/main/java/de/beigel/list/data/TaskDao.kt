package de.beigel.list.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TaskDao {

    // === Grundlegende CRUD-Operationen ===

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    // === Backlog-Operationen ===

    @Query("SELECT * FROM tasks WHERE isInTodayList = 0 AND isCompleted = 0 ORDER BY smartScore DESC, priority DESC, createdAt ASC")
    fun getBacklogTasks(): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE isInTodayList = 0 AND isCompleted = 0")
    suspend fun getBacklogTaskCount(): Int

    // === Heutige Liste ===

    @Query("SELECT * FROM tasks WHERE isInTodayList = 1 AND isCompleted = 0 ORDER BY smartScore DESC, priority DESC")
    fun getTodayTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isInTodayList = 1 ORDER BY isCompleted ASC, smartScore DESC, priority DESC")
    fun getTodayTasksWithCompleted(): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE isInTodayList = 1 AND isCompleted = 0")
    suspend fun getTodayTaskCount(): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE isInTodayList = 1 AND isCompleted = 1")
    suspend fun getCompletedTodayTaskCount(): Int

    // === Prioritäts-Queries ===

    @Query("SELECT * FROM tasks WHERE priority = :priority AND isCompleted = 0 ORDER BY smartScore DESC")
    fun getTasksByPriority(priority: TaskPriority): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isInTodayList = 0 AND isCompleted = 0 ORDER BY priority DESC, dueDate ASC, smartScore DESC LIMIT :limit")
    suspend fun getTopBacklogTasksForToday(limit: Int): List<TaskEntity>

    // === Überfällige Tasks ===

    @Query("SELECT * FROM tasks WHERE dueDate < :today AND isCompleted = 0 ORDER BY dueDate ASC")
    fun getOverdueTasks(today: LocalDate): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE dueDate < :today AND isCompleted = 0")
    suspend fun getOverdueTaskCount(today: LocalDate): Int

    // === Fälligkeitsdatum ===

    @Query("SELECT * FROM tasks WHERE dueDate = :date AND isCompleted = 0 ORDER BY priority DESC")
    fun getTasksDueOn(date: LocalDate): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate BETWEEN :startDate AND :endDate AND isCompleted = 0 ORDER BY dueDate ASC, priority DESC")
    fun getTasksDueBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<TaskEntity>>

    // === Quick Wins ===

    @Query("SELECT * FROM tasks WHERE estimatedMinutes <= 15 AND isCompleted = 0 ORDER BY estimatedMinutes ASC, smartScore DESC")
    fun getQuickWinTasks(): Flow<List<TaskEntity>>

    // === Kontext-basierte Queries ===

    @Query("SELECT * FROM tasks WHERE context = :context AND isCompleted = 0 ORDER BY smartScore DESC, priority DESC")
    fun getTasksByContext(context: String): Flow<List<TaskEntity>>

    // === Energie-Level ===

    @Query("SELECT * FROM tasks WHERE energyLevel = :energyLevel AND isCompleted = 0 ORDER BY smartScore DESC")
    fun getTasksByEnergyLevel(energyLevel: String): Flow<List<TaskEntity>>

    // === Vervollständigungs-Operationen ===

    @Query("UPDATE tasks SET isCompleted = :completed, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun markTaskCompleted(taskId: Long, completed: Boolean, completedAt: String?, updatedAt: String)

    @Query("UPDATE tasks SET isInTodayList = 0, updatedAt = :updatedAt WHERE isCompleted = 1 AND isInTodayList = 1")
    suspend fun moveCompletedTasksToBacklog(updatedAt: String)

    // === Smart Focus Operations ===

    @Query("UPDATE tasks SET smartScore = :score, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateSmartScore(taskId: Long, score: Double, updatedAt: String)

    @Query("UPDATE tasks SET isInTodayList = :inTodayList, lastShownInTodayList = :lastShown, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateTodayListStatus(taskId: Long, inTodayList: Boolean, lastShown: String?, updatedAt: String)

    // === Batch Operations ===

    @Query("UPDATE tasks SET isInTodayList = 0, updatedAt = :updatedAt WHERE isInTodayList = 1 AND isCompleted = 0")
    suspend fun clearTodayList(updatedAt: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    // === Statistiken ===

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1 AND DATE(completedAt) = :date")
    suspend fun getCompletedTasksCountForDate(date: String): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1 AND DATE(completedAt) BETWEEN :startDate AND :endDate")
    suspend fun getCompletedTasksCountBetween(startDate: String, endDate: String): Int

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY completedAt DESC LIMIT :limit")
    fun getRecentlyCompletedTasks(limit: Int): Flow<List<TaskEntity>>

    // === Suche ===

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY isCompleted ASC, smartScore DESC")
    fun searchTasks(query: String): Flow<List<TaskEntity>>

    // === Tags ===

    @Query("SELECT DISTINCT tags FROM tasks WHERE tags != '[]' AND tags != ''")
    suspend fun getAllUniqueTags(): List<String>

    // === Reminder ===

    @Query("SELECT * FROM tasks WHERE reminderDateTime <= :currentDateTime AND hasNotificationShown = 0 AND isCompleted = 0")
    suspend fun getTasksNeedingReminders(currentDateTime: String): List<TaskEntity>

    @Query("UPDATE tasks SET hasNotificationShown = 1 WHERE id = :taskId")
    suspend fun markReminderShown(taskId: Long)

    // === Wiederkehrende Tasks ===

    @Query("SELECT * FROM tasks WHERE recurrencePattern IS NOT NULL AND isCompleted = 1")
    suspend fun getCompletedRecurringTasks(): List<TaskEntity>
}