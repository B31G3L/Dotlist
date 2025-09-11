package de.beigel.list.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.beigel.list.data.TaskEntity
import de.beigel.list.data.TaskPriority
import de.beigel.list.repository.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
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

    fun addTask(title: String, description: String = "", priority: TaskPriority = TaskPriority.MEDIUM, addToDaily: Boolean = true) {
        if (title.isBlank()) return

        viewModelScope.launch {
            val newTask = TaskEntity(
                title = title.trim(),
                description = description.trim(),
                priority = priority,
                position = if (addToDaily) todayTasks.value.size else 0,
                backlogPosition = if (!addToDaily) backlogTasks.value.size else 0
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

    // NEU: Fehlende Methode für Drag & Drop
    fun updateTaskPositions(tasks: List<TaskEntity>) {
        viewModelScope.launch {
            repository.updateTaskPositions(tasks)
        }
    }

    // NEU: Reorder Methoden für bessere UX
    fun moveTask(fromIndex: Int, toIndex: Int, isDaily: Boolean = true) {
        viewModelScope.launch {
            val currentTasks = if (isDaily) todayTasks.value else backlogTasks.value
            if (fromIndex in currentTasks.indices && toIndex in currentTasks.indices) {
                val reorderedTasks = currentTasks.toMutableList()
                val movedTask = reorderedTasks.removeAt(fromIndex)
                reorderedTasks.add(toIndex, movedTask)

                // Update positions in database
                updateTaskPositions(reorderedTasks)
            }
        }
    }

    // NEU: Batch-Operationen
    fun markMultipleTasksComplete(taskIds: List<String>, completed: Boolean = true) {
        viewModelScope.launch {
            taskIds.forEach { taskId ->
                val allTasks = todayTasks.value + backlogTasks.value
                val task = allTasks.find { it.id == taskId }
                task?.let {
                    val updatedTask = it.copy(
                        isCompleted = completed,
                        completedAt = if (completed) LocalDateTime.now().toString() else null
                    )
                    repository.updateTask(updatedTask)
                }
            }
        }
    }

    fun deleteMultipleTasks(taskIds: List<String>) {
        viewModelScope.launch {
            taskIds.forEach { taskId ->
                val allTasks = todayTasks.value + backlogTasks.value
                val task = allTasks.find { it.id == taskId }
                task?.let { repository.deleteTask(it) }
            }
        }
    }

    fun setShowCompleted(show: Boolean) {
        _uiState.update { it.copy(showCompletedTasks = show) }
    }

    fun setDialogState(state: DialogState) {
        _uiState.update { it.copy(dialogState = state) }
    }

    fun setCurrentView(view: ViewType) {
        _uiState.update { it.copy(currentView = view) }
    }

    // NEU: Search functionality
    fun searchTasks(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    // NEU: Filter functionality
    fun filterByPriority(priority: TaskPriority?) {
        _uiState.update { it.copy(priorityFilter = priority) }
    }

    // NEU: Computed properties für gefilterte Listen
    val filteredTodayTasks = combine(
        todayTasks,
        _uiState
    ) { tasks, state ->
        tasks.filter { task ->
            val matchesSearch = if (state.searchQuery.isBlank()) {
                true
            } else {
                task.title.contains(state.searchQuery, ignoreCase = true) ||
                        task.description.contains(state.searchQuery, ignoreCase = true)
            }

            val matchesPriority = state.priorityFilter?.let { filter ->
                task.priority == filter
            } ?: true

            val matchesCompletion = if (state.showCompletedTasks) {
                true
            } else {
                !task.isCompleted
            }

            matchesSearch && matchesPriority && matchesCompletion
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredBacklogTasks = combine(
        backlogTasks,
        _uiState
    ) { tasks, state ->
        tasks.filter { task ->
            val matchesSearch = if (state.searchQuery.isBlank()) {
                true
            } else {
                task.title.contains(state.searchQuery, ignoreCase = true) ||
                        task.description.contains(state.searchQuery, ignoreCase = true)
            }

            val matchesPriority = state.priorityFilter?.let { filter ->
                task.priority == filter
            } ?: true

            val matchesCompletion = if (state.showCompletedTasks) {
                true
            } else {
                !task.isCompleted
            }

            matchesSearch && matchesPriority && matchesCompletion
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}

data class TaskUiState(
    val showCompletedTasks: Boolean = true,
    val dialogState: DialogState = DialogState.None,
    val currentView: ViewType = ViewType.DAILY,
    val searchQuery: String = "",
    val priorityFilter: TaskPriority? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

enum class ViewType {
    DAILY, BACKLOG
}

sealed class DialogState {
    object None : DialogState()
    data class AddTask(val addToDaily: Boolean = true) : DialogState()
    data class EditTask(val task: TaskEntity) : DialogState()
    data class TaskDetails(val task: TaskEntity) : DialogState()
}