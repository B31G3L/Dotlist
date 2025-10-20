package de.beigel.anschaffungsliste.repository

import android.content.Context
import androidx.room.Room
import de.beigel.anschaffungsliste.data.Anschaffung
import de.beigel.anschaffungsliste.data.AnschaffungDao
import de.beigel.anschaffungsliste.data.AnschaffungDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnschaffungRepository(private val context: Context) {

    private val database: AnschaffungDatabase by lazy {
        Room.databaseBuilder(
            context,
            AnschaffungDatabase::class.java,
            "anschaffung_database"
        ).build()
    }

    private val dao: AnschaffungDao = database.anschaffungDao()
    private val repoScope = CoroutineScope(Dispatchers.IO)

    // Firebase-Platzhalter (werden später aktiviert)
    private val firebaseEnabled = false

    // Lokale Datenbank Operationen
    fun getAlleAnschaffungen(listenId: String = "default"): Flow<List<Anschaffung>> {
        return dao.getAlleAnschaffungen(listenId)
    }

    fun getOffeneAnschaffungen(listenId: String = "default"): Flow<List<Anschaffung>> {
        return dao.getOffeneAnschaffungen(listenId)
    }

    suspend fun addAnschaffung(anschaffung: Anschaffung) {
        // Lokal speichern
        withContext(Dispatchers.IO) {
            dao.insertAnschaffung(anschaffung)
        }

        // Firebase später hinzufügen
        if (firebaseEnabled) {
            // syncToFirebase(anschaffung)
        }
    }

    suspend fun updateAnschaffung(anschaffung: Anschaffung) {
        withContext(Dispatchers.IO) {
            dao.updateAnschaffung(anschaffung)
        }

        if (firebaseEnabled) {
            // syncToFirebase(anschaffung)
        }
    }

    suspend fun deleteAnschaffung(anschaffung: Anschaffung) {
        withContext(Dispatchers.IO) {
            dao.deleteAnschaffung(anschaffung)
        }

        if (firebaseEnabled) {
            // deleteFromFirebase(anschaffung)
        }
    }

    suspend fun updatePrioritaeten(anschaffungen: List<Anschaffung>) {
        withContext(Dispatchers.IO) {
            anschaffungen.forEachIndexed { index, anschaffung ->
                val updatedAnschaffung = anschaffung.copy(prioritaet = index)
                dao.updateAnschaffung(updatedAnschaffung)
            }
        }
    }

    suspend fun markiereAlsErledigt(id: String, erledigt: Boolean) {
        withContext(Dispatchers.IO) {
            dao.markiereAlsErledigt(id, erledigt)
        }
    }

    // Firebase-Platzhalter (werden später implementiert)
    fun startFirebaseSync(listenId: String = "default") {
        // TODO: Firebase Sync implementieren
    }

    fun stopFirebaseSync() {
        // TODO: Firebase Sync stoppen
    }

    suspend fun signInAnonymously(): Boolean {
        // Erstmal ohne Firebase
        return true
    }

    fun getCurrentUserId(): String? {
        return "local_user"
    }

    fun isSignedIn(): Boolean {
        return true
    }
}