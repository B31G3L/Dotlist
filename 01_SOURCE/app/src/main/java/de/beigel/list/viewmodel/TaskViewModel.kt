package de.beigel.list.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.beigel.list.data.*
import de.beigel.list.repository.TaskRepository
import de.beigel.list.repository.TaskProgress
import de.beigel.list.repository.ContextualRecommendations
import de.beigel.list.settings.SettingsManager
import de.beigel.list.utils.TaskPriorityCalculator
import de.beigel.list.utils.TaskReasoning
import de.beigel.list.utils.RecommendedAction
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

class TaskViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val settingsManager: SettingsManager,
    private val priorityCalculator: TaskPriorityCalculator
) : ViewModel() {

    // === UI State ===

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    private val _smartFocusState = MutableStateFlow(SmartFocusUiState())
    val smartFocusState: StateFlow<SmartFocusUiState> = _smartFocusState.asStateFlow()

    private val _dialogState = MutableStateFlow<DialogState?>(null)
    val dialogState: StateFlow<DialogState?> = _dialogState.asStateFlow()

    // === Data Flows ===

    val todayTasks: StateFlow<List<TaskEntity>> = repository.getTodayTasksWithCompleted()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val backlogTasks: StateFlow<List<TaskEntity>> = repository.getBacklogTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val overdueTasks: StateFlow<List<TaskEntity>> = repository.getOverdueTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val quickWinTasks: StateFlow<List<TaskEntity>> = repository.getQuickWinTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // === Settings ===

    val dailyTaskLimit: StateFlow<Int> = settingsManager.dailyTaskLimit
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 5
        )

    val smartFocusEnabled: StateFlow<Boolean> = settingsManager.smartFocusEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    init {
        loadInitialData()
        observeTaskChanges()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Lade Statistiken
                updateProgress()
                updateContextualRecommendations()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unbekannter Fehler"
                    )
                }
            }
        }
    }

    private fun observeTaskChanges() {
        viewModelScope.launch {
            // Überwache Task-Änderungen und aktualisiere UI
            combine(
                todayTasks,
                backlogTasks,
                overdueTasks
            ) { today, backlog, overdue ->
                Triple(today, backlog, overdue)
            }.collect { (today, backlog, overdue) ->
                updateProgress()
                updateSmartFocusState(today)

                _uiState.update { state ->
                    state.copy(
                        todayTasksCount = today.count { !it.isCompleted },
                        backlogCount = backlog.size,
                        overdueCount = overdue.size,
                        hasOverdueTasks = overdue.isNotEmpty()
                    )
                }
            }
        }
    }

    // === Task Operations ===

    fun createTask(
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
    ) {
        if (title.isBlank()) {
            _uiState.update { it.copy(error = "Titel darf nicht leer sein") }
            return
        }

        viewModelScope.launch {
            try {
                repository.createTask(
                    title = title,
                    description = description,
                    priority = priority,
                    dueDate = dueDate,
                    estimatedMinutes = estimatedMinutes,
                    energyLevel = energyLevel,
                    context = context,
                    tags = tags,
                    recurrencePattern = recurrencePattern,
                    reminderDateTime = reminderDateTime
                )

                _uiState.update { it.copy(successMessage = "Aufgabe erstellt") }

            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Fehler beim Erstellen: ${e.message}") }
            }
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            try {
                repository.updateTask(task)
                _uiState.update { it.copy(successMessage = "Aufgabe aktualisiert") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Fehler beim Aktualisieren: ${e.message}") }
            }
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteTask(taskId)
                _uiState.update { it.copy(successMessage = "Aufgabe gelöscht") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Fehler beim Löschen: ${e.message}") }
            }
        }
    }

    fun toggleTaskCompleted(taskId: Long) {
        viewModelScope.launch {
            try {
                val wasCompleted = repository.toggleTaskCompleted(taskId)
                val message = if (wasCompleted) "Aufgabe erledigt! 🎉" else "Aufgabe wieder geöffnet"
                _uiState.update { it.copy(successMessage = message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Fehler: ${e.message}") }
            }
        }
    }

    // === View Mode Management ===

    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(currentViewMode = mode) }

        // Lade spezifische Daten je nach View Mode
        when (mode) {
            ViewMode.SMART_FOCUS -> {
                viewModelScope.launch {
                    updateSmartFocusState(todayTasks.value)
                }
            }
            ViewMode.OVERDUE -> {
                // Daten werden bereits über Flow geladen
            }
            ViewMode.QUICK_WINS -> {
                // Daten werden bereits über Flow geladen
            }
            else -> { /* Andere Modi */ }
        }
    }

    // === Search ===

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        if (query.isNotBlank()) {
            viewModelScope.launch {
                repository.searchTasks(query).collect { tasks ->
                    _uiState.update { it.copy(searchResults = tasks) }
                }
            }
        } else {
            _uiState.update { it.copy(searchResults = emptyList()) }
        }
    }

    // === Smart Focus ===

    private suspend fun updateSmartFocusState(todayTasks: List<TaskEntity>) {
        val focusTasks = todayTasks.filter { !it.isCompleted }.take(5)
        val taskReasonings = focusTasks.map { task ->
            priorityCalculator.getTaskReasoning(task)
        }

        _smartFocusState.update { state ->
            state.copy(
                focusTasks = focusTasks,
                taskReasonings = taskReasonings,
                isLoading = false
            )
        }
    }

    fun getTaskReasoning(taskId: Long): TaskReasoning? {
        val task = todayTasks.value.find { it.id == taskId }
        return task?.let { priorityCalculator.getTaskReasoning(it) }
    }

    fun refreshSmartFocus() {
        viewModelScope.launch {
            _smartFocusState.update { it.copy(isLoading = true) }

            try {
                repository.refreshSmartScores()
                repository.refreshTodayListIfNeeded()

                _smartFocusState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _smartFocusState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Fehler beim Aktualisieren"
                    )
                }
            }
        }
    }

    // === Statistics ===

    private suspend fun updateProgress() {
        try {
            val progress = repository.getTodayProgress()
            _uiState.update { it.copy(todayProgress = progress) }
        } catch (e: Exception) {
            // Ignoriere Fehler bei Progress-Update
        }
    }

    private suspend fun updateContextualRecommendations() {
        try {
            val recommendations = repository.getContextualRecommendations()
            _uiState.update { it.copy(contextualRecommendations = recommendations) }
        } catch (e: Exception) {
            // Ignoriere Fehler bei Recommendations-Update
        }
    }

    suspend fun getTaskById(id: Long): TaskEntity? = repository.getTaskById(id)

    // === Dialog Management ===

    fun showCreateTaskDialog() {
        _dialogState.value = DialogState.CreateTask()
    }

    fun showEditTaskDialog(task: TaskEntity) {
        _dialogState.value = DialogState.EditTask(task)
    }

    fun showDeleteConfirmDialog(task: TaskEntity) {
        _dialogState.value = DialogState.DeleteConfirm(task)
    }

    fun showTaskReasoningDialog(taskId: Long) {
        val reasoning = getTaskReasoning(taskId)
        if (reasoning != null) {
            _dialogState.value = DialogState.TaskReasoning(reasoning)
        }
    }

    fun dismissDialog() {
        _dialogState.value = null
    }

    // === Settings ===

    fun updateDailyTaskLimit(limit: Int) {
        viewModelScope.launch {
            settingsManager.setDailyTaskLimit(limit)
        }
    }

    fun toggleSmartFocus(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setSmartFocusEnabled(enabled)
        }
    }

    // === Error Handling ===

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    // === Filter & Sort ===

    fun setContextFilter(context: TaskContext?) {
        _uiState.update { it.copy(selectedContext = context) }
    }

    fun setPriorityFilter(priority: TaskPriority?) {
        _uiState.update { it.copy(selectedPriority = priority) }
    }

    fun toggleShowCompleted() {
        _uiState.update { it.copy(showCompleted = !it.showCompleted) }
    }
}

