package de.beigel.list.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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

    fun addTodo(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            try {
                repository.addTodo(listId, title)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Todo konnte nicht hinzugefügt werden") }
            }
        }
    }

    fun toggleTodo(todo: TodoItem) {
        viewModelScope.launch {
            try {
                repository.toggleTodo(listId, todo)
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
