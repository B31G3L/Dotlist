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
}