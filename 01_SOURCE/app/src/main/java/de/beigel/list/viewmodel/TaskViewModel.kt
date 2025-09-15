package de.beigel.list.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.beigel.list.data.TaskEntity
import de.beigel.list.data.TaskPriority
import de.beigel.list.repository.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    val todayTasks = repository.getDailyTasksForToday()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val backlogTasks = repository.getBacklogTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered tasks basierend auf UI State
    val filteredTodayTasks = combine(
        todayTasks,
        _uiState
    ) { tasks, state ->
        filterTasks(tasks, state)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredBacklogTasks = combine(
        backlogTasks,
        _uiState
    ) { tasks, state ->
        filterTasks(tasks, state)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun filterTasks(tasks: List<TaskEntity>, state: TaskUiState): List<TaskEntity> {
        return tasks.filter { task ->
            // Search filter
            val matchesSearch = if (state.searchQuery.isBlank()) {
                true
            } else {
                task.title.contains(state.searchQuery, ignoreCase = true) ||
                        task.description.contains(state.searchQuery, ignoreCase = true)
            }

            // Priority filter
            val matchesPriority = state.priorityFilter?.let { filter ->
                task.priority == filter
            } ?: true

            // Completion filter
            val matchesCompletion = if (state.showCompletedTasks) {
                true
            } else {
                !task.isCompleted
            }

            matchesSearch && matchesPriority && matchesCompletion
        }
    }

    fun addTask(
        title: String,
        description: String = "",
        priority: TaskPriority = TaskPriority.MEDIUM,
        addToDaily: Boolean = true
    ) {
        if (title.isBlank()) return

        viewModelScope.launch {
            val newTask = TaskEntity(
                title = title.trim(),
                description = description.trim(),
                priority = priority
            )
            repository.insertTask(newTask, addToDaily)
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            val updatedTask = task.copy(
                isCompleted = !task.isCompleted,
                completedAt = if (!task.isCompleted) LocalDateTime.now().toString() else null
            )
            repository.updateTask(updatedTask)
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun moveTaskToDaily(task: TaskEntity) {
        viewModelScope.launch {
            repository.moveTaskToDaily(task)
        }
    }

    fun moveTaskToBacklog(task: TaskEntity) {
        viewModelScope.launch {
            repository.moveTaskToBacklog(task)
        }
    }

    fun fillFromBacklog(maxTasks: Int) {
        viewModelScope.launch {
            repository.fillDailyListFromBacklog(maxTasks)
        }
    }

    // Selection Mode Functions
    fun toggleTaskSelection(taskId: String) {
        _uiState.update { currentState ->
            val selectedTasks = currentState.selectedTaskIds.toMutableSet()
            if (selectedTasks.contains(taskId)) {
                selectedTasks.remove(taskId)
            } else {
                selectedTasks.add(taskId)
            }

            currentState.copy(
                selectedTaskIds = selectedTasks,
                isSelectionMode = selectedTasks.isNotEmpty()
            )
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedTaskIds = emptySet(), isSelectionMode = false) }
    }

    fun deleteSelectedTasks() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedTaskIds
            val allTasks = todayTasks.value + backlogTasks.value

            selectedIds.forEach { taskId ->
                val task = allTasks.find { it.id == taskId }
                task?.let { repository.deleteTask(it) }
            }

            clearSelection()
        }
    }

    fun moveSelectedTasksToDaily() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedTaskIds
            val allTasks = todayTasks.value + backlogTasks.value

            selectedIds.forEach { taskId ->
                val task = allTasks.find { it.id == taskId }
                if (task != null && !task.isInDailyList) {
                    repository.moveTaskToDaily(task)
                }
            }

            clearSelection()
        }
    }

    fun moveSelectedTasksToBacklog() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedTaskIds
            val allTasks = todayTasks.value + backlogTasks.value

            selectedIds.forEach { taskId ->
                val task = allTasks.find { it.id == taskId }
                if (task != null && task.isInDailyList) {
                    repository.moveTaskToBacklog(task)
                }
            }

            clearSelection()
        }
    }

    // UI State Updates
    fun setShowCompleted(show: Boolean) {
        _uiState.update { it.copy(showCompletedTasks = show) }
    }

    fun setDialogState(state: DialogState) {
        _uiState.update { it.copy(dialogState = state) }
    }

    fun setCurrentView(view: ViewType) {
        _uiState.update { it.copy(currentView = view) }
        clearSelection() // Clear selection when switching views
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setPriorityFilter(priority: TaskPriority?) {
        _uiState.update { it.copy(priorityFilter = priority) }
    }

    fun setInteractionMode(mode: InteractionMode) {
        _uiState.update { it.copy(interactionMode = mode) }
        clearSelection()
    }

    // Helper functions
    fun getSelectedTasks(): List<TaskEntity> {
        val selectedIds = _uiState.value.selectedTaskIds
        val allTasks = todayTasks.value + backlogTasks.value
        return allTasks.filter { it.id in selectedIds }
    }

    fun canMoveSelectedToDaily(): Boolean {
        return getSelectedTasks().any { !it.isInDailyList }
    }

    fun canMoveSelectedToBacklog(): Boolean {
        return getSelectedTasks().any { it.isInDailyList }
    }
}

data class TaskUiState(
    val showCompletedTasks: Boolean = true,
    val dialogState: DialogState = DialogState.None,
    val currentView: ViewType = ViewType.DAILY,
    val searchQuery: String = "",
    val priorityFilter: TaskPriority? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedTaskIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val interactionMode: InteractionMode = InteractionMode.MINIMAL
)

enum class ViewType {
    DAILY, BACKLOG
}

enum class InteractionMode {
    MINIMAL,      // Nur Swipe-Gesten
    CONTEXT_MENU, // Long press für Context Menu
    SELECTION     // Multi-Select mit floating actions
}

sealed class DialogState {
    object None : DialogState()
    data class AddTask(val addToDaily: Boolean = true) : DialogState()
    data class EditTask(val task: TaskEntity) : DialogState()
    data class TaskDetails(val task: TaskEntity) : DialogState()
    data class ContextMenu(val task: TaskEntity, val position: androidx.compose.ui.geometry.Offset) : DialogState()
}