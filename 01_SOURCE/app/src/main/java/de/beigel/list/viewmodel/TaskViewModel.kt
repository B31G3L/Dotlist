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

    fun setShowCompleted(show: Boolean) {
        _uiState.update { it.copy(showCompletedTasks = show) }
    }

    fun setDialogState(state: DialogState) {
        _uiState.update { it.copy(dialogState = state) }
    }

    fun setCurrentView(view: ViewType) {
        _uiState.update { it.copy(currentView = view) }
    }
}

data class TaskUiState(
    val showCompletedTasks: Boolean = true,
    val dialogState: DialogState = DialogState.None,
    val currentView: ViewType = ViewType.DAILY
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
