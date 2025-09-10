package de.beigel.list.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// App-spezifische Farben
private val AppGreen = Color(0xFF009966)
private val AppGreenLight = Color(0xFF00CC88)
private val AppGreenDark = Color(0xFF007755)

private val AppDarkGray = Color(0xFF3C3C3C)
private val AppMediumGray = Color(0xFF5A5A5A)
private val AppLightGray = Color(0xFF787878)

// Priority Farben
private val PriorityHigh = Color(0xFFE53E3E)
private val PriorityMedium = AppGreen
private val PriorityLow = Color(0xFF4A5568)

// Dark Color Scheme
private val DarkColorScheme = darkColorScheme(
    primary = AppGreenLight,
    onPrimary = Color.Black,
    primaryContainer = AppGreenDark,
    onPrimaryContainer = Color.White,

    secondary = Color(0xFF03DAC5),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF018786),
    onSecondaryContainer = Color.White,

    tertiary = Color(0xFFBB86FC),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF6200EE),
    onTertiaryContainer = Color.White,

    error = PriorityHigh,
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFE0E0E0),

    outline = AppMediumGray,
    outlineVariant = AppLightGray
)

// Light Color Scheme
private val LightColorScheme = lightColorScheme(
    primary = AppGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F5E8),
    onPrimaryContainer = AppGreenDark,

    secondary = Color(0xFF03DAC5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F7FA),
    onSecondaryContainer = Color(0xFF00695C),

    tertiary = Color(0xFF6200EE),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE7F6),
    onTertiaryContainer = Color(0xFF4A148C),

    error = PriorityHigh,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),

    background = Color(0xFFFFFBFE),
    onBackground = AppDarkGray,
    surface = Color.White,
    onSurface = AppDarkGray,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = AppMediumGray,

    outline = AppMediumGray,
    outlineVariant = AppLightGray
)

@Composable
fun DailyListTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Erweiterte Farben für die App
object DailyListColors {
    val priorityHigh = PriorityHigh
    val priorityMedium = PriorityMedium
    val priorityLow = PriorityLow

    val successGreen = Color(0xFF10B981)
    val warningOrange = Color(0xFFFF8C00)

    val energyHigh = Color(0xFFFFD700) // Gold
    val energyMedium = Color(0xFF87CEEB) // Sky Blue
    val energyLow = Color(0xFFDDA0DD) // Plum
}

// Zusätzliche Compose-Erweiterungen
@Composable
fun priorityColor(priority: de.beigel.list.data.TaskPriority): Color {
    return when (priority) {
        de.beigel.list.data.TaskPriority.HIGH -> DailyListColors.priorityHigh
        de.beigel.list.data.TaskPriority.MEDIUM -> DailyListColors.priorityMedium
        de.beigel.list.data.TaskPriority.LOW -> DailyListColors.priorityLow
    }
}

@Composable
fun energyColor(energyLevel: de.beigel.list.data.EnergyLevel): Color {
    return when (energyLevel) {
        de.beigel.list.data.EnergyLevel.HIGH -> DailyListColors.energyHigh
        de.beigel.list.data.EnergyLevel.MEDIUM -> DailyListColors.energyMedium
        de.beigel.list.data.EnergyLevel.LOW -> DailyListColors.energyLow
    }
}