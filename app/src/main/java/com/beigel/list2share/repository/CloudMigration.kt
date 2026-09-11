package com.beigel.list2share.repository

import android.content.Context
import android.util.Log
import com.beigel.list2share.auth.AuthManager
import com.beigel.list2share.data.DeviceIdManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

/**
 * Überführt nach einer Google-Anmeldung alle lokalen Listen in Firestore.
 *
 * Mit Google-Konto gilt: jede Liste liegt in der Cloud, damit sie auch in der
 * Web- und Desktop-Version erscheint. Room bleibt nur für anonyme Nutzer.
 *
 * Läuft bewusst in einem eigenen, prozessweiten Scope statt im Scope eines
 * Screens: der Upload kann offline beliebig lange hängen, und ein Verlassen
 * des Konto-Screens oder ein Activity-recreate() soll ihn nicht abbrechen.
 *
 * Wird an zwei Stellen angestoßen:
 *  - direkt nach einer erfolgreichen Verknüpfung ([AuthManager.signInWithGoogle]),
 *  - bei jedem App-Start. Damit wird eine abgebrochene Migration (App beendet,
 *    kein Netz, Fehler bei einer einzelnen Liste) automatisch fortgesetzt.
 *
 * Mehrfaches Aufrufen ist unkritisch: läuft für dieselbe UID schon eine
 * Migration, passiert nichts; wechselt die UID, wird die alte abgebrochen.
 */
object CloudMigration {

    private const val TAG = "CloudMigration"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var runningUid: String? = null

    @Synchronized
    fun start(context: Context, uid: String) {
        if (!AuthManager.isSignedInWithGoogle) return
        if (job?.isActive == true && runningUid == uid) return

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
                val migrated = TodoRepository(uid, appContext).migrateLocalListsToCloud(name)
                if (migrated > 0) Log.i(TAG, "$migrated lokale Liste(n) in die Cloud übernommen")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Migration unterbrochen, nächster Versuch beim nächsten Start", e)
            }
        }
    }
}
