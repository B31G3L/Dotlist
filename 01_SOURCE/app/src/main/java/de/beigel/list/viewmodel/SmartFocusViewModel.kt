package de.beigel.list.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.beigel.list.data.*
import de.beigel.list.repository.SmartFocusRepository
import de.beigel.list.repository.TaskRepository
import de.beigel.list.repository.ContextualRecommendations
import de.beigel.list.repository.ProductivityInsights
import de.beigel.list.repository.RecommendedAction
import de.beigel.list.utils.TaskReasoning
import de.beigel.list.utils.UserContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class SmartFocusViewModel(
    private val smartFocusRepository: SmartFocusRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmartFocusUiState())
    val uiState: StateFlow<SmartFocusUiState> = _uiState.asStateFlow()

    private val _userContext = MutableStateFlow<UserContext?>(null)
    val userContext: StateFlow<UserContext?> = _userContext.asStateFlow()

    // Smart Focus Tasks - die wichtigsten 5
    val focusTasks = combine(
        smartFocusRepository.getSmartFocusTasks(),
        _uiState.map { it.currentContext }
    ) { allTasks, selectedContext ->
        if (selectedContext != null) {
            // Wenn Kontext ausgewählt, nur Tasks für diesen Kontext
            allTasks.filter { it.context == selectedContext || it.context == null }.take(5)
        } else {
            allTasks
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Quick Wins
    val quickWins = smartFocusRepository.getQuickWins(3)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Überfällige Aufgaben
    val overdueTasks = smartFocusRepository.getOverdueTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Heute fällige Aufgaben
    val todaysDueTasks = smartFocusRepository.getTodaysDueTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Contextual Recommendations
    private val _recommendations = MutableStateFlow<ContextualRecommendations?>(null)
    val recommendations: StateFlow<ContextualRecommendations?> = _recommendations.asStateFlow()

    // Productivity Insights
    private val _productivityInsights = MutableStateFlow<ProductivityInsights?>(null)
    val productivityInsights: StateFlow<ProductivityInsights?> = _productivityInsights.asStateFlow()

    // Suchfunktionalität
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults = _searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                smartFocusRepository.searchActiveTasks(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Verfügbare Tags
    private val _availableTags = MutableStateFlow<List<String>>(emptyList())
    val availableTags: StateFlow<List<String>> = _availableTags.asStateFlow()

    init {
        loadInitialData()
        startPeriodicUpdates()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                // Recommendations laden
                val recommendations = smartFocusRepository.getContextualRecommendations(_userContext.value)
                _recommendations.value = recommendations

                // Productivity Insights laden
                val insights = smartFocusRepository.getProductivityInsights()
                _productivityInsights.value = insights

                // Tags laden
                val tags = smartFocusRepository.getAllActiveTags()
                _availableTags.value = tags

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Fehler beim Laden der Daten: ${e.message}"
                    )
                }
            }
        }
    }

    private fun startPeriodicUpdates() {
        viewModelScope.launch {
            while (true) {
                delay(5 * 60 * 1000) // Alle 5 Minuten
                try {
                    smartFocusRepository.updateAllSmartScores(_userContext.value)

                    // Recommendations aktualisieren
                    val recommendations = smartFocusRepository.getContextualRecommendations(_userContext.value)
                    _recommendations.value = recommendations

                } catch (e: Exception) {
                    // Silent fail für Background Updates
                }
            }
        }
    }

    // UI Actions
    fun setCurrentContext(context: TaskContext?) {
        _uiState.update { it.copy(currentContext = context) }
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun showTaskReasoning(task: TaskEntity) {
        viewModelScope.launch {
            try {
                val reasoning = smartFocusRepository.getTaskReasoning(task)
                _uiState.update {
                    it.copy(
                        showReasoningDialog = true,
                        currentTaskReasoning = reasoning
                    )
                }
            } catch (e: Exception) {
                showError("Fehler beim Laden der Begründung")
            }
        }
    }

    fun hideTaskReasoning() {
        _uiState.update {
            it.copy(
                showReasoningDialog = false,
                currentTaskReasoning = null
            )
        }
    }

    fun showTaskDetails(task: TaskEntity) {
        _uiState.update {
            it.copy(
                showTaskDetailsDialog = true,
                selectedTask = task
            )
        }
    }

    fun hideTaskDetails() {
        _uiState.update {
            it.copy(
                showTaskDetailsDialog = false,
                selectedTask = null
            )
        }
    }

    fun showAddTaskDialog() {
        _uiState.update {
            it.copy(
                showAddTaskDialog = true,
                taskToEdit = null
            )
        }
    }

    fun showEditTaskDialog(task: TaskEntity) {
        _uiState.update {
            it.copy(
                showAddTaskDialog = true,
                taskToEdit = task
            )
        }
    }

    fun hideAddTaskDialog() {
        _uiState.update {
            it.copy(
                showAddTaskDialog = false,
                taskToEdit = null
            )
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            try {
                val updatedTask = task.copy(
                    isCompleted = !task.isCompleted,
                    completedAt = if (!task.isCompleted) LocalDateTime.now().toString() else null,
                    actualMinutes = if (!task.isCompleted) task.estimatedMinutes else null
                )
                taskRepository.updateTask(updatedTask)

                // Wenn es ein recurring task ist, neue Instanz erstellen
                if (updatedTask.isCompleted && updatedTask.isRecurring) {
                    createNextRecurringTask(updatedTask)
                }

                showSuccess("Aufgabe ${if (updatedTask.isCompleted) "erledigt" else "wieder geöffnet"}")
            } catch (e: Exception) {
                showError("Fehler beim Aktualisieren der Aufgabe")
            }
        }
    }

    private suspend fun createNextRecurringTask(completedTask: TaskEntity) {
        try {
            val pattern = completedTask.recurrencePattern ?: return
            val nextDate = LocalDate.parse(completedTask.date).plusDays(pattern.daysInterval.toLong())

            val nextTask = completedTask.copy(
                id = java.util.UUID.randomUUID().toString(),
                isCompleted = false,
                completedAt = null,
                actualMinutes = null,
                date = nextDate.toString(),
                createdAt = LocalDateTime.now().toString(),
                lastModified = LocalDateTime.now().toString()
            )

            taskRepository.insertTask(nextTask)
        } catch (e: Exception) {
            // Silent fail für recurring tasks
        }
    }

    fun addTask(
        title: String,
        description: String,
        priority: TaskPriority,
        dueDate: String?,
        estimatedMinutes: Int?,
        energyLevel: EnergyLevel,
        context: TaskContext?,
        tags: List<String>
    ) {
        viewModelScope.launch {
            try {
                val newTask = TaskEntity(
                    title = title.trim(),
                    description = description.trim(),
                    priority = priority,
                    dueDate = dueDate,
                    estimatedMinutes = estimatedMinutes,
                    energyLevel = energyLevel,
                    context = context,
                    tags = tags.joinToString(","),
                    date = dueDate ?: LocalDate.now().toString()
                )

                taskRepository.insertTask(newTask)
                hideAddTaskDialog()
                showSuccess("Aufgabe hinzugefügt")

                // Smart Score berechnen
                smartFocusRepository.updateAllSmartScores(_userContext.value)

            } catch (e: Exception) {
                showError("Fehler beim Hinzufügen der Aufgabe")
            }
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            try {
                val updatedTask = task.copy(
                    lastModified = LocalDateTime.now().toString()
                )
                taskRepository.updateTask(updatedTask)
                hideAddTaskDialog()
                showSuccess("Aufgabe aktualisiert")

                // Smart Score neu berechnen
                smartFocusRepository.updateAllSmartScores(_userContext.value)

            } catch (e: Exception) {
                showError("Fehler beim Aktualisieren der Aufgabe")
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            try {
                taskRepository.deleteTask(task)
                showSuccess("Aufgabe gelöscht")
            } catch (e: Exception) {
                showError("Fehler beim Löschen der Aufgabe")
            }
        }
    }

    fun getTasksByTag(tag: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(selectedTag = tag) }
            } catch (e: Exception) {
                showError("Fehler beim Laden der Aufgaben")
            }
        }
    }

    fun clearTagFilter() {
        _uiState.update { it.copy(selectedTag = null) }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Smart Scores aktualisieren
                smartFocusRepository.updateAllSmartScores(_userContext.value)

                // Recommendations neu laden
                val recommendations = smartFocusRepository.getContextualRecommendations(_userContext.value)
                _recommendations.value = recommendations

                // Productivity Insights aktualisieren
                val insights = smartFocusRepository.getProductivityInsights()
                _productivityInsights.value = insights

                showSuccess("Daten aktualisiert")
            } catch (e: Exception) {
                showError("Fehler beim Aktualisieren")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateUserContext(context: UserContext) {
        _userContext.value = context
        viewModelScope.launch {
            smartFocusRepository.updateAllSmartScores(context)
        }
    }

    fun getCurrentTimeMessage(): String {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 5..11 -> "Guten Morgen! 🌅"
            in 12..17 -> "Guten Tag! ☀️"
            in 18..22 -> "Guten Abend! 🌆"
            else -> "Gute Nacht! 🌙"
        }
    }

    fun getMotivationalMessage(): String {
        val insights = _productivityInsights.value
        return insights?.getMotivationalMessage() ?: "Lass uns produktiv sein! 💪"
    }

    private fun showSuccess(message: String) {
        _uiState.update {
            it.copy(
                successMessage = message,
                error = null
            )
        }
        // Message nach 3 Sekunden ausblenden
        viewModelScope.launch {
            delay(3000)
            _uiState.update { it.copy(successMessage = null) }
        }
    }

    private fun showError(message: String) {
        _uiState.update {
            it.copy(
                error = message,
                successMessage = null
            )
        }
        // Error nach 5 Sekunden ausblenden
        viewModelScope.launch {
            delay(5000)
            _uiState.update { it.copy(error = null) }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                error = null,
                successMessage = null
            )
        }
    }
}

