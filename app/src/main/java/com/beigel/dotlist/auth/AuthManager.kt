package com.beigel.dotlist.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Zentrale Stelle für Firebase Authentication.
 *
 * Jedes Gerät wird beim ersten Start automatisch anonym angemeldet
 * (ersetzt die frühere zufällige lokale "deviceId"). Die UID von
 * FirebaseAuth ist die verlässliche Identität, gegen die die
 * Firestore Security Rules über request.auth.uid prüfen können.
 *
 * Ein anonymer Account kann später mit einem Google-Konto verknüpft
 * werden ("linkWithCredential") — dabei bleibt die UID (und damit
 * der Zugriff auf alle bisherigen Listen) exakt erhalten.
 */
object AuthManager {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser
    val currentUid: String? get() = auth.currentUser?.uid
    val isAnonymous: Boolean get() = auth.currentUser?.isAnonymous ?: true
    val isSignedInWithGoogle: Boolean
        get() = auth.currentUser?.providerData.orEmpty().any { it.providerId == GoogleAuthProvider.PROVIDER_ID }

    /**
     * Stellt sicher, dass ein Nutzer angemeldet ist (mindestens anonym).
     * Beim allerersten Start wird automatisch ein neuer anonymer Account erzeugt;
     * bei jedem weiteren Start ist der Nutzer bereits angemeldet.
     */
    suspend fun ensureSignedIn(): String {
        auth.currentUser?.let { return it.uid }
        val result = auth.signInAnonymously().await()
        return result.user?.uid ?: error("Anonyme Anmeldung fehlgeschlagen")
    }

    /**
     * Verknüpft den aktuellen anonymen Account mit einem Google-Konto.
     * Die UID (und damit alle bisherigen Listen/Zuweisungen) bleibt erhalten.
     */
    suspend fun linkWithGoogleIdToken(idToken: String): FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val current = auth.currentUser
        return if (current != null && current.isAnonymous) {
            current.linkWithCredential(credential).await().user
                ?: error("Verknüpfung mit Google fehlgeschlagen")
        } else {
            auth.signInWithCredential(credential).await().user
                ?: error("Google-Anmeldung fehlgeschlagen")
        }
    }

    fun signOut() = auth.signOut()
}