package de.beigel.list.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.beigel.list.data.ListCounts
import de.beigel.list.data.SelectedListsPreferences
import de.beigel.list.data.TodoList
import de.beigel.list.repository.TodoRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ListsUiState(
    val lists           : List<TodoList>          = emptyList(),
    val listCounts      : Map<String, ListCounts>  = emptyMap(),
    val isLoading       : Boolean        = true,
    val error           : String?        = null,
    val showCreateDialog: Boolean        = false,
    val showJoinDialog  : Boolean        = false,
    val joinSuccess     : TodoList?      = null,
    val invitePreview   : TodoList?      = null,
    val lastListId      : String?        = null,
    val selectedListIds : Set<String>    = emptySet()
)

class ListsViewModel(
    private val repository : TodoRepository,
    private val context    : Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListsUiState())
    val uiState: StateFlow<ListsUiState> = _uiState.asStateFlow()

    init {
        loadAndObserve()
    }

    private fun loadAndObserve() {
        viewModelScope.launch {
            // Gespeicherte Auswahl einmalig laden
            val savedIds = SelectedListsPreferences.getSelectedIds(context).first()

            repository.observeLists()
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
                .flatMapLatest { lists ->
                    val countsFlow: Flow<Map<String, ListCounts>> = if (lists.isEmpty()) {
                        flowOf(emptyMap())
                    } else {
                        combine(lists.map { list ->
                            repository.observeTodoCounts(list.id).map { list.id to it }
                        }) { pairs -> pairs.toMap() }
                    }
                    countsFlow.map { counts -> lists to counts }
                }
                .collect { (lists, counts) ->
                    _uiState.update { state ->
                        val validLastId = if (state.lastListId != null &&
                            lists.any { it.id == state.lastListId }) state.lastListId
                        else lists.firstOrNull()?.id

                        // Gespeicherte IDs filtern (nur noch existierende)
                        val restoredIds = savedIds.filter { id -> lists.any { it.id == id } }.toSet()
                        val validIds    = state.selectedListIds.filter { id -> lists.any { it.id == id } }.toSet()

                        val newSelected = when {
                            validIds.isNotEmpty()    -> validIds        // bereits im State
                            restoredIds.isNotEmpty() -> restoredIds     // aus DataStore
                            lists.isNotEmpty()       -> setOf(lists.first().id) // Fallback
                            else                     -> emptySet()
                        }

                        state.copy(
                            lists           = lists,
                            listCounts      = counts,
                            isLoading       = false,
                            lastListId      = validLastId,
                            selectedListIds = newSelected
                        )
                    }
                }
        }
    }

    fun toggleListSelection(listId: String) {
        _uiState.update { state ->
            val current = state.selectedListIds
            val updated = if (listId in current) {
                if (current.size > 1) current - listId else current
            } else {
                current + listId
            }
            // Sofort persistieren
            viewModelScope.launch {
                SelectedListsPreferences.setSelectedIds(context, updated)
            }
            state.copy(selectedListIds = updated)
        }
    }

    fun setLastList(listId: String) {
        _uiState.update { it.copy(lastListId = listId) }
    }

    fun createList(name: String, color: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val newId   = repository.createList(name.trim(), color)
                val updated = _uiState.value.selectedListIds + newId
                SelectedListsPreferences.setSelectedIds(context, updated)
                _uiState.update { it.copy(lastListId = newId, selectedListIds = updated) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Liste konnte nicht erstellt werden") }
            }
        }
    }

    fun deleteList(listId: String) {
        viewModelScope.launch {
            try {
                repository.deleteList(listId)
                val updated = _uiState.value.selectedListIds - listId
                SelectedListsPreferences.setSelectedIds(context, updated)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Liste konnte nicht gelöscht werden") }
            }
        }
    }

    fun leaveList(listId: String) {
        viewModelScope.launch {
            try {
                repository.leaveList(listId)
                val updated = _uiState.value.selectedListIds - listId
                SelectedListsPreferences.setSelectedIds(context, updated)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Fehler beim Verlassen der Liste") }
            }
        }
    }

    fun previewInvite(listId: String) {
        if (listId.isBlank()) return
        viewModelScope.launch {
            try {
                val list = repository.previewList(listId.trim())
                if (list != null) {
                    _uiState.update { it.copy(invitePreview = list, showJoinDialog = false) }
                } else {
                    _uiState.update { it.copy(error = "Liste nicht gefunden") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Fehler beim Laden der Einladung") }
            }
        }
    }

    fun confirmJoin(listId: String) {
        viewModelScope.launch {
            try {
                val list = repository.joinList(listId)
                if (list != null) {
                    val updated = _uiState.value.selectedListIds + list.id
                    SelectedListsPreferences.setSelectedIds(context, updated)
                    _uiState.update { it.copy(
                        joinSuccess     = list,
                        invitePreview   = null,
                        lastListId      = list.id,
                        selectedListIds = updated
                    )}
                } else {
                    _uiState.update { it.copy(error = "Liste nicht gefunden", invitePreview = null) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Fehler beim Beitreten", invitePreview = null) }
            }
        }
    }

    fun clearInvitePreview() = _uiState.update { it.copy(invitePreview = null) }

    fun joinList(listId: String) {
        if (listId.isBlank()) return
        viewModelScope.launch {
            try {
                val list = repository.joinList(listId.trim())
                if (list != null) {
                    val updated = _uiState.value.selectedListIds + list.id
                    SelectedListsPreferences.setSelectedIds(context, updated)
                    _uiState.update { it.copy(
                        joinSuccess     = list,
                        showJoinDialog  = false,
                        lastListId      = list.id,
                        selectedListIds = updated
                    )}
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
    fun showJoinDialog()   = _uiState.update { it.copy(showJoinDialog = true) }
    fun hideJoinDialog()   = _uiState.update { it.copy(showJoinDialog = false) }
    fun clearError()       = _uiState.update { it.copy(error = null) }
    fun clearJoinSuccess() = _uiState.update { it.copy(joinSuccess = null) }

    class Factory(
        private val repository : TodoRepository,
        private val context    : Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ListsViewModel(repository, context) as T
    }
}