package de.beigel.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import de.beigel.todo.data.DeviceIdManager
import de.beigel.todo.repository.TodoRepository
import de.beigel.todo.ui.screens.MainScreen
import de.beigel.todo.ui.theme.AccentColor
import de.beigel.todo.ui.theme.AccentColorPreferences
import de.beigel.todo.ui.theme.ThemeMode
import de.beigel.todo.ui.theme.ThemePreferences
import de.beigel.todo.ui.theme.TodoSharedTheme

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

            TodoSharedTheme(darkTheme = isDark, accentColor = accentColor) {
                MainScreen(repository = repository, deviceId = deviceId)
            }
        }
    }
}