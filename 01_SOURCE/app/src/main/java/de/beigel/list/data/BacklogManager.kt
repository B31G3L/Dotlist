package de.beigel.list.data

import de.beigel.list.repository.TaskRepository
import de.beigel.list.settings.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class BacklogManager(
    private val repository: TaskRepository,
    private val settingsManager: SettingsManager
) {

    /**
     * Prüft ob das tägliche Limit erreicht wurde
     */
    suspend fun isDailyLimitReached(): Boolean {
        return withContext(Dispatchers.IO) {
            val todayTasks = repository.getDailyTasksForToday().first()
            todayTasks.size >= settingsManager.maxDailyTasks
        }
    }

    /**
     * Fügt eine Aufgabe automatisch zur passenden Liste hinzu
     */
    suspend fun addTaskSmartly(task: TaskEntity): TaskEntity {
        return withContext(Dispatchers.IO) {
            val shouldAddToDaily = !isDailyLimitReached() || settingsManager.maxDailyTasks <= 0
            if (shouldAddToDaily) {
                repository.insertTask(task, addToDaily = true)
                task.copy(isInDailyList = true)
            } else {
                repository.insertTask(task, addToDaily = false)
                task.copy(isInDailyList = false)
            }
        }
    }

    /**
     * Füllt die tägliche Liste optimal aus dem Backlog auf
     */
    suspend fun optimizeDaily() {
        withContext(Dispatchers.IO) {
            if (settingsManager.autoBacklogEnabled) {
                repository.fillDailyListFromBacklog(settingsManager.maxDailyTasks)
            }
        }
    }

    /**
     * Verschiebt überschüssige Aufgaben ins Backlog
     */
    suspend fun enforceLimit() {
        withContext(Dispatchers.IO) {
            val todayTasks = repository.getDailyTasksForToday().first()
            val maxTasks = settingsManager.maxDailyTasks

            if (todayTasks.size > maxTasks && maxTasks > 0) {
                // Die letzten (neuesten) Aufgaben ins Backlog verschieben
                val excessTasks = todayTasks.sortedBy { it.position }.drop(maxTasks)
                excessTasks.forEach { task ->
                    repository.moveTaskToBacklog(task)
                }
            }
        }
    }

    /**
     * Analysiert die Backlog-Performance
     */
    suspend fun getBacklogStats(): BacklogStats {
        return withContext(Dispatchers.IO) {
            val backlogTasks = repository.getBacklogTasks().first()
            val todayTasks = repository.getDailyTasksForToday().first()

            BacklogStats(
                totalBacklogTasks = backlogTasks.size,
                completedBacklogTasks = backlogTasks.count { it.isCompleted },
                todayTasksCount = todayTasks.size,
                maxDailyLimit = settingsManager.maxDailyTasks,
                isAutoBacklogEnabled = settingsManager.autoBacklogEnabled
            )
        }
    }

    /**
     * Empfiehlt optimale Einstellungen basierend auf Nutzungsverhalten
     */
    suspend fun getOptimizationSuggestions(): List<OptimizationSuggestion> {
        return withContext(Dispatchers.IO) {
            val suggestions = mutableListOf<OptimizationSuggestion>()
            val stats = getBacklogStats()

            // Zu viele Tasks im Backlog
            if (stats.totalBacklogTasks > 20) {
                suggestions.add(
                    OptimizationSuggestion(
                        type = SuggestionType.INCREASE_DAILY_LIMIT,
                        title = "Tägliches Limit erhöhen",
                        description = "Du hast viele Aufgaben im Backlog. Erwäge das tägliche Limit zu erhöhen.",
                        recommendedValue = minOf(stats.maxDailyLimit + 2, 10)
                    )
                )
            }

            // Zu wenige Tasks im Daily
            if (stats.todayTasksCount < stats.maxDailyLimit * 0.7) {
                suggestions.add(
                    OptimizationSuggestion(
                        type = SuggestionType.ENABLE_AUTO_BACKLOG,
                        title = "Auto-Backlog aktivieren",
                        description = "Lass das System automatisch Aufgaben aus dem Backlog hinzufügen."
                    )
                )
            }

            suggestions
        }
    }
}

data class BacklogStats(
    val totalBacklogTasks: Int,
    val completedBacklogTasks: Int,
    val todayTasksCount: Int,
    val maxDailyLimit: Int,
    val isAutoBacklogEnabled: Boolean
)

data class OptimizationSuggestion(
    val type: SuggestionType,
    val title: String,
    val description: String,
    val recommendedValue: Int? = null
)

enum class SuggestionType {
    INCREASE_DAILY_LIMIT,
    DECREASE_DAILY_LIMIT,
    ENABLE_AUTO_BACKLOG,
    DISABLE_AUTO_BACKLOG
}