package com.beigel.list2share

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
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
import com.beigel.list2share.auth.AuthManager
import com.beigel.list2share.data.DeviceIdManager
import com.beigel.list2share.data.NotificationPreferences
import com.beigel.list2share.notifications.NotificationRoute
import com.beigel.list2share.repository.CloudMigration
import com.beigel.list2share.repository.TodoRepository
import com.beigel.list2share.ui.screens.MainScreen
import com.beigel.list2share.ui.screens.WillkommenScreen
import com.beigel.list2share.utils.ReviewManager
import com.beigel.list2share.ui.theme.AccentColor
import com.beigel.list2share.ui.theme.AccentColorPreferences
import com.beigel.list2share.ui.theme.ThemeMode
import com.beigel.list2share.ui.theme.ThemePreferences
import com.beigel.list2share.ui.theme.TodoSharedTheme
import com.beigel.list2share.utils.HapticFeedback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    /**
     * Läuft die App schon, liefert Android den Tap auf eine Benachrichtigung
     * hier ab statt in onCreate – die Activity ist als singleTop deklariert.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        NotificationRoute.submit(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wurde die App über eine Benachrichtigung geöffnet, steckt das Ziel
        // im Start-Intent. MainScreen springt dorthin, sobald die Listen da sind.
        NotificationRoute.submit(intent)

        // enableEdgeToEdge() setzt decorFitsSystemWindows selbst und macht die
        // Systemleisten auch auf Geraeten vor Android 15 transparent. Ein
        // zusaetzliches WindowCompat.setDecorFitsSystemWindows(window, false)
        // ist redundant und ab API 35 deprecated.
        enableEdgeToEdge()

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
            var uid by remember { mutableStateOf(AuthManager.currentUid) }

            /*
             * Die UID kann sich zur Laufzeit ändern: meldet sich der Nutzer mit einem
             * Google-Konto an, das schon zu einem früheren Account gehört (typisch nach
             * einer Neuinstallation), wird der frische anonyme Account verworfen und der
             * alte übernommen. Ohne diesen Listener würde das Repository mit der alten,
             * toten UID weiterarbeiten.
             *
             * Bewusst nur auf nicht-null reagieren: beim Kontowechsel ist currentUser
             * kurzzeitig null, und ein Wechsel auf den Lade-Spinner würde die gerade
             * laufende Anmelde-Coroutine aus der Komposition werfen. Echtes Abmelden
             * löst ohnehin ein recreate() aus.
             */
            DisposableEffect(Unit) {
                val listener = FirebaseAuth.AuthStateListener { auth ->
                    auth.currentUser?.uid?.let { uid = it }
                }
                FirebaseAuth.getInstance().addAuthStateListener(listener)
                onDispose { FirebaseAuth.getInstance().removeAuthStateListener(listener) }
            }

            LaunchedEffect(Unit) {
                val id = AuthManager.ensureSignedIn()
                uid = id
            }

            // FCM-Token bei jeder gültigen UID neu hinterlegen – nach einem
            // Kontowechsel muss das Token am neuen Account hängen.
            LaunchedEffect(uid) {
                val id = uid ?: return@LaunchedEffect
                try {
                    val token = FirebaseMessaging.getInstance().token.await()
                    val repo = TodoRepository(id, this@MainActivity)
                    repo.saveDeviceToken(token)
                    repo.setPushEnabled(
                        NotificationPreferences.getPushEnabled(this@MainActivity).first()
                    )
                } catch (_: Exception) {
                    // Kein Netzwerk o.ä. – wird beim nächsten Start erneut versucht
                }
            }

            TodoSharedTheme(darkTheme = isDark, accentColor = accentColor) {
                val currentUid = uid
                if (currentUid == null) {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
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
                        val repository =
                            remember(currentUid) { TodoRepository(currentUid, this@MainActivity) }
                        LaunchedEffect(Unit) {
                            ReviewManager.maybeRequestReview(this@MainActivity)
                        }
                        // Mit Google-Konto: noch lokal liegende Listen in die Cloud
                        // übernehmen. Setzt auch eine unterbrochene Migration fort.
                        LaunchedEffect(currentUid) {
                            CloudMigration.start(this@MainActivity, currentUid)
                        }
                        MainScreen(repository = repository, deviceId = currentUid)
                    }
                }
            }
        }
    }
}