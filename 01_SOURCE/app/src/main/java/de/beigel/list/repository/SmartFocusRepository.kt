package de.beigel.list.repository

import de.beigel.list.data.*
import de.beigel.list.utils.TaskPriorityCalculator
import de.beigel.list.utils.UserContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime

class SmartFocusRepository(
    private val taskDao: TaskDao,
    private val calculator: TaskPriorityCalculator = TaskPriorityCalculator()
) {

    // Smart Focus - die wichtigsten 5 Aufgaben
    fun getSmartFocusTasks(userContext: UserContext? = null): Flow<List<TaskEntity>> {
        return taskDao.getAllActiveTasks()
            .map { allTasks ->
                calculator.getTop5Tasks(allTasks, userContext)
            }
    }

    // Aufgaben nach Kontext gefiltert
    fun getFocusTasksWithContext(context: TaskContext, userContext: UserContext? = null): Flow<List<TaskEntity>> {
        return taskDao.getTasksByContext(context.name)
            .map { contextTasks ->
                calculator.getTasksByContext(contextTasks, context, userContext)
                    .take(5)
            }
    }

    // Quick Wins - schnell erledigbare Aufgaben
    fun getQuickWins(maxCount: Int = 3): Flow<List<TaskEntity>> {
        return taskDao.getQuickWinTasks(15)
            .map { quickWinTasks ->
                calculator.getQuickWins(quickWinTasks, maxCount)
            }
    }

    // Überfällige Aufgaben
    fun getOverdueTasks(): Flow<List<TaskEntity>> {
        return taskDao.getOverdueTasks(LocalDate.now().toString())
            .map { overdueTasks ->
                overdueTasks.sortedByDescending { calculator.calculateSmartScore(it) }
            }
    }

    // Heute fällige Aufgaben
    fun getTodaysDueTasks(): Flow<List<TaskEntity>> {
        return taskDao.getTasksDueToday(LocalDate.now().toString())
            .map { dueTasks ->
                dueTasks.sortedByDescending { calculator.calculateSmartScore(it) }
            }
    }

    // Aufgaben nach Energielevel
    fun getTasksByEnergyLevel(energyLevel: EnergyLevel): Flow<List<TaskEntity>> {
        return taskDao.getTasksByEnergyLevel(energyLevel.name)
            .map { tasks ->
                tasks.sortedByDescending { calculator.calculateSmartScore(it) }
            }
    }

    // Suche in aktiven Aufgaben
    fun searchActiveTasks(query: String): Flow<List<TaskEntity>> {
        return if (query.isBlank()) {
            flowOf(emptyList())
        } else {
            taskDao.searchActiveTasks(query)
                .map { searchResults ->
                    searchResults.sortedByDescending { calculator.calculateSmartScore(it) }
                }
        }
    }

    // Aufgaben-Details mit Smart-Scoring
    suspend fun getTaskWithDetails(taskId: String): TaskEntity? {
        return taskDao.getAllActiveTasks().first()
            .find { it.id == taskId }
    }

    // Smart Score für eine einzelne Aufgabe berechnen
    suspend fun calculateTaskScore(task: TaskEntity, userContext: UserContext? = null): Double {
        return calculator.calculateSmartScore(task, LocalDateTime.now(), userContext)
    }

    // Begründung für Task-Priorität
    suspend fun getTaskReasoning(task: TaskEntity): de.beigel.list.utils.TaskReasoning {
        val score = calculator.calculateSmartScore(task)
        return calculator.generateTaskReasoning(task, score)
    }

    // Batch-Update der Smart Scores
    suspend fun updateAllSmartScores(userContext: UserContext? = null) {
        val allTasks = taskDao.getAllActiveTasks().first()
        val updates = allTasks.map { task ->
            val score = calculator.calculateSmartScore(task, LocalDateTime.now(), userContext)
            SmartScoreUpdate(
                taskId = task.id,
                score = score,
                updateTime = LocalDateTime.now().toString()
            )
        }

        taskDao.updateMultipleSmartScores(updates)
    }

    // Kontextuelle Empfehlungen
    suspend fun getContextualRecommendations(userContext: UserContext?): ContextualRecommendations {
        val allTasks = taskDao.getAllActiveTasks().first()
        val now = LocalDateTime.now()

        val quickWins = calculator.getQuickWins(allTasks, 3)
        val energyBasedTasks = getEnergyBasedRecommendations(allTasks, now, userContext)
        val urgentTasks = allTasks.filter { it.isOverdue() || it.isDueToday() }
            .sortedByDescending { calculator.calculateSmartScore(it, now, userContext) }
            .take(3)

        return ContextualRecommendations(
            quickWins = quickWins,
            energyBasedTasks = energyBasedTasks,
            urgentTasks = urgentTasks,
            recommendedAction = determineRecommendedAction(quickWins, urgentTasks, now)
        )
    }

    private fun getEnergyBasedRecommendations(
        allTasks: List<TaskEntity>,
        currentTime: LocalDateTime,
        userContext: UserContext?
    ): List<TaskEntity> {
        val currentEnergyLevel = getCurrentEnergyLevel(currentTime, userContext)

        return allTasks
            .filter { !it.isCompleted && it.energyLevel <= currentEnergyLevel }
            .sortedByDescending { calculator.calculateSmartScore(it, currentTime, userContext) }
            .take(3)
    }

    private fun getCurrentEnergyLevel(currentTime: LocalDateTime, userContext: UserContext?): EnergyLevel {
        val hour = currentTime.hour

        return userContext?.preferredEnergyTimes?.get(hour) ?: run {
            when (hour) {
                in 6..9 -> EnergyLevel.HIGH
                in 10..12 -> EnergyLevel.HIGH
                in 13..14 -> EnergyLevel.MEDIUM
                in 15..17 -> EnergyLevel.HIGH
                in 18..20 -> EnergyLevel.MEDIUM
                else -> EnergyLevel.LOW
            }
        }
    }

    private fun determineRecommendedAction(
        quickWins: List<TaskEntity>,
        urgentTasks: List<TaskEntity>,
        currentTime: LocalDateTime
    ): RecommendedAction {
        val hour = currentTime.hour

        return when {
            urgentTasks.isNotEmpty() -> RecommendedAction.FOCUS_ON_URGENT
            hour in 8..11 && quickWins.isNotEmpty() -> RecommendedAction.START_WITH_QUICK_WIN
            hour in 15..17 -> RecommendedAction.TACKLE_BIG_TASK
            hour >= 18 && quickWins.isNotEmpty() -> RecommendedAction.FINISH_WITH_QUICK_WIN
            else -> RecommendedAction.WORK_ON_TOP_PRIORITY
        }
    }

    // Produktivitäts-Insights
    suspend fun getProductivityInsights(days: Int = 7): ProductivityInsights {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(days.toLong())

        val dailyStats = mutableListOf<DailyStats>()
        var currentDate = startDate

        while (!currentDate.isAfter(endDate)) {
            val stats = taskDao.getDailyStats(currentDate.toString())
            dailyStats.add(stats)
            currentDate = currentDate.plusDays(1)
        }

        val totalCompleted = dailyStats.sumOf { it.completedTasks }
        val averageDaily = totalCompleted.toDouble() / days
        val bestDay = dailyStats.maxByOrNull { it.completionRate }
        val streak = calculateCurrentStreak(dailyStats)

        val contextStats = taskDao.getCompletionStatsByContext(startDate.toString())
        val priorityStats = taskDao.getCompletionStatsByPriority(startDate.toString())

        return ProductivityInsights(
            totalCompletedTasks = totalCompleted,
            averageDailyCompletion = averageDaily,
            bestDay = bestDay,
            currentStreak = streak,
            topContext = contextStats.firstOrNull()?.context,
            topPriority = priorityStats.firstOrNull()?.priority,
            dailyBreakdown = dailyStats
        )
    }

    private fun calculateCurrentStreak(dailyStats: List<DailyStats>): Int {
        var streak = 0

        // Von heute rückwärts zählen
        for (day in dailyStats.reversed()) {
            if (day.completedTasks > 0) {
                streak++
            } else {
                break
            }
        }

        return streak
    }

    // Tags verwalten
    suspend fun getAllActiveTags(): List<String> {
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
            .map { tasks ->
                tasks.sortedByDescending { calculator.calculateSmartScore(it) }
            }
    }

    // Maintenance-Funktionen
    suspend fun cleanupOldTasks(daysToKeep: Int = 30) {
        val cutoffDate = LocalDate.now().minusDays(daysToKeep.toLong()).toString()
        taskDao.deleteOldCompletedTasks(cutoffDate)
    }

    suspend fun resetOutdatedScores(hoursThreshold: Int = 6) {
        val cutoffTime = LocalDateTime.now().minusHours(hoursThreshold.toLong()).toString()
        taskDao.resetOutdatedSmartScores(cutoffTime)
    }

    // Background-Updates
    fun startPeriodicScoreUpdates(userContext: UserContext?): Flow<Unit> {
        return flow {
            while (true) {
                updateAllSmartScores(userContext)
                emit(Unit)
                delay(30 * 60 * 1000) // Alle 30 Minuten aktualisieren
            }
        }
    }
}

