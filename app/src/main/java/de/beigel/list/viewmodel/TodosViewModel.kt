package de.beigel.list.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.beigel.list.data.Comment
import de.beigel.list.data.Subtask
import de.beigel.list.data.TodoItem
import de.beigel.list.data.TodoList
import de.beigel.list.repository.TodoRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TodosUiState(
    val list: TodoList? = null,
    val todos: List<TodoItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val editingTodo: TodoItem? = null
)

class TodosViewModel(
    private val repository: TodoRepository,
    private val listId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodosUiState())
    val uiState: StateFlow<TodosUiState> = _uiState.asStateFlow()

    init {
        observeTodos()
    }

    private fun observeTodos() {
        viewModelScope.launch {
            repository.observeTodos(listId)
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { todos ->
                    _uiState.update { it.copy(todos = todos, isLoading = false) }
                }
        }
    }

    fun addTodo(
        title           : String,
        description     : String = "",
        priority        : de.beigel.list.data.Priority = de.beigel.list.data.Priority.MITTEL,
        dueDate         : com.google.firebase.Timestamp? = null,
        assignedTo      : String? = null,
        reminderMinutes : Int? = null,
        actorName       : String = "",
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            try {
                repository.addTodo(listId, title, description, priority, dueDate, assignedTo, reminderMinutes)
                if (assignedTo != null) {
                    repository.notifyAssigned(assignedTo, actorName, title, listId, "")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Todo konnte nicht hinzugefügt werden") }
            }
        }
    }

    fun toggleTodo(todo: TodoItem, actorName: String = "") {
        viewModelScope.launch {
            try {
                val willBeDone = !todo.isDone
                repository.toggleTodo(listId, todo)
                if (willBeDone && todo.assignedTo != null) {
                    repository.notifyDone(todo.assignedTo, actorName, todo.title, listId, todo.id)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Fehler beim Aktualisieren") }
            }
        }
    }

    fun deleteTodo(todoId: String) {
        viewModelScope.launch {
            try {
                repository.deleteTodo(listId, todoId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Todo konnte nicht gelöscht werden") }
            }
        }
    }

    fun editTodo(todoId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            try {
                repository.editTodo(listId, todoId, newTitle)
                _uiState.update { it.copy(editingTodo = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Fehler beim Bearbeiten") }
            }
        }
    }

    fun updateTodo(
        todoId             : String,
        title              : String,
        description        : String,
        priority           : de.beigel.list.data.Priority,
        dueDate            : com.google.firebase.Timestamp?,
        assignedTo         : String?,
        reminderMinutes    : Int?,
        previousAssignedTo : String? = null,
        actorName          : String = "",
        onDone             : () -> Unit = {},
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            try {
                repository.updateTodo(listId, todoId, title, description, priority, dueDate, assignedTo, reminderMinutes)
                if (assignedTo != null && assignedTo != previousAssignedTo) {
                    repository.notifyAssigned(assignedTo, actorName, title, listId, todoId)
                }
                onDone()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Fehler beim Speichern") }
            }
        }
    }

    fun moveTodo(todo: TodoItem, toListId: String, onDone: () -> Unit = {}) {
        if (toListId == listId) { onDone(); return }
        viewModelScope.launch {
            try {
                repository.moveTodo(listId, toListId, todo)
                onDone()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Aufgabe konnte nicht verschoben werden") }
            }
        }
    }

    fun addSubtask(todo: TodoItem, title: String) {
        if (title.isBlank()) return
        val newSubtask = Subtask(id = java.util.UUID.randomUUID().toString(), title = title.trim())
        viewModelScope.launch {
            try {
                repository.updateSubtasks(listId, todo.id, todo.subtasks + newSubtask)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Unteraufgabe konnte nicht gespeichert werden") }
            }
        }
    }

    fun toggleSubtask(todo: TodoItem, subtaskId: String) {
        val updated = todo.subtasks.map { if (it.id == subtaskId) it.copy(isDone = !it.isDone) else it }
        viewModelScope.launch {
            try {
                repository.updateSubtasks(listId, todo.id, updated)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Unteraufgabe konnte nicht aktualisiert werden") }
            }
        }
    }

    fun deleteSubtask(todo: TodoItem, subtaskId: String) {
        viewModelScope.launch {
            try {
                repository.updateSubtasks(listId, todo.id, todo.subtasks.filterNot { it.id == subtaskId })
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Unteraufgabe konnte nicht gelöscht werden") }
            }
        }
    }

    fun addComment(todo: TodoItem, text: String, authorId: String, actorName: String = "") {
        if (text.isBlank()) return
        val comment = Comment(
            id        = java.util.UUID.randomUUID().toString(),
            authorId  = authorId,
            text      = text.trim(),
            createdAt = com.google.firebase.Timestamp.now()
        )
        viewModelScope.launch {
            try {
                repository.addComment(listId, todo.id, comment)
                // Zuständige Person benachrichtigen (falls es nicht der Kommentierende selbst ist)
                repository.notifyComment(todo.assignedTo, actorName, todo.title, listId, todo.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Kommentar konnte nicht gespeichert werden") }
            }
        }
    }

    fun startEditing(todo: TodoItem) = _uiState.update { it.copy(editingTodo = todo) }
    fun stopEditing() = _uiState.update { it.copy(editingTodo = null) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    class Factory(
        private val repository: TodoRepository,
        private val listId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TodosViewModel(repository, listId) as T
    }
}