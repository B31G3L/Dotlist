package com.beigel.list2share.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Ergebnis einer Google-Anmeldung.
 *
 * Der Unterschied ist wichtig: bei [Linked] bleibt die UID erhalten (und damit
 * alles, was an ihr hängt), bei [SwitchedAccount] wechselt sie – dann müssen
 * lokale Daten umgeschrieben und die Activity neu gestartet werden.
 */
sealed interface GoogleAuthResult {

    /** Anonymer Account wurde mit Google verknüpft – UID bleibt exakt gleich. */
    data class Linked(val uid: String) : GoogleAuthResult

    /**
     * Das Google-Konto gehörte bereits zu einem früheren Account (klassischer Fall:
     * App neu installiert). Der frische anonyme Wegwerf-Account wurde verworfen,
     * der alte Account ist wieder aktiv – die UID hat sich dabei geändert.
     */
    data class SwitchedAccount(val previousUid: String, val uid: String) : GoogleAuthResult

    /**
     * Wie [SwitchedAccount], aber der aktuelle anonyme Account hat noch geteilte
     * Listen in Firestore, die beim Wechsel unerreichbar würden. Der Aufruf muss
     * nach Rückfrage mit `discardAnonymousData = true` wiederholt werden.
     */
    data class ConflictWithData(val sharedListCount: Int) : GoogleAuthResult
}

/**
 * Zentrale Stelle für Firebase Authentication.
 *
 * Jedes Gerät wird beim ersten Start automatisch anonym angemeldet. Die UID von
 * FirebaseAuth ist die verlässliche Identität, gegen die die Firestore Security
 * Rules über request.auth.uid prüfen können.
 */
object AuthManager {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser
    val currentUid: String? get() = auth.currentUser?.uid
    val isAnonymous: Boolean get() = auth.currentUser?.isAnonymous ?: true
    val isSignedInWithGoogle: Boolean
        get() = auth.currentUser?.providerData.orEmpty().any { it.providerId == GoogleAuthProvider.PROVIDER_ID }

    /**
     * Profilbild des Google-Kontos, oder null.
     *
     * Wird ein anonymes Konto mit Google verknüpft, übernimmt Firebase Name
     * und Bild NICHT ins Hauptprofil – `currentUser.photoUrl` bleibt dann
     * leer. Zu finden sind sie nur in den Provider-Daten. Deshalb prüft schon
     * [isSignedInWithGoogle] dort, und hier gilt dasselbe.
     */
    val googlePhotoUrl: String?
        get() {
            val user = auth.currentUser ?: return null
            user.photoUrl?.let { return it.toString() }
            return user.providerData
                .firstOrNull { it.providerId == GoogleAuthProvider.PROVIDER_ID }
                ?.photoUrl
                ?.toString()
        }

    /**
     * Stellt sicher, dass ein Nutzer angemeldet ist (mindestens anonym).
     * Beim allerersten Start wird ein neuer anonymer Account erzeugt.
     */
    suspend fun ensureSignedIn(): String {
        auth.currentUser?.let { return it.uid }
        val result = auth.signInAnonymously().await()
        return result.user?.uid ?: error("Anonyme Anmeldung fehlgeschlagen")
    }

    /**
     * Meldet mit einem Google-ID-Token an und löst dabei den Fall auf, dass das
     * Google-Konto bereits zu einem früheren Account gehört.
     *
     * Ablauf:
     *  1. Ist der aktuelle Nutzer anonym, wird zuerst `linkWithCredential` versucht –
     *     dabei bleibt die UID erhalten (Erstnutzer, der sein Konto absichert).
     *  2. Wirft Firebase dabei eine [FirebaseAuthUserCollisionException], ist das
     *     Google-Konto schon vergeben. Der anonyme Account war dann ein Wegwerf-Account
     *     (typisch nach Neuinstallation) und wird gelöscht, danach normale Anmeldung
     *     mit `signInWithCredential`.
     *  3. Hätte der anonyme Account dabei noch geteilte Listen zu verlieren, wird
     *     stattdessen [GoogleAuthResult.ConflictWithData] zurückgegeben, damit die UI
     *     nachfragen kann.
     *
     * @param discardAnonymousData Bestätigung des Nutzers, dass Schritt 3 übersprungen
     *        und der anonyme Account verworfen werden darf.
     */
    suspend fun signInWithGoogle(
        idToken: String,
        discardAnonymousData: Boolean = false,
    ): GoogleAuthResult {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val current = auth.currentUser

        // Kein oder bereits nicht-anonymer Nutzer: einfach anmelden.
        if (current == null || !current.isAnonymous) {
            val user = auth.signInWithCredential(credential).await().user
                ?: error("Google-Anmeldung fehlgeschlagen")
            return if (current == null || current.uid == user.uid) {
                GoogleAuthResult.Linked(user.uid)
            } else {
                GoogleAuthResult.SwitchedAccount(current.uid, user.uid)
            }
        }

        val previousUid = current.uid

        return try {
            val user = current.linkWithCredential(credential).await().user
                ?: error("Verknüpfung mit Google fehlgeschlagen")
            GoogleAuthResult.Linked(user.uid)
        } catch (collision: FirebaseAuthUserCollisionException) {
            // Google-Konto hängt schon an einem anderen (= dem alten) Account.
            if (!discardAnonymousData) {
                val count = countSharedLists(previousUid)
                if (count > 0) return GoogleAuthResult.ConflictWithData(count)
            }
            // Wegwerf-Account aufräumen, solange wir ihn noch erreichen können –
            // nach signInWithCredential ist er nicht mehr referenzierbar.
            runCatching { current.delete().await() }

            val user = auth.signInWithCredential(credential).await().user
                ?: error("Google-Anmeldung fehlgeschlagen")
            GoogleAuthResult.SwitchedAccount(previousUid, user.uid)
        }
    }

    /**
     * Wie viele geteilte Listen hängen an dieser UID? Dient nur der Rückfrage vor
     * dem Verwerfen eines anonymen Accounts; im Fehlerfall (offline) 0.
     */
    private suspend fun countSharedLists(uid: String): Int = runCatching {
        FirebaseFirestore.getInstance()
            .collection("lists")
            .whereArrayContains("memberIds", uid)
            .limit(20)
            .get()
            .await()
            .size()
    }.getOrDefault(0)

    fun signOut() = auth.signOut()
}