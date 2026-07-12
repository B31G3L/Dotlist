package de.beigel.list.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import de.beigel.list.R
import de.beigel.list.auth.AuthManager
import de.beigel.list.data.DeviceIdManager
import de.beigel.list.utils.HapticFeedback
import kotlinx.coroutines.launch

@Composable
fun KontoScreen(
    haptic  : HapticFeedback,
    onBack  : () -> Unit,
    onSignedOut: () -> Unit,
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var isLinked      by remember { mutableStateOf(AuthManager.isSignedInWithGoogle) }
    var googleEmail   by remember { mutableStateOf(AuthManager.currentUser?.email) }
    var linkError     by remember { mutableStateOf<String?>(null) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var isLinking     by remember { mutableStateOf(false) }

    suspend fun startGoogleLink() {
        linkError = null
        isLinking = true
        try {
            val credentialManager = CredentialManager.create(context)
            val option = GetSignInWithGoogleOption.Builder(context.getString(R.string.google_web_client_id)).build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val response = credentialManager.getCredential(context, request)
            val credential = response.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                AuthManager.linkWithGoogleIdToken(googleIdTokenCredential.idToken)
                isLinked    = true
                googleEmail = AuthManager.currentUser?.email
                googleIdTokenCredential.displayName?.let { DeviceIdManager.setDeviceName(context, it) }
                haptic.click()
            }
        } catch (e: GetCredentialException) {
            linkError = context.getString(R.string.error_link_cancelled)
        } catch (e: Exception) {
            linkError = context.getString(R.string.error_link_failed, e.message)
        } finally {
            isLinking = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = { haptic.tick(); onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(stringResource(R.string.title_account), fontSize = 20.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface)
        }

        Column(modifier = Modifier.padding(horizontal = 22.dp)) {

            // Status-Karte
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier          = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier         = Modifier.size(44.dp).clip(CircleShape)
                            .background(
                                (if (isLinked) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error).copy(alpha = 0.16f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isLinked) Icons.Default.CheckCircle else Icons.Default.Devices,
                            null,
                            tint = if (isLinked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            if (isLinked) stringResource(R.string.status_linked) else stringResource(R.string.status_not_linked),
                            fontSize = 15.sp, fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (isLinked) googleEmail ?: "" else stringResource(R.string.status_not_linked_hint),
                            fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            if (!isLinked) {
                Text(
                    stringResource(R.string.link_google_hint),
                    fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(
                    onClick  = { scope.launch { startGoogleLink() } },
                    enabled  = !isLinking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLinking) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(stringResource(R.string.action_link_google))
                }
                linkError?.let { msg ->
                    Text(msg, fontSize = 13.sp, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 10.dp))
                }
            } else {
                OutlinedButton(
                    onClick  = { haptic.tick(); showSignOutConfirm = true },
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_sign_out))
                }
                Text(
                    stringResource(R.string.sign_out_hint),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title   = { Text(stringResource(R.string.dialog_sign_out_title)) },
            text    = { Text(stringResource(R.string.dialog_sign_out_message)) },
            confirmButton = {
                TextButton(onClick = {
                    haptic.heavy()
                    AuthManager.signOut()
                    showSignOutConfirm = false
                    onSignedOut()
                }) { Text(stringResource(R.string.action_sign_out), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showSignOutConfirm = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}