package de.beigel.todo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.beigel.todo.data.TodoList
import de.beigel.todo.repository.TodoRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ListsUiState(
    val lists: List<TodoList> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val showJoinDialog: Boolean = false,
    val joinSuccess: TodoList? = null
)

class ListsViewModel(private val repository: TodoRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ListsUiState())
    val uiState: StateFlow<ListsUiState> = _uiState.asStateFlow()

    init {
        observeLists()
    }

    private fun observeLists() {
        viewModelScope.launch {
            repository.observeLists()
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { lists ->
                    _uiState.update { it.copy(lists = lists, isLoading = false) }
                }
        }
    }

    fun createList(name: String, color: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                repository.createList(name.trim(), color)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Liste konnte nicht erstellt werden") }
            }
        }
    }

    fun deleteList(listId: String) {
        viewModelScope.launch {
            try {
                repository.deleteList(listId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Liste konnte nicht gelöscht werden") }
            }
        }
    }

    fun leaveList(listId: String) {
        viewModelScope.launch {
            try {
                repository.leaveList(listId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Fehler beim Verlassen der Liste") }
            }
        }
    }

    fun joinList(listId: String) {
        if (listId.isBlank()) return
        viewModelScope.launch {
            try {
                val list = repository.joinList(listId.trim())
                if (list != null) {
                    _uiState.update { it.copy(joinSuccess = list, showJoinDialog = false) }
                } else {
                    _uiState.update { it.copy(error = "Liste nicht gefunden") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Fehler beim Beitreten") }
            }
        }
    }

    fun showCreateDialog() = _uiState.update { it.copy(showCreateDialog = true) }
    fun hideCreateDialog() = _uiState.update { it.copy(showCreateDialog = false) }
    fun showJoinDialog() = _uiState.update { it.copy(showJoinDialog = true) }
    fun hideJoinDialog() = _uiState.update { it.copy(showJoinDialog = false) }
    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearJoinSuccess() = _uiState.update { it.copy(joinSuccess = null) }

    class Factory(private val repository: TodoRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ListsViewModel(repository) as T
    }
}
