package de.beigel.list.repository

import de.beigel.list.data.*
import de.beigel.list.settings.SettingsManager
import de.beigel.list.utils.TaskPriorityCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val settingsManager: SettingsManager,
    private val priorityCalculator: TaskPriorityCalculator
) {

    // === Core Task Operations ===

    suspend fun createTask(
        title: String,
        description: String = "",
        priority: TaskPriority = TaskPriority.MEDIUM,
        dueDate: LocalDate? = null,
        estimatedMinutes: Int? = null,
        energyLevel: EnergyLevel = EnergyLevel.MEDIUM,
        context: TaskContext = TaskContext.NONE,
        tags: List<String> = emptyList(),
        recurrencePattern: RecurrencePattern? = null,
        reminderDateTime: LocalDateTime? = null
    ): Long {
        val task = TaskEntity(
            title = title.trim(),
            description = description.trim(),
            priority = priority,
            dueDate = dueDate,
            estimatedMinutes = estimatedMinutes,
            energyLevel = energyLevel,
            context = context,
            tags = tags.map { it.trim() }.filter { it.isNotEmpty() },
            recurrencePattern = recurrencePattern,
            reminderDateTime = reminderDateTime,
            smartScore = priorityCalculator.calculateInitialScore(
                priority = priority,
                dueDate = dueDate,
                estimatedMinutes = estimatedMinutes,
                energyLevel = energyLevel
            )
        )

        val taskId = taskDao.insertTask(task)

        // Prüfe ob die heutige Liste aufgefüllt werden kann
        refreshTodayListIfNeeded()

        return taskId
    }

    suspend fun updateTask(task: TaskEntity) {
        val updatedTask = task.copy(
            updatedAt = LocalDateTime.now(),
            smartScore = priorityCalculator.calculateScore(task)
        )
        taskDao.updateTask(updatedTask)

        // Aktualisiere Smart Scores aller Tasks
        refreshSmartScores()
        refreshTodayListIfNeeded()
    }

    suspend fun deleteTask(taskId: Long) {
        taskDao.deleteTaskById(taskId)
        refreshTodayListIfNeeded()
    }

    suspend fun toggleTaskCompleted(taskId: Long): Boolean {
        val task = taskDao.getTaskById(taskId) ?: return false

        val now = LocalDateTime.now()
        val isCompleted = !task.isCompleted

        val updatedTask = task.copy(
            isCompleted = isCompleted,
            completedAt = if (isCompleted) now else null,
            updatedAt = now
        )

        taskDao.updateTask(updatedTask)

        // Wenn Task completed wurde und wiederkehrend ist, erstelle neuen Task
        if (isCompleted && task.recurrencePattern != null) {
            createRecurringTask(task)
        }

        // Aktualisiere die heutige Liste
        refreshTodayListIfNeeded()

        return isCompleted
    }

    // === Smart Focus System ===

    suspend fun refreshTodayListIfNeeded() {
        val currentTodayCount = taskDao.getTodayTaskCount()
        val dailyLimit = settingsManager.dailyTaskLimit.first()
        val smartFocusEnabled = settingsManager.smartFocusEnabled.first()

        if (!smartFocusEnabled) return

        if (currentTodayCount < dailyLimit) {
            val needed = dailyLimit - currentTodayCount
            val candidates = taskDao.getTopBacklogTasksForToday(needed)

            candidates.forEach { task ->
                val updatedTask = task.copy(
                    isInTodayList = true,
                    lastShownInTodayList = LocalDate.now(),
                    updatedAt = LocalDateTime.now()
                )
                taskDao.updateTask(updatedTask)
            }
        }
    }

    suspend fun refreshSmartScores() {
        val smartScoringEnabled = settingsManager.smartScoringEnabled.first()
        if (!smartScoringEnabled) return

        val allTasks = taskDao.getAllTasks().first()
        val now = LocalDateTime.now()

        allTasks.filter { !it.isCompleted }.forEach { task ->
            val newScore = priorityCalculator.calculateScore(task)
            if (newScore != task.smartScore) {
                taskDao.updateSmartScore(
                    taskId = task.id,
                    score = newScore,
                    updatedAt = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
            }
        }
    }

    suspend fun performMidnightReset() {
        val today = LocalDate.now()
        val lastResetDate = settingsManager.lastResetDate.first()

        // Prüfe ob bereits heute zurückgesetzt wurde
        if (lastResetDate == today.format(DateTimeFormatter.ISO_LOCAL_DATE)) {
            return
        }

        // Verschiebe nicht erledigte Tasks zurück ins Backlog
        val now = LocalDateTime.now()
        taskDao.clearTodayList(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))

        // Aktualisiere Smart Scores
        refreshSmartScores()

        // Fülle die neue heutige Liste
        refreshTodayListIfNeeded()

        // Setze Reset-Datum
        settingsManager.setLastResetDate(today.format(DateTimeFormatter.ISO_LOCAL_DATE))
    }

    // === Data Access ===

    fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()

    fun getTodayTasks(): Flow<List<TaskEntity>> = taskDao.getTodayTasks()

    fun getTodayTasksWithCompleted(): Flow<List<TaskEntity>> = taskDao.getTodayTasksWithCompleted()

    fun getBacklogTasks(): Flow<List<TaskEntity>> = taskDao.getBacklogTasks()

    fun getOverdueTasks(): Flow<List<TaskEntity>> = taskDao.getOverdueTasks(LocalDate.now())

    fun getQuickWinTasks(): Flow<List<TaskEntity>> = taskDao.getQuickWinTasks()

    fun getTasksByPriority(priority: TaskPriority): Flow<List<TaskEntity>> =
        taskDao.getTasksByPriority(priority)

    fun getTasksByContext(context: TaskContext): Flow<List<TaskEntity>> =
        taskDao.getTasksByContext(context)

    fun getTasksByEnergyLevel(energyLevel: EnergyLevel): Flow<List<TaskEntity>> =
        taskDao.getTasksByEnergyLevel(energyLevel)

    fun searchTasks(query: String): Flow<List<TaskEntity>> = taskDao.searchTasks(query)

    suspend fun getTaskById(id: Long): TaskEntity? = taskDao.getTaskById(id)

    // === Statistics ===

    suspend fun getTodayProgress(): TaskProgress {
        val completed = taskDao.getCompletedTodayTaskCount()
        val total = completed + taskDao.getTodayTaskCount()

        return TaskProgress(
            completed = completed,
            total = total,
            percentage = if (total > 0) (completed * 100) / total else 0
        )
    }

    suspend fun getBacklogCount(): Int = taskDao.getBacklogTaskCount()

    suspend fun getOverdueCount(): Int = taskDao.getOverdueTaskCount(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))

    suspend fun getCompletedTasksForDate(date: LocalDate): Int {
        return taskDao.getCompletedTasksCountForDate(
            date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
    }

    suspend fun getWeeklyCompletedTasks(): Int {
        val today = LocalDate.now()
        val weekStart = today.minusDays(6)

        return taskDao.getCompletedTasksCountBetween(
            weekStart.format(DateTimeFormatter.ISO_LOCAL_DATE),
            today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
    }

    // === Helper Methods ===

    private suspend fun createRecurringTask(originalTask: TaskEntity) {
        val newDueDate = when (originalTask.recurrencePattern) {
            RecurrencePattern.DAILY -> originalTask.dueDate?.plusDays(1)
            RecurrencePattern.WEEKLY -> originalTask.dueDate?.plusWeeks(1)
            RecurrencePattern.MONTHLY -> originalTask.dueDate?.plusMonths(1)
            null -> null
        }

        val newTask = originalTask.copy(
            id = 0, // Room generiert neue ID
            isCompleted = false,
            isInTodayList = false,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            completedAt = null,
            dueDate = newDueDate,
            parentTaskId = originalTask.id,
            lastShownInTodayList = null,
            hasNotificationShown = false,
            reminderDateTime = originalTask.reminderDateTime?.let { reminder ->
                newDueDate?.let { due ->
                    val reminderOffset = java.time.Duration.between(
                        originalTask.dueDate?.atStartOfDay(),
                        reminder
                    )
                    due.atStartOfDay().plus(reminderOffset)
                }
            }
        )

        taskDao.insertTask(newTask)
    }

    // === Tags Management ===

    suspend fun getAllUniqueTags(): List<String> {
        return taskDao.getAllUniqueTags()
            .flatMap { tagsString ->
                try {
                    // Parse das JSON Array von Tags
                    kotlinx.serialization.json.Json.decodeFromString<List<String>>(tagsString)
                } catch (e: Exception) {
                    emptyList()
                }
            }
            .distinct()
            .sorted()
    }

    // === Contextual Recommendations ===

    suspend fun getContextualRecommendations(): ContextualRecommendations {
        val currentHour = LocalDateTime.now().hour
        val workingHours = getWorkingHours()
        val isWorkingTime = currentHour in workingHours.first..workingHours.second

        val recommendedTasks = when {
            isWorkingTime -> getTasksByContext(TaskContext.WORK).first().take(3)
            currentHour < 10 -> getTasksByEnergyLevel(EnergyLevel.HIGH).first().take(3)
            currentHour > 18 -> getTasksByContext(TaskContext.HOME).first().take(3)
            else -> getQuickWinTasks().first().take(3)
        }

        return ContextualRecommendations(
            recommendedTasks = recommendedTasks,
            context = when {
                isWorkingTime -> "Es ist Arbeitszeit - fokussiere dich auf berufliche Aufgaben"
                currentHour < 10 -> "Der Morgen ist ideal für anspruchsvolle Aufgaben"
                currentHour > 18 -> "Abends eignen sich Haushalts- und persönliche Aufgaben"
                else -> "Perfekte Zeit für Quick Wins"
            }
        )
    }

    private suspend fun getWorkingHours(): Pair<Int, Int> {
        val startHour = settingsManager.workStartHour.first()
        val endHour = settingsManager.workEndHour.first()
        return Pair(startHour, endHour)
    }
}

data class TaskProgress(
    val completed: Int,
    val total: Int,
    val percentage: Int
)

data class ContextualRecommendations(
    val recommendedTasks: List<TaskEntity>,
    val context: String
)