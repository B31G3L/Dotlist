package com.beigel.list2share.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
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
import androidx.credentials.exceptions.GetCredentialException
import com.beigel.list2share.R
import com.beigel.list2share.auth.AuthManager
import com.beigel.list2share.auth.GoogleAuthResult
import com.beigel.list2share.auth.GoogleIdentity
import com.beigel.list2share.auth.requestGoogleIdentity
import com.beigel.list2share.data.DeviceIdManager
import com.beigel.list2share.data.local.LocalOwnership
import com.beigel.list2share.notifications.PushTokenStore
import com.beigel.list2share.repository.CloudMigration
import com.beigel.list2share.utils.HapticFeedback
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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
    var isSigningOut  by remember { mutableStateOf(false) }
    var isLinking     by remember { mutableStateOf(false) }

    // Google-Konto existiert bereits, der aktuelle anonyme Account hätte aber noch
    // geteilte Listen zu verlieren -> Rückfrage, bevor er verworfen wird.
    var conflict by remember { mutableStateOf<Pair<GoogleIdentity, Int>?>(null) }

    /**
     * Verarbeitet ein Google-ID-Token.
     *
     * Wichtig: nach einer Neuinstallation ist der lokale Auth-Token weg, die App
     * legt einen frischen anonymen Account an – und `linkWithCredential` schlägt
     * dann zwangsläufig fehl, weil das Google-Konto bereits an den alten Account
     * gebunden ist. [AuthManager.signInWithGoogle] fängt genau diesen Fall ab und
     * meldet stattdessen am bestehenden Account an.
     */
    suspend fun applyIdentity(identity: GoogleIdentity, discardAnonymousData: Boolean) {
        when (val result = AuthManager.signInWithGoogle(identity.idToken, discardAnonymousData)) {

            is GoogleAuthResult.ConflictWithData -> {
                conflict = identity to result.sharedListCount
            }

            is GoogleAuthResult.Linked -> {
                isLinked    = true
                googleEmail = AuthManager.currentUser?.email
                identity.displayName?.let { DeviceIdManager.setDeviceName(context, it) }
                haptic.click()
                // UID bleibt gleich, die Activity wird also nicht neu erzeugt –
                // die lokalen Listen müssen hier explizit in die Cloud.
                CloudMigration.check(context, result.uid)
            }

            is GoogleAuthResult.SwitchedAccount -> {
                identity.displayName?.let { DeviceIdManager.setDeviceName(context, it) }
                LocalOwnership.migrate(context, result.previousUid, result.uid, identity.displayName)
                haptic.click()
                // Die UID hat gewechselt: Repository und ViewModels müssen mit der
                // neuen Identität neu aufgebaut werden.
                (context as? Activity)?.recreate()
            }
        }
    }

    suspend fun startGoogleLink(discardAnonymousData: Boolean = false, reuse: GoogleIdentity? = null) {
        linkError = null
        isLinking = true
        try {
            val identity = reuse ?: requestGoogleIdentity(context)
            if (identity == null) {
                linkError = context.getString(R.string.error_link_cancelled)
                return
            }
            applyIdentity(identity, discardAnonymousData)
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

    conflict?.let { (identity, count) ->
        AlertDialog(
            onDismissRequest = { conflict = null },
            title = { Text(stringResource(R.string.dialog_account_exists_title)) },
            text  = { Text(stringResource(R.string.dialog_account_exists_message, count)) },
            confirmButton = {
                TextButton(onClick = {
                    haptic.heavy()
                    conflict = null
                    scope.launch { startGoogleLink(discardAnonymousData = true, reuse = identity) }
                }) { Text(stringResource(R.string.action_use_existing_account)) }
            },
            dismissButton = {
                TextButton(onClick = { conflict = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isSigningOut) showSignOutConfirm = false },
            title   = { Text(stringResource(R.string.dialog_sign_out_title)) },
            text    = { Text(stringResource(R.string.dialog_sign_out_message)) },
            confirmButton = {
                TextButton(
                    enabled = !isSigningOut,
                    onClick = {
                        haptic.heavy()
                        isSigningOut = true
                        scope.launch {
                            // Push-Token dieses Geräts entfernen, solange die Rules den
                            // Zugriff noch erlauben. Offline nicht ewig blockieren –
                            // abgemeldet wird in jedem Fall.
                            AuthManager.currentUid?.let { uid ->
                                runCatching { withTimeoutOrNull(3_000) { PushTokenStore.remove(context, uid) } }
                            }
                            AuthManager.signOut()
                            showSignOutConfirm = false
                            onSignedOut()
                        }
                    }
                ) { Text(stringResource(R.string.action_sign_out), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(enabled = !isSigningOut, onClick = { showSignOutConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}