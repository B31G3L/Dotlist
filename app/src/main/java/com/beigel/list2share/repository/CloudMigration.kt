package com.beigel.list2share.repository

import android.content.Context
import android.util.Log
import com.beigel.list2share.auth.AuthManager
import com.beigel.list2share.data.DeviceIdManager
import com.beigel.list2share.data.MigrationPreferences
import com.beigel.list2share.data.TodoList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Übernahme lokaler Listen in die Cloud nach einer Google-Anmeldung.
 *
 * Bewusst mit Rückfrage statt automatisch: wer die App ohne Konto benutzt hat,
 * hat seine Listen absichtlich nur auf dem Gerät. Dass eine Anmeldung sie
 * stillschweigend auf einen Server legt, wäre eine Entscheidung über den Kopf
 * des Nutzers hinweg – und eine, die auch im Data-Safety-Formular steht.
 *
 * Ablauf: [check] sammelt die noch lokalen Listen und meldet sie über
 * [pending]. Die Oberfläche fragt, danach ruft sie [migrate] für die
 * ausgewählten und [decline] für die übrigen auf. Abgelehnte Listen bleiben
 * lokal und werden nicht erneut vorgeschlagen; hochladen kann man sie später
 * jederzeit über den Teilen-Regler.
 *
 * Der Upload läuft in einem eigenen, prozessweiten Scope: er kann offline
 * beliebig lange hängen, und ein Verlassen des Screens oder ein
 * Activity-recreate() soll ihn nicht abbrechen.
 */
object CloudMigration {

    private const val TAG = "CloudMigration"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var runningUid: String? = null

    /** Lokale Listen, über die der Nutzer noch nicht entschieden hat. */
    private val _pending = MutableStateFlow<List<TodoList>>(emptyList())
    val pending: StateFlow<List<TodoList>> = _pending.asStateFlow()

    /**
     * Prüft, ob es noch lokale Listen gibt, und meldet sie über [pending].
     *
     * Wird beim App-Start und nach einer Verknüpfung aufgerufen. Ohne
     * Google-Konto oder ohne offene Listen passiert nichts.
     */
    fun check(context: Context, uid: String) {
        if (!AuthManager.isSignedInWithGoogle) {
            _pending.value = emptyList()
            return
        }
        val appContext = context.applicationContext
        scope.launch {
            try {
                val declined = MigrationPreferences.getDeclinedIds(appContext)
                val local = TodoRepository(uid, appContext).localListsOnce()
                _pending.value = local.filterNot { it.id in declined }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Lokale Listen nicht gelesen", e)
            }
        }
    }

    /** Die ausgewählten Listen in die Cloud übernehmen. */
    @Synchronized
    fun migrate(context: Context, uid: String, ids: Set<String>) {
        _pending.value = emptyList()
        if (ids.isEmpty()) return

        val previous = job
        val appContext = context.applicationContext
        runningUid = uid

        job = scope.launch {
            previous?.cancelAndJoin()
            // Während wir auf den alten Job gewartet haben, kann sich die
            // Anmeldung erneut geändert haben.
            if (AuthManager.currentUid != uid || !AuthManager.isSignedInWithGoogle) return@launch

            try {
                val name = DeviceIdManager.getDeviceName(appContext)
                val migrated = TodoRepository(uid, appContext).migrateLocalListsToCloud(name, ids)
                if (migrated > 0) Log.i(TAG, "$migrated lokale Liste(n) in die Cloud übernommen")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Migration unterbrochen, nächster Versuch beim nächsten Start", e)
            }
        }
    }

    /** Wegwischen ohne Entscheidung: beim nächsten Start wird erneut gefragt. */
    fun dismiss() {
        _pending.value = emptyList()
    }

    /** Die übrigen Listen bleiben lokal und werden nicht erneut vorgeschlagen. */
    fun decline(context: Context, ids: Set<String>) {
        _pending.value = emptyList()
        if (ids.isEmpty()) return
        val appContext = context.applicationContext
        scope.launch {
            runCatching { MigrationPreferences.addDeclined(appContext, ids) }
                .onFailure { Log.w(TAG, "Ablehnung nicht gespeichert", it) }
        }
    }
}
