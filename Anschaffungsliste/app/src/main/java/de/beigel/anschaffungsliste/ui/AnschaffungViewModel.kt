package de.beigel.anschaffungsliste.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.beigel.anschaffungsliste.data.Anschaffung
import de.beigel.anschaffungsliste.repository.AnschaffungRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AnschaffungViewModel(
    private val repository: AnschaffungRepository
) : ViewModel() {

    // UI State
    private val _anschaffungen = MutableStateFlow<List<Anschaffung>>(emptyList())
    val anschaffungen: StateFlow<List<Anschaffung>> = _anschaffungen.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    // Form State
    var dialogName by mutableStateOf("")
        private set
    var dialogPreis by mutableStateOf("")
        private set
    var dialogLink by mutableStateOf("")
        private set

    private var editingAnschaffung: Anschaffung? = null

    // Aktuelle Liste ID (für geteilte Listen)
    private val _currentListenId = MutableStateFlow("default")
    val currentListenId: StateFlow<String> = _currentListenId.asStateFlow()

    init {
        loadAnschaffungen()
        initializeFirebase()
    }

    private fun initializeFirebase() {
        viewModelScope.launch {
            if (!repository.isSignedIn()) {
                repository.signInAnonymously()
            }
            repository.startFirebaseSync(_currentListenId.value)
        }
    }

    private fun loadAnschaffungen() {
        viewModelScope.launch {
            repository.getOffeneAnschaffungen(_currentListenId.value).collect {
                _anschaffungen.value = it
            }
        }
    }

    fun showAddDialog() {
        clearDialogFields()
        editingAnschaffung = null
        _showDialog.value = true
    }

    fun showEditDialog(anschaffung: Anschaffung) {
        dialogName = anschaffung.name
        dialogPreis = anschaffung.preis.toString()
        dialogLink = anschaffung.link
        editingAnschaffung = anschaffung
        _showDialog.value = true
    }

    fun hideDialog() {
        _showDialog.value = false
        clearDialogFields()
        editingAnschaffung = null
    }

    fun updateDialogName(name: String) {
        dialogName = name
    }

    fun updateDialogPreis(preis: String) {
        dialogPreis = preis
    }

    fun updateDialogLink(link: String) {
        dialogLink = link
    }

    fun saveAnschaffung() {
        val preis = dialogPreis.toDoubleOrNull() ?: 0.0

        if (dialogName.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true

            try {
                if (editingAnschaffung != null) {
                    // Bearbeiten
                    val updated = editingAnschaffung!!.copy(
                        name = dialogName,
                        preis = preis,
                        link = dialogLink
                    )
                    repository.updateAnschaffung(updated)
                } else {
                    // Neu hinzufügen
                    val neue = Anschaffung(
                        name = dialogName,
                        preis = preis,
                        link = dialogLink,
                        prioritaet = _anschaffungen.value.size,
                        listenId = _currentListenId.value
                    )
                    repository.addAnschaffung(neue)
                }

                hideDialog()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAnschaffung(anschaffung: Anschaffung) {
        viewModelScope.launch {
            repository.deleteAnschaffung(anschaffung)
        }
    }

    fun toggleErledigt(anschaffung: Anschaffung) {
        viewModelScope.launch {
            repository.markiereAlsErledigt(anschaffung.id, !anschaffung.erledigt)
        }
    }

    fun updatePrioritaeten(newOrder: List<Anschaffung>) {
        viewModelScope.launch {
            repository.updatePrioritaeten(newOrder)
        }
    }

    fun switchToList(listenId: String) {
        _currentListenId.value = listenId
        repository.stopFirebaseSync()
        repository.startFirebaseSync(listenId)
        loadAnschaffungen()
    }

    private fun clearDialogFields() {
        dialogName = ""
        dialogPreis = ""
        dialogLink = ""
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopFirebaseSync()
    }
}

class AnschaffungViewModelFactory(
    private val repository: AnschaffungRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnschaffungViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnschaffungViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}