data class SmartFocusUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val successMessage: String? = null,

    // View State
    val currentContext: TaskContext? = null,
    val viewMode: ViewMode = ViewMode.FOCUS,
    val selectedTag: String? = null,

    // Dialog States
    val showReasoningDialog: Boolean = false,
    val currentTaskReasoning: TaskReasoning? = null,
    val showTaskDetailsDialog: Boolean = false,
    val selectedTask: TaskEntity? = null,
    val showAddTaskDialog: Boolean = false,
    val taskToEdit: TaskEntity? = null,

    // UI Preferences
    val showCompletedTasks: Boolean = false,
    val showMotivationalMessages: Boolean = true,
    val enableAutoRefresh: Boolean = true
)

enum class ViewMode(val displayName: String, val icon: String) {
    FOCUS("Smart Focus", "🎯"),
    ALL("Alle Aufgaben", "📋"),
    OVERDUE("Überfällig", "🚨"),
    TODAY("Heute", "📅"),
    QUICK_WINS("Quick Wins", "⚡"),
    BY_CONTEXT("Nach Kontext", "📂"),
    SEARCH("Suche", "🔍")
}

class SmartFocusViewModelFactory(
    private val smartFocusRepository: SmartFocusRepository,
    private val taskRepository: TaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SmartFocusViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SmartFocusViewModel(smartFocusRepository, taskRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}