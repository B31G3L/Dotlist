package com.beigel.dotlist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.beigel.dotlist.data.AppNotification
import com.beigel.dotlist.repository.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notifications: List<AppNotification> = emptyList(),
    val isLoading: Boolean = true,
) {
    val unreadCount: Int get() = notifications.count { !it.isRead }
}

class NotificationsViewModel(private val repository: TodoRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    /** Wird bei jeder neu eingetroffenen Benachrichtigung aufgerufen (für die Systembenachrichtigung). */
    var onNewNotification: ((AppNotification) -> Unit)? = null

    private var knownIds: Set<String>? = null

    init {
        viewModelScope.launch {
            repository.observeNotifications()
                .catch { _uiState.update { it.copy(isLoading = false) } }
                .collect { list ->
                    val previous = knownIds
                    if (previous != null) {
                        list.filter { it.id !in previous }.forEach { onNewNotification?.invoke(it) }
                    }
                    knownIds = list.map { it.id }.toSet()
                    _uiState.update { it.copy(notifications = list, isLoading = false) }
                }
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch { runCatching { repository.markNotificationRead(id) } }
    }

    fun markAllRead() {
        val unreadIds = _uiState.value.notifications.filter { !it.isRead }.map { it.id }
        viewModelScope.launch { runCatching { repository.markAllNotificationsRead(unreadIds) } }
    }

    class Factory(private val repository: TodoRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NotificationsViewModel(repository) as T
    }
}