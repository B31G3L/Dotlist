package com.beigel.list2share.notifications

import android.content.Context
import com.beigel.list2share.data.DeviceIdManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Verwaltet die FCM-Tokens eines Nutzers in Firestore.
 *
 * Struktur:
 *   deviceTokens/{uid}/tokens/{installationId}
 *       token, platform, pushEnabled, updatedAt
 *
 * Ein Dokument pro Installation statt eines Tokens pro Nutzer: sobald dieselbe
 * UID auf mehreren Geräten (Handy, Web, Desktop) angemeldet ist, würde sonst
 * jedes Gerät das Token des anderen überschreiben, und Pushes kämen nur noch
 * auf dem zuletzt gestarteten an. Als Dokument-ID dient die zufällige
 * Installations-ID aus [DeviceIdManager] – bei einer Token-Rotation wird also
 * dasselbe Dokument aktualisiert, statt dass sich alte Tokens ansammeln.
 *
 * `pushEnabled` hängt ebenfalls an der Installation: wer Pushes am Handy
 * abschaltet, will sie deshalb nicht zwingend am Desktop verlieren.
 */
object PushTokenStore {

    private const val PLATFORM = "android"

    private fun db() = FirebaseFirestore.getInstance()

    private fun installationRef(context: Context, uid: String): DocumentReference =
        db().collection("deviceTokens")
            .document(uid)
            .collection("tokens")
            .document(DeviceIdManager.getDeviceId(context))

    /**
     * Altes Format `deviceTokens/{uid}` mit genau einem Token.
     *
     * TODO: Übergangsweise weiter beschreiben, bis die Cloud Function auf die
     *  Subcollection umgestellt ist und keine ältere App-Version mehr aktiv ist.
     *  Danach die beiden legacy-Aufrufe entfernen.
     */
    private fun legacyRef(uid: String): DocumentReference =
        db().collection("deviceTokens").document(uid)

    // Legacy jeweils zuerst: sind die neuen Rules noch nicht deployt, schlägt nur
    // der zweite Schreibvorgang fehl, und Pushes über die alte Function laufen weiter.

    suspend fun save(context: Context, uid: String, token: String) {
        val now = Timestamp.now()
        legacyRef(uid)
            .set(mapOf("token" to token, "updatedAt" to now), SetOptions.merge())
            .await()
        installationRef(context, uid)
            .set(
                mapOf("token" to token, "platform" to PLATFORM, "updatedAt" to now),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun setPushEnabled(context: Context, uid: String, enabled: Boolean) {
        legacyRef(uid)
            .set(mapOf("pushEnabled" to enabled), SetOptions.merge())
            .await()
        installationRef(context, uid)
            .set(mapOf("pushEnabled" to enabled), SetOptions.merge())
            .await()
    }

    /**
     * Token dieser Installation entfernen – vor dem Abmelden aufrufen, solange
     * die Security Rules den Zugriff noch erlauben. Sonst bekäme das Gerät
     * weiterhin Pushes für ein Konto, das hier gar nicht mehr angemeldet ist.
     *
     * Das Legacy-Dokument bleibt bewusst unangetastet: es könnte inzwischen das
     * Token eines anderen Geräts enthalten.
     */
    suspend fun remove(context: Context, uid: String) {
        installationRef(context, uid).delete().await()
    }
}
