package de.beigel.list.repository

import de.beigel.list.data.*
import de.beigel.list.utils.TaskPriorityCalculator
import de.beigel.list.utils.UserContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class TaskRepository(
    private val taskDao: TaskDao,
    private val priorityCalculator: TaskPriorityCalculator = TaskPriorityCalculator()
) {

    // Basic CRUD Operations
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

    fun getAllActiveTasks(): Flow<List<TaskEntity>> {
        return taskDao.getAllActiveTasks()
    }

    suspend fun insertTask(task: TaskEntity): Result<TaskEntity> {
        return try {
            val validatedTask = task.copy(
                createdAt = LocalDateTime.now().toString(),
                lastModified = LocalDateTime.now().toString(),
                smartScore = priorityCalculator.calculateSmartScore(task),
                lastScoreUpdate = LocalDateTime.now().toString()
            )

            val result = taskDao.insertTaskWithValidation(validatedTask)
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTask(task: TaskEntity): Result<TaskEntity> {
        return try {
            val updatedTask = task.copy(
                lastModified = LocalDateTime.now().toString(),
                smartScore = priorityCalculator.calculateSmartScore(task),
                lastScoreUpdate = LocalDateTime.now().toString()
            )

            taskDao.updateTaskWithValidation(updatedTask)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    suspend fun toggleTaskCompletion(task: TaskEntity): TaskEntity {
        val now = LocalDateTime.now()
        val updatedTask = task.copy(
            isCompleted = !task.isCompleted,
            completedAt = if (!task.isCompleted) now.toString() else null,
            actualMinutes = if (!task.isCompleted && task.estimatedMinutes != null) {
                // Wenn keine tatsächliche Zeit gemessen wurde, nimm die Schätzung
                task.actualMinutes ?: task.estimatedMinutes
            } else if (task.isCompleted) {
                null
            } else {
                task.actualMinutes
            },
            lastModified = now.toString()
        )

        taskDao.updateTask(updatedTask)

        // Wenn es ein wiederkehrender Task ist und abgeschlossen wurde, erstelle nächste Instanz
        if (updatedTask.isCompleted && updatedTask.isRecurring && updatedTask.recurrencePattern != null) {
            createNextRecurringTask(updatedTask)
        }

        return updatedTask
    }

    private suspend fun createNextRecurringTask(completedTask: TaskEntity) {
        try {
            val pattern = completedTask.recurrencePattern ?: return
            val currentDate = LocalDate.parse(completedTask.date)
            val nextDate = when (pattern) {
                RecurrencePattern.DAILY -> currentDate.plusDays(1)
                RecurrencePattern.WEEKLY -> currentDate.plusWeeks(1)
                RecurrencePattern.BIWEEKLY -> currentDate.plusWeeks(2)
                RecurrencePattern.MONTHLY -> currentDate.plusMonths(1)
                RecurrencePattern.QUARTERLY -> currentDate.plusMonths(3)
                RecurrencePattern.YEARLY -> currentDate.plusYears(1)
            }

            val nextTask = completedTask.copy(
                id = java.util.UUID.randomUUID().toString(),
                isCompleted = false,
                completedAt = null,
                actualMinutes = null,
                date = nextDate.toString(),
                createdAt = LocalDateTime.now().toString(),
                lastModified = LocalDateTime.now().toString(),
                smartScore = 0.0,
                lastScoreUpdate = LocalDateTime.now().toString()
            )

            taskDao.insertTask(nextTask)
        } catch (e: Exception) {
            // Log error but don't fail the main operation
            android.util.Log.w("TaskRepository", "Failed to create recurring task", e)
        }
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

    // Smart Task Management
    suspend fun updateAllSmartScores(userContext: UserContext? = null) {
        try {
            val allTasks = taskDao.getAllActiveTasks().first()
            val updates = allTasks.map { task ->
                val score = priorityCalculator.calculateSmartScore(task, LocalDateTime.now(), userContext)
                SmartScoreUpdate(
                    taskId = task.id,
                    score = score,
                    updateTime = LocalDateTime.now().toString()
                )
            }

            if (updates.isNotEmpty()) {
                taskDao.updateMultipleSmartScores(updates)
            }
        } catch (e: Exception) {
            android.util.Log.e("TaskRepository", "Failed to update smart scores", e)
        }
    }

    fun getSmartFocusTasks(userContext: UserContext? = null, limit: Int = 5): Flow<List<TaskEntity>> {
        return taskDao.getAllActiveTasks()
            .map { tasks ->
                tasks
                    .map { task ->
                        val score = priorityCalculator.calculateSmartScore(task, LocalDateTime.now(), userContext)
                        task to score
                    }
                    .sortedByDescending { it.second }
                    .take(limit)
                    .map { it.first }
            }
    }

    fun getTasksByContext(context: TaskContext): Flow<List<TaskEntity>> {
        return taskDao.getTasksByContext(context.name)
    }

    fun getOverdueTasks(): Flow<List<TaskEntity>> {
        return taskDao.getOverdueTasks(LocalDate.now().toString())
    }

    fun getTodaysDueTasks(): Flow<List<TaskEntity>> {
        return taskDao.getTasksDueToday(LocalDate.now().toString())
    }

    fun getQuickWinTasks(maxMinutes: Int = 15): Flow<List<TaskEntity>> {
        return taskDao.getQuickWinTasks(maxMinutes)
    }

    fun getTasksByEnergyLevel(energyLevel: EnergyLevel): Flow<List<TaskEntity>> {
        return taskDao.getTasksByEnergyLevel(energyLevel.name)
    }

    fun getTasksByPriority(priority: TaskPriority): Flow<List<TaskEntity>> {
        return taskDao.getTasksByPriority(priority.name)
    }

    fun searchTasks(query: String): Flow<List<TaskEntity>> {
        return if (query.isBlank()) {
            taskDao.getAllActiveTasks()
        } else {
            taskDao.searchActiveTasks(query)
        }
    }

    // Analytics and Statistics
    suspend fun getProductivityStats(days: Int = 7): ProductivityStats {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(days.toLong())

        val totalCompleted = taskDao.getCompletedTasksInRange(startDate.toString(), endDate.toString())
        val averageTime = taskDao.getAverageCompletionTime(startDate.toString())
        val contextStats = taskDao.getCompletionStatsByContext(startDate.toString())
        val priorityStats = taskDao.getCompletionStatsByPriority(startDate.toString())

        // Calculate streak
        val streak = calculateCurrentStreak()

        // Calculate best day
        val dailyStats = mutableListOf<DailyStats>()
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            val stats = taskDao.getDailyStats(currentDate.toString())
            dailyStats.add(stats)
            currentDate = currentDate.plusDays(1)
        }

        val bestDay = dailyStats.maxByOrNull { it.completionRate }

        return ProductivityStats(
            totalCompletedTasks = totalCompleted,
            averageCompletionTimeMinutes = averageTime,
            averageDailyCompletion = totalCompleted.toDouble() / days,
            currentStreak = streak,
            bestDay = bestDay,
            topContext = contextStats.firstOrNull()?.context,
            topPriority = priorityStats.firstOrNull()?.priority,
            dailyBreakdown = dailyStats
        )
    }

    private suspend fun calculateCurrentStreak(): Int {
        var streak = 0
        var currentDate = LocalDate.now()

        while (true) {
            val completedCount = taskDao.getCompletedTasksCount(currentDate.toString())
            if (completedCount > 0) {
                streak++
                currentDate = currentDate.minusDays(1)
            } else {
                break
            }

            // Limit streak calculation to prevent infinite loops
            if (streak > 365) break
        }

        return streak
    }

    suspend fun getTaskEstimationAccuracy(): TaskEstimationAccuracy {
        val completedTasks = taskDao.getAllActiveTasks().first()
            .filter { it.isCompleted && it.estimatedMinutes != null && it.actualMinutes != null }

        if (completedTasks.isEmpty()) {
            return TaskEstimationAccuracy(0, 0.0, 0.0, emptyList())
        }

        val accuracyData = completedTasks.map { task ->
            val estimated = task.estimatedMinutes!!.toDouble()
            val actual = task.actualMinutes!!.toDouble()
            val accuracy = 1.0 - (kotlin.math.abs(estimated - actual) / estimated)
            EstimationAccuracyData(task, estimated, actual, accuracy)
        }

        val averageAccuracy = accuracyData.map { it.accuracy }.average()
        val averageOverestimation = accuracyData
            .filter { it.actual < it.estimated }
            .map { (it.estimated - it.actual) / it.estimated }
            .average()

        return TaskEstimationAccuracy(
            totalTasks = completedTasks.size,
            averageAccuracy = averageAccuracy,
            averageOverestimation = averageOverestimation,
            detailedData = accuracyData
        )
    }

    // Tag Management
    suspend fun getAllTags(): List<String> {
        return taskDao.getAllActiveTags()
            .flatMap { tagsString ->
                tagsString.split(",").map { it.trim() }
            }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    fun getTasksByTag(tag: String): Flow<List<TaskEntity>> {
        return taskDao.getTasksByTag(tag)
    }

    suspend fun getPopularTags(limit: Int = 10): List<TagUsage> {
        val allTags = getAllTags()
        val tagCounts = mutableMapOf<String, Int>()

        allTags.forEach { tag ->
            val count = taskDao.getTasksByTag(tag).first().size
            tagCounts[tag] = count
        }

        return tagCounts.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { TagUsage(it.key, it.value) }
    }

    // Batch Operations
    suspend fun bulkUpdateTasks(updates: List<Pair<String, Map<String, Any>>>): List<Result<TaskEntity>> {
        val results = mutableListOf<Result<TaskEntity>>()

        updates.forEach { (taskId, updateData) ->
            try {
                val currentTask = taskDao.getAllActiveTasks().first().find { it.id == taskId }
                if (currentTask != null) {
                    val updatedTask = applyUpdates(currentTask, updateData)
                    val result = updateTask(updatedTask)
                    results.add(result)
                } else {
                    results.add(Result.failure(NoSuchElementException("Task with ID $taskId not found")))
                }
            } catch (e: Exception) {
                results.add(Result.failure(e))
            }
        }

        return results
    }

    private fun applyUpdates(task: TaskEntity, updates: Map<String, Any>): TaskEntity {
        var updatedTask = task

        updates.forEach { (field, value) ->
            updatedTask = when (field) {
                "title" -> updatedTask.copy(title = value as String)
                "description" -> updatedTask.copy(description = value as String)
                "priority" -> updatedTask.copy(priority = value as TaskPriority)
                "dueDate" -> updatedTask.copy(dueDate = value as String?)
                "estimatedMinutes" -> updatedTask.copy(estimatedMinutes = value as Int?)
                "energyLevel" -> updatedTask.copy(energyLevel = value as EnergyLevel)
                "context" -> updatedTask.copy(context = value as TaskContext?)
                "tags" -> updatedTask.copy(tags = value as String)
                "isCompleted" -> updatedTask.copy(isCompleted = value as Boolean)
                else -> updatedTask
            }
        }

        return updatedTask
    }

    // Import/Export
    suspend fun exportTasks(includeCompleted: Boolean = false): String {
        val tasks = if (includeCompleted) {
            taskDao.getTasksForDateRange(
                LocalDate.now().minusMonths(3).toString(),
                LocalDate.now().toString()
            ).first()
        } else {
            taskDao.getAllActiveTasks().first()
        }

        return DatabaseUtils.exportTasksToJson(taskDao)
    }

    suspend fun importTasks(jsonData: String): Result<Int> {
        return try {
            // Simplified JSON parsing - in real app use Gson/Moshi
            val importedCount = 0 // Would parse and import here
            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Maintenance
    suspend fun cleanupOldTasks(daysToKeep: Int = 30) {
        DatabaseUtils.cleanupOldData(taskDao, daysToKeep)
    }

    suspend fun optimizeDatabase() {
        try {
            // Reset outdated smart scores
            DatabaseUtils.resetAllSmartScores(taskDao)

            // Update all active smart scores
            updateAllSmartScores()

        } catch (e: Exception) {
            android.util.Log.e("TaskRepository", "Database optimization failed", e)
        }
    }

    // Helper function to get task insights
    suspend fun getTaskInsights(taskId: String): TaskInsights? {
        val task = taskDao.getAllActiveTasks().first().find { it.id == taskId }
            ?: return null

        val reasoning = priorityCalculator.generateTaskReasoning(task, task.smartScore)
        val similarTasks = findSimilarTasks(task)
        val estimationHistory = getEstimationHistoryForSimilarTasks(task)

        return TaskInsights(
            task = task,
            reasoning = reasoning,
            similarTasks = similarTasks,
            estimationHistory = estimationHistory,
            recommendations = generateTaskRecommendations(task)
        )
    }

    private suspend fun findSimilarTasks(task: TaskEntity, limit: Int = 5): List<TaskEntity> {
        val allTasks = taskDao.getAllActiveTasks().first()

        return allTasks
            .filter { it.id != task.id }
            .filter { otherTask ->
                // Similar if same context, priority, or similar estimated time
                otherTask.context == task.context ||
                        otherTask.priority == task.priority ||
                        (otherTask.estimatedMinutes != null && task.estimatedMinutes != null &&
                                kotlin.math.abs(otherTask.estimatedMinutes!! - task.estimatedMinutes!!) <= 15)
            }
            .take(limit)
    }

    private suspend fun getEstimationHistoryForSimilarTasks(task: TaskEntity): List<EstimationAccuracyData> {
        val similarTasks = findSimilarTasks(task)
        return similarTasks
            .filter { it.isCompleted && it.estimatedMinutes != null && it.actualMinutes != null }
            .map { completedTask ->
                val estimated = completedTask.estimatedMinutes!!.toDouble()
                val actual = completedTask.actualMinutes!!.toDouble()
                val accuracy = 1.0 - (kotlin.math.abs(estimated - actual) / estimated)
                EstimationAccuracyData(completedTask, estimated, actual, accuracy)
            }
    }

    private fun generateTaskRecommendations(task: TaskEntity): List<String> {
        val recommendations = mutableListOf<String>()

        // Due date recommendations
        task.dueDate?.let { dueDate ->
            val days = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(dueDate))
            when {
                days < 0 -> recommendations.add("⚠️ Diese Aufgabe ist überfällig - höchste Priorität!")
                days == 0L -> recommendations.add("🔥 Heute fällig - sollte sofort erledigt werden")
                days == 1L -> recommendations.add("⏰ Morgen fällig - heute vorbereiten")
            }
        }

        // Time estimation recommendations
        task.estimatedMinutes?.let { minutes ->
            when {
                minutes <= 15 -> recommendations.add("⚡ Perfect für eine kurze Pause")
                minutes > 120 -> recommendations.add("🎯 Große Aufgabe - in kleinere Teile aufteilen?")
            }
        }

        // Energy level recommendations
        val currentHour = LocalDateTime.now().hour
        when (task.energyLevel) {
            EnergyLevel.HIGH -> {
                if (currentHour in 14..16) {
                    recommendations.add("💪 Perfekte Zeit für anspruchsvolle Aufgaben!")
                }
            }
            EnergyLevel.LOW -> {
                if (currentHour >= 18) {
                    recommendations.add("🌙 Ideal für den Abend")
                }
            }
            else -> {}
        }

        return recommendations
    }
}

// Data classes for analytics
data class ProductivityStats(
    val totalCompletedTasks: Int,
    val averageCompletionTimeMinutes: Double?,
    val averageDailyCompletion: Double,
    val currentStreak: Int,
    val bestDay: DailyStats?,
    val topContext: String?,
    val topPriority: String?,
    val dailyBreakdown: List<DailyStats>
)

data class TaskEstimationAccuracy(
    val totalTasks: Int,
    val averageAccuracy: Double,
    val averageOverestimation: Double,
    val detailedData: List<EstimationAccuracyData>
)

data class EstimationAccuracyData(
    val task: TaskEntity,
    val estimated: Double,
    val actual: Double,
    val accuracy: Double
)

data class TagUsage(
    val tag: String,
    val usageCount: Int
)

data class TaskInsights(
    val task: TaskEntity,
    val reasoning: de.beigel.list.utils.TaskReasoning,
    val similarTasks: List<TaskEntity>,
    val estimationHistory: List<EstimationAccuracyData>,
    val recommendations: List<String>
)