// Datenklassen für Repository-Ergebnisse
data class ContextualRecommendations(
    val quickWins: List<TaskEntity>,
    val energyBasedTasks: List<TaskEntity>,
    val urgentTasks: List<TaskEntity>,
    val recommendedAction: RecommendedAction
)

enum class RecommendedAction(val title: String, val description: String, val icon: String) {
    FOCUS_ON_URGENT("Dringend!", "Du hast überfällige oder heute fällige Aufgaben", "🚨"),
    START_WITH_QUICK_WIN("Quick Win", "Starte den Tag mit einem schnellen Erfolg", "⚡"),
    TACKLE_BIG_TASK("Große Aufgabe", "Jetzt ist die beste Zeit für wichtige Projekte", "🎯"),
    FINISH_WITH_QUICK_WIN("Aufräumen", "Schließe den Tag mit ein paar Quick Wins ab", "🧹"),
    WORK_ON_TOP_PRIORITY("Top Priorität", "Konzentriere dich auf deine wichtigste Aufgabe", "⭐")
}

data class ProductivityInsights(
    val totalCompletedTasks: Int,
    val averageDailyCompletion: Double,
    val bestDay: DailyStats?,
    val currentStreak: Int,
    val topContext: String?,
    val topPriority: String?,
    val dailyBreakdown: List<DailyStats>
) {
    fun getMotivationalMessage(): String {
        return when {
            currentStreak >= 7 -> "🔥 $currentStreak Tage am Stück! Du bist unstoppbar!"
            currentStreak >= 3 -> "💪 $currentStreak Tage Streak! Weiter so!"
            totalCompletedTasks >= 20 -> "🚀 $totalCompletedTasks Aufgaben in einer Woche? Wow!"
            averageDailyCompletion >= 3 -> "⭐ Durchschnittlich ${averageDailyCompletion.toInt()} Aufgaben pro Tag!"
            else -> "🌱 Jeder Schritt zählt! Weiter so!"
        }
    }

    fun getProductivityTrend(): ProductivityTrend {
        if (dailyBreakdown.size < 3) return ProductivityTrend.STABLE

        val recent = dailyBreakdown.takeLast(3).map { it.completedTasks }
        val earlier = dailyBreakdown.dropLast(3).takeLast(3).map { it.completedTasks }

        val recentAvg = recent.average()
        val earlierAvg = earlier.average()

        return when {
            recentAvg > earlierAvg * 1.2 -> ProductivityTrend.IMPROVING
            recentAvg < earlierAvg * 0.8 -> ProductivityTrend.DECLINING
            else -> ProductivityTrend.STABLE
        }
    }
}

enum class ProductivityTrend(val displayName: String, val icon: String, val color: Long) {
    IMPROVING("Steigend", "📈", 0xFF4CAF50),
    STABLE("Stabil", "➡️", 0xFF2196F3),
    DECLINING("Fallend", "📉", 0xFFFF9800)
}