// === UI State Data Classes ===

data class TaskUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,

    // Current view
    val currentViewMode: ViewMode = ViewMode.SMART_FOCUS,

    // Statistics
    val todayProgress: TaskProgress = TaskProgress(0, 0, 0),
    val todayTasksCount: Int = 0,
    val backlogCount: Int = 0,
    val overdueCount: Int = 0,
    val hasOverdueTasks: Boolean = false,

    // Search
    val searchQuery: String = "",
    val searchResults: List<TaskEntity> = emptyList(),

    // Filters
    val selectedContext: TaskContext? = null,
    val selectedPriority: TaskPriority? = null,
    val showCompleted: Boolean = true,

    // Recommendations
    val contextualRecommendations: ContextualRecommendations? = null
)

data class SmartFocusUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val focusTasks: List<TaskEntity> = emptyList(),
    val taskReasonings: List<TaskReasoning> = emptyList()
)

sealed class DialogState {
    data class CreateTask(
        val initialTitle: String = "",
        val initialContext: TaskContext = TaskContext.NONE
    ) : DialogState()

    data class EditTask(val task: TaskEntity) : DialogState()
    data class DeleteConfirm(val task: TaskEntity) : DialogState()
    data class TaskReasoning(val reasoning: TaskReasoning) : DialogState()
}

enum class ViewMode {
    SMART_FOCUS,
    ALL_TASKS,
    OVERDUE,
    TODAY,
    QUICK_WINS,
    BY_CONTEXT,
    SEARCH
}