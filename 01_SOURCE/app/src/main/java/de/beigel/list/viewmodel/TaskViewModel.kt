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

    val todayTasks = repository.getTasksForToday()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTask(title: String, description: String = "", priority: TaskPriority = TaskPriority.MEDIUM) {
        if (title.isBlank()) return

        viewModelScope.launch {
            val newTask = TaskEntity(
                title = title.trim(),
                description = description.trim(),
                priority = priority,
                position = todayTasks.value.size
            )
            repository.insertTask(newTask)
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

    fun reorderTasks(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentTasks = todayTasks.value.toMutableList()
            val movedTask = currentTasks.removeAt(fromIndex)
            currentTasks.add(toIndex, movedTask)

            val updatedTasks = currentTasks.mapIndexed { index, task ->
                task.copy(position = index)
            }

            repository.updateTaskPositions(updatedTasks)
        }
    }

    fun setShowCompleted(show: Boolean) {
        _uiState.update { it.copy(showCompletedTasks = show) }
    }

    fun setDialogState(state: DialogState) {
        _uiState.update { it.copy(dialogState = state) }
    }
}

data class TaskUiState(
    val showCompletedTasks: Boolean = true,
    val dialogState: DialogState = DialogState.None
)

sealed class DialogState {
    object None : DialogState()
    object AddTask : DialogState()
    data class EditTask(val task: TaskEntity) : DialogState()
    data class TaskDetails(val task: TaskEntity) : DialogState()
}

class TaskViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
