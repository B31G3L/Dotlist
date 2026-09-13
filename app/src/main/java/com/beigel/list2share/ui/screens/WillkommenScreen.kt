package com.beigel.list2share.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.exceptions.GetCredentialException
import com.beigel.list2share.R
import com.beigel.list2share.auth.AuthManager
import com.beigel.list2share.auth.GoogleAuthResult
import com.beigel.list2share.auth.requestGoogleIdentity
import com.beigel.list2share.data.DeviceIdManager
import com.beigel.list2share.data.local.LocalOwnership
import com.beigel.list2share.repository.CloudMigration
import com.beigel.list2share.utils.HapticFeedback
import kotlinx.coroutines.launch

/**
 * Wird einmalig beim allerersten App-Start gezeigt (siehe DeviceIdManager.isNameSet).
 *
 * Zwei Wege:
 *  - Name eintippen: schneller Einstieg, Daten bleiben zunächst nur auf dem Gerät.
 *  - Mit Google anmelden: wer die App schon einmal benutzt hat (Gerätewechsel,
 *    Neuinstallation), bekommt seine geteilten Listen sofort zurück. Der Name
 *    kommt dann aus dem Google-Profil.
 */
@Composable
fun WillkommenScreen(
    haptic : HapticFeedback,
    onDone : (name: String) -> Unit,
) {
    val context           = LocalContext.current
    val scope             = rememberCoroutineScope()
    val focusRequester    = remember { FocusRequester() }
    val focusManager      = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var name by remember {
        mutableStateOf(
            DeviceIdManager.getDeviceName(context).takeIf {
                // Build.MODEL als Vorbelegung wollen wir nicht 1:1 übernehmen,
                // ein leeres Feld wirkt hier einladender.
                DeviceIdManager.isNameSet(context)
            } ?: ""
        )
    }
    var isSigningIn by remember { mutableStateOf(false) }
    var signInError by remember { mutableStateOf<String?>(null) }

    fun confirm() {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        haptic.click()
        DeviceIdManager.setDeviceName(context, trimmed)
        keyboardController?.hide()
        onDone(trimmed)
    }

    /**
     * Google-Anmeldung direkt beim Onboarding.
     *
     * Der anonyme Account ist hier immer wenige Sekunden alt und kann keine Daten
     * enthalten – ein [GoogleAuthResult.ConflictWithData] kann also nicht auftreten.
     * Käme es trotzdem, wird bewusst mit `discardAnonymousData = true` wiederholt.
     */
    suspend fun signInWithGoogle() {
        signInError = null
        isSigningIn = true
        keyboardController?.hide()
        try {
            val identity = requestGoogleIdentity(context)
            if (identity == null) {
                signInError = context.getString(R.string.error_link_cancelled)
                return
            }

            var result = AuthManager.signInWithGoogle(identity.idToken)
            if (result is GoogleAuthResult.ConflictWithData) {
                result = AuthManager.signInWithGoogle(identity.idToken, discardAnonymousData = true)
            }

            val displayName = identity.displayName?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: identity.email?.substringBefore('@')
                ?: name.trim().ifEmpty { context.getString(R.string.placeholder_your_name) }

            DeviceIdManager.setDeviceName(context, displayName)
            haptic.click()

            when (result) {
                is GoogleAuthResult.SwitchedAccount -> {
                    // UID hat gewechselt: lokale Referenzen umschreiben und mit der
                    // neuen Identität sauber neu starten.
                    LocalOwnership.migrate(context, result.previousUid, result.uid, displayName)
                    (context as? Activity)?.recreate()
                }
                is GoogleAuthResult.Linked -> {
                    // Beim Onboarding gibt es praktisch nie lokale Listen – der Aufruf
                    // ist trotzdem da, damit die Regel "mit Google alles in der Cloud"
                    // nicht von dieser Annahme abhängt.
                    CloudMigration.check(context, result.uid)
                    onDone(displayName)
                }
                else -> onDone(displayName)
            }
        } catch (e: GetCredentialException) {
            signInError = context.getString(R.string.error_link_cancelled)
        } catch (e: Exception) {
            signInError = context.getString(R.string.error_link_failed, e.message)
        } finally {
            isSigningIn = false
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
            ) {
                Image(
                    painter            = painterResource(id = R.mipmap.ic_launcher_background),
                    contentDescription = null,
                    modifier           = Modifier.fillMaxSize().scale(1.5f)
                )
                Image(
                    painter            = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = null,
                    modifier           = Modifier.fillMaxSize().scale(1.5f)
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text          = stringResource(R.string.welcome_title),
                fontSize      = 26.sp,
                fontWeight    = FontWeight.SemiBold,
                textAlign     = TextAlign.Center,
                color         = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text       = stringResource(R.string.welcome_subtitle),
                fontSize   = 15.sp,
                textAlign  = TextAlign.Center,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier   = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value         = name,
                onValueChange = { if (it.length <= 30) name = it },
                singleLine    = true,
                enabled       = !isSigningIn,
                placeholder   = { Text(stringResource(R.string.placeholder_your_name)) },
                shape         = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { confirm() }),
                modifier      = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick  = { confirm() },
                enabled  = name.trim().isNotEmpty() && !isSigningIn,
                shape    = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(stringResource(R.string.action_get_started), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(22.dp))

            // ─── Trenner ─────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text     = stringResource(R.string.welcome_or),
                    fontSize = 12.5.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(22.dp))

            OutlinedButton(
                onClick  = { haptic.tick(); scope.launch { signInWithGoogle() } },
                enabled  = !isSigningIn,
                shape    = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    stringResource(R.string.action_continue_with_google),
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text      = signInError ?: stringResource(R.string.welcome_google_hint),
                fontSize  = 12.5.sp,
                textAlign = TextAlign.Center,
                color     = if (signInError != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier  = Modifier.padding(horizontal = 6.dp)
            )

            Spacer(Modifier.weight(1.4f))
        }
    }
}