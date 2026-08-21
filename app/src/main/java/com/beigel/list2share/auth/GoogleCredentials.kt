package com.beigel.list2share.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.beigel.list2share.R

/**
 * Vom Credential Manager gelieferte Google-Identität.
 */
data class GoogleIdentity(
    val idToken: String,
    val displayName: String?,
    val email: String?,
)

/**
 * Öffnet den Google-Kontoauswahl-Dialog und liefert das ID-Token.
 *
 * Wird von WillkommenScreen und KontoScreen gemeinsam genutzt, damit der
 * Credential-Manager-Boilerplate nur an einer Stelle steht.
 *
 * @return null, wenn der Nutzer zwar etwas ausgewählt hat, es aber kein
 *         Google-ID-Token war. Bei Abbruch wirft der Credential Manager eine
 *         `GetCredentialException`, die der Aufrufer behandeln muss.
 */
suspend fun requestGoogleIdentity(context: Context): GoogleIdentity? {
    val credentialManager = CredentialManager.create(context)
    val option = GetSignInWithGoogleOption
        .Builder(context.getString(R.string.google_web_client_id))
        .build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

    val credential = credentialManager.getCredential(context, request).credential
    if (credential !is CustomCredential ||
        credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) return null

    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
    return GoogleIdentity(
        idToken = googleCredential.idToken,
        displayName = googleCredential.displayName,
        email = googleCredential.id,
    )
}
