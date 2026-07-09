package de.beigel.list

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import de.beigel.list.auth.AuthManager
import de.beigel.list.data.DeviceIdManager
import de.beigel.list.repository.TodoRepository
import de.beigel.list.ui.screens.MainScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import de.beigel.list.ui.screens.WillkommenScreen
import de.beigel.list.ui.theme.AccentColor
import de.beigel.list.ui.theme.AccentColorPreferences
import de.beigel.list.ui.theme.ThemeMode
import de.beigel.list.ui.theme.ThemePreferences
import de.beigel.list.ui.theme.TodoSharedTheme
import de.beigel.list.utils.HapticFeedback

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val themeMode   by ThemePreferences.getThemeMode(this).collectAsState(initial = ThemeMode.SYSTEM)
            val accentColor by AccentColorPreferences.getAccentColor(this).collectAsState(initial = AccentColor.VIOLET)
            val systemDark  = isSystemInDarkTheme()

            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT  -> false
                ThemeMode.DARK   -> true
            }

            SideEffect {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                // Im Dark Theme helle (weiße) Symbole/Text in der Statusleiste, im Light Theme dunkle
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }

            // Firebase Auth ist asynchron (Netzwerk-Aufruf beim allerersten Start),
            // daher kurz warten bevor die App mit einer echten UID startet.
            var uid by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(Unit) {
                val id = AuthManager.ensureSignedIn()
                uid = id
                // FCM-Token besorgen und in Firestore hinterlegen, damit Cloud
                // Functions Push-Nachrichten an dieses Gerät schicken können.
                try {
                    val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                    TodoRepository(id).saveDeviceToken(token)
                    TodoRepository(id).setPushEnabled(de.beigel.list.data.NotificationPreferences.getPushEnabled(this@MainActivity).first())
                } catch (_: Exception) {
                    // Kein Netzwerk o.ä. – wird beim nächsten Start erneut versucht
                }
            }

            TodoSharedTheme(darkTheme = isDark, accentColor = accentColor) {
                val currentUid = uid
                if (currentUid == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    var nameSet by remember {
                        mutableStateOf(DeviceIdManager.isNameSet(this@MainActivity))
                    }
                    if (!nameSet) {
                        val haptic = remember { HapticFeedback(this@MainActivity) }
                        WillkommenScreen(haptic = haptic, onDone = { nameSet = true })
                    } else {
                        val repository = remember(currentUid) { TodoRepository(currentUid) }
                        MainScreen(repository = repository, deviceId = currentUid)
                    }
                }
            }
        }
    }
}