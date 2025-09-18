package de.beigel.list.widget

import android.content.Context
import de.beigel.list.data.TaskDatabase
import de.beigel.list.data.TaskEntity
import de.beigel.list.data.TaskPriority
import de.beigel.list.repository.TaskRepository
import de.beigel.list.settings.SettingsManager
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Repository für Widget-Daten - Verbindet Widgets mit der App-Logik
 */
class WidgetRepository(private val context: Context) {

    private val database = TaskDatabase.getDatabase(context)
    private val taskRepository = TaskRepository(database.taskDao())
    private val settingsManager = SettingsManager(context)
    private val widgetPreferences = WidgetPreferences(context)

    /**
     * Holt die Daten für das Today Tasks Widget
     */
    suspend fun getTodayWidgetData(): TodayWidgetData {
        return try {
            val todayTasks = taskRepository.getDailyTasksForToday().first()
            val filteredTasks = if (widgetPreferences.showCompleted) {
                todayTasks
            } else {
                todayTasks.filter { !it.isCompleted }
            }

            val completedCount = todayTasks.count { it.isCompleted }
            val totalCount = todayTasks.size
            val progress = if (totalCount > 0) {
                completedCount.toFloat() / totalCount.toFloat()
            } else 0f

            TodayWidgetData(
                tasks = filteredTasks.take(widgetPreferences.maxItems),
                completedCount = completedCount,
                totalCount = totalCount,
                maxTasks = settingsManager.maxDailyTasks,
                progress = progress,
                isLoading = false,
                lastUpdated = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            TodayWidgetData(isLoading = false) // Fallback bei Fehlern
        }
    }

    /**
     * Fügt eine neue Aufgabe über das Widget hinzu
     */
    suspend fun addQuickTask(title: String, priority: TaskPriority, addToDaily: Boolean = true): Boolean {
        return try {
            val newTask = TaskEntity(
                title = title.trim(),
                description = "",
                priority = priority,
                date = LocalDate.now().toString(),
                createdAt = LocalDateTime.now().toString()
            )

            taskRepository.insertTask(newTask, addToDaily)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Togglet den Erledigungsstatus einer Aufgabe
     */
    suspend fun toggleTaskCompletion(taskId: String): Boolean {
        return try {
            val todayTasks = taskRepository.getDailyTasksForToday().first()
            val task = todayTasks.find { it.id == taskId }

            if (task != null) {
                val updatedTask = task.copy(
                    isCompleted = !task.isCompleted,
                    completedAt = if (!task.isCompleted) {
                        LocalDateTime.now().toString()
                    } else null
                )
                taskRepository.updateTask(updatedTask)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Holt Statistiken für das Stats Widget (für zukünftige Erweiterung)
     */
    suspend fun getWidgetStats(): WidgetStats {
        return try {
            val todayTasks = taskRepository.getDailyTasksForToday().first()
            val completedToday = todayTasks.count { it.isCompleted }
            val totalToday = todayTasks.size

            // Wöchliche Daten (vereinfacht)
            val weekTasks = mutableListOf<TaskEntity>()
            for (i in 0..6) {
                val date = LocalDate.now().minusDays(i.toLong())
                val dayTasks = taskRepository.getDailyTasksForDate(date).first()
                weekTasks.addAll(dayTasks)
            }

            val weeklyCompleted = weekTasks.count { it.isCompleted }
            val weeklyTotal = weekTasks.size
            val weeklyRate = if (weeklyTotal > 0) {
                weeklyCompleted.toFloat() / weeklyTotal.toFloat()
            } else 0f

            WidgetStats(
                todayCompleted = completedToday,
                todayTotal = totalToday,
                weeklyCompleted = weeklyCompleted,
                weeklyTotal = weeklyTotal,
                weeklyCompletionRate = weeklyRate,
                streakDays = calculateStreak()
            )
        } catch (e: Exception) {
            WidgetStats() // Fallback
        }
    }

    /**
     * Berechnet die aktuelle Streak (vereinfacht)
     */
    private suspend fun calculateStreak(): Int {
        return try {
            var streak = 0
            var currentDate = LocalDate.now()

            // Schaue die letzten 30 Tage an
            for (i in 0 until 30) {
                val dayTasks = taskRepository.getDailyTasksForDate(currentDate).first()
                val completionRate = if (dayTasks.isNotEmpty()) {
                    dayTasks.count { it.isCompleted }.toFloat() / dayTasks.size
                } else 0f

                if (completionRate >= 0.8f) { // 80% als Erfolg
                    streak++
                    currentDate = currentDate.minusDays(1)
                } else {
                    break
                }
            }

            streak
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Holt häufige Aufgaben basierend auf Verlauf
     */
    suspend fun getFrequentTasks(): List<String> {
        return try {
            val recentTasks = mutableListOf<TaskEntity>()

            // Sammle Aufgaben der letzten 14 Tage
            for (i in 0..13) {
                val date = LocalDate.now().minusDays(i.toLong())
                val dayTasks = taskRepository.getDailyTasksForDate(date).first()
                recentTasks.addAll(dayTasks)
            }

            // Analysiere häufige Titel (vereinfacht)
            val titleCounts = mutableMapOf<String, Int>()
            recentTasks.forEach { task ->
                val normalizedTitle = task.title.lowercase().trim()
                titleCounts[normalizedTitle] = titleCounts.getOrDefault(normalizedTitle, 0) + 1
            }

            titleCounts.entries
                .sortedByDescending { it.value }
                .take(6)
                .map { it.key.replaceFirstChar { char -> char.uppercase() } }

        } catch (e: Exception) {
            listOf(
                "E-Mails bearbeiten",
                "Einkaufen gehen",
                "Sport machen",
                "Wichtigen Anruf",
                "Termine planen",
                "Projekt bearbeiten"
            )
        }
    }
}

/**
 * Statistik-Daten für Widgets
 */
data class WidgetStats(
    val todayCompleted: Int = 0,
    val todayTotal: Int = 0,
    val weeklyCompleted: Int = 0,
    val weeklyTotal: Int = 0,
    val weeklyCompletionRate: Float = 0f,
    val streakDays: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val todayProgress: Float get() = if (todayTotal > 0) todayCompleted.toFloat() / todayTotal else 0f
}