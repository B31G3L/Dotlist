package de.beigel.list

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import de.beigel.list.data.DeviceIdManager
import de.beigel.list.repository.TodoRepository
import de.beigel.list.ui.screens.MainScreen
import de.beigel.list.ui.theme.AccentColor
import de.beigel.list.ui.theme.AccentColorPreferences
import de.beigel.list.ui.theme.ThemeMode
import de.beigel.list.ui.theme.ThemePreferences
import de.beigel.list.ui.theme.TodoSharedTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val deviceId   = DeviceIdManager.getDeviceId(this)
        val repository = TodoRepository(deviceId)

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

            TodoSharedTheme(darkTheme = isDark, accentColor = accentColor) {
                MainScreen(repository = repository, deviceId = deviceId)
            }
        }
    }
}