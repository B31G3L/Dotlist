package de.beigel.list.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TaskDao {
    // Bestehende Queries
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

    // Neue Smart Focus Queries
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY smartScore DESC, lastModified DESC")
    fun getAllActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND context = :context ORDER BY smartScore DESC")
    fun getTasksByContext(context: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND dueDate = :date ORDER BY smartScore DESC")
    fun getTasksDueToday(date: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND dueDate IS NOT NULL AND dueDate <= :maxDate ORDER BY dueDate ASC, smartScore DESC")
    fun getUpcomingTasks(maxDate: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND dueDate IS NOT NULL AND dueDate < :today ORDER BY dueDate ASC")
    fun getOverdueTasks(today: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND estimatedMinutes IS NOT NULL AND estimatedMinutes <= :maxMinutes ORDER BY smartScore DESC")
    fun getQuickWinTasks(maxMinutes: Int = 15): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND energyLevel = :energyLevel ORDER BY smartScore DESC")
    fun getTasksByEnergyLevel(energyLevel: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND priority = :priority ORDER BY smartScore DESC")
    fun getTasksByPriority(priority: String): Flow<List<TaskEntity>>

    // Batch-Updates für Smart Scoring
    @Query("UPDATE tasks SET smartScore = :score, lastScoreUpdate = :updateTime WHERE id = :taskId")
    suspend fun updateSmartScore(taskId: String, score: Double, updateTime: String)

    @Transaction
    suspend fun updateMultipleSmartScores(updates: List<SmartScoreUpdate>) {
        updates.forEach { update ->
            updateSmartScore(update.taskId, update.score, update.updateTime)
        }
    }

    // Such-Queries
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') ORDER BY smartScore DESC")
    fun searchActiveTasks(query: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') ORDER BY lastModified DESC")
    fun searchAllTasks(query: String): Flow<List<TaskEntity>>

    // Tag-Management
    @Query("SELECT DISTINCT tags FROM tasks WHERE tags != '' AND isCompleted = 0")
    suspend fun getAllActiveTags(): List<String>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND tags LIKE '%' || :tag || '%' ORDER BY smartScore DESC")
    fun getTasksByTag(tag: String): Flow<List<TaskEntity>>

    // Statistiken für Analytics
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1 AND date >= :startDate AND date <= :endDate")
    suspend fun getCompletedTasksInRange(startDate: String, endDate: String): Int

    @Query("SELECT AVG(actualMinutes) FROM tasks WHERE isCompleted = 1 AND actualMinutes IS NOT NULL AND date >= :startDate")
    suspend fun getAverageCompletionTime(startDate: String): Double?

    @Query("SELECT context, COUNT(*) as count FROM tasks WHERE isCompleted = 1 AND date >= :startDate GROUP BY context ORDER BY count DESC")
    suspend fun getCompletionStatsByContext(startDate: String): List<ContextStats>

    @Query("SELECT priority, COUNT(*) as count FROM tasks WHERE isCompleted = 1 AND date >= :startDate GROUP BY priority ORDER BY count DESC")
    suspend fun getCompletionStatsByPriority(startDate: String): List<PriorityStats>

    // Recurring Tasks
    @Query("SELECT * FROM tasks WHERE isRecurring = 1 AND isCompleted = 1 AND date = :date")
    suspend fun getCompletedRecurringTasks(date: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE isRecurring = 1 AND isCompleted = 0")
    fun getActiveRecurringTasks(): Flow<List<TaskEntity>>

    // Performance-optimierte Queries
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0")
    suspend fun getActiveTasksCount(): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0 AND dueDate IS NOT NULL AND dueDate <= :today")
    suspend fun getUrgentTasksCount(today: String): Int

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY smartScore DESC LIMIT :limit")
    suspend fun getTopTasks(limit: Int): List<TaskEntity>

    // Cleanup-Queries
    @Query("DELETE FROM tasks WHERE isCompleted = 1 AND date < :cutoffDate")
    suspend fun deleteOldCompletedTasks(cutoffDate: String)

    @Query("UPDATE tasks SET smartScore = 0.0 WHERE lastScoreUpdate < :cutoffTime")
    suspend fun resetOutdatedSmartScores(cutoffTime: String)
}

// Datenklassen für Query-Ergebnisse
data class SmartScoreUpdate(
    val taskId: String,
    val score: Double,
    val updateTime: String
)

data class ContextStats(
    val context: String?,
    val count: Int
)

data class PriorityStats(
    val priority: String,
    val count: Int
)

data class DailyStats(
    val date: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val averageCompletionTime: Double?,
    val completionRate: Float
) {
    fun getCompletionPercentage(): Int = (completionRate * 100).toInt()
}

// Extension Functions für TaskDao
suspend fun TaskDao.getDailyStats(date: String): DailyStats {
    val total = getTotalTasksCount(date)
    val completed = getCompletedTasksCount(date)
    val averageTime = getAverageCompletionTime(date)
    val rate = if (total > 0) completed.toFloat() / total.toFloat() else 0f

    return DailyStats(
        date = date,
        totalTasks = total,
        completedTasks = completed,
        averageCompletionTime = averageTime,
        completionRate = rate
    )
}

suspend fun TaskDao.getProductivityMetrics(startDate: String, endDate: String): ProductivityMetrics {
    val totalCompleted = getCompletedTasksInRange(startDate, endDate)
    val averageTime = getAverageCompletionTime(startDate)
    val contextStats = getCompletionStatsByContext(startDate)
    val priorityStats = getCompletionStatsByPriority(startDate)

    return ProductivityMetrics(
        totalCompletedTasks = totalCompleted,
        averageCompletionTimeMinutes = averageTime,
        topContext = contextStats.firstOrNull()?.context,
        topPriority = priorityStats.firstOrNull()?.priority
    )
}

data class ProductivityMetrics(
    val totalCompletedTasks: Int,
    val averageCompletionTimeMinutes: Double?,
    val topContext: String?,
    val topPriority: String?
)