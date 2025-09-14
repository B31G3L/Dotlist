package de.beigel.list.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Enhanced Color Palette
object DailyListColors {
    // Primary Colors
    val Green50 = Color(0xFFE8F5E8)
    val Green100 = Color(0xFFC8E6C9)
    val Green500 = Color(0xFF009966)
    val Green600 = Color(0xFF00875A)
    val Green700 = Color(0xFF00754F)
    val Green900 = Color(0xFF004D35)

    // Secondary Colors
    val Blue50 = Color(0xFFE3F2FD)
    val Blue500 = Color(0xFF2196F3)
    val Blue700 = Color(0xFF1976D2)

    // Error Colors
    val Red50 = Color(0xFFFFEBEE)
    val Red500 = Color(0xFFE53E3E)
    val Red700 = Color(0xFFD32F2F)

    // Warning Colors
    val Orange50 = Color(0xFFFFF3E0)
    val Orange500 = Color(0xFFFF8C00)
    val Orange700 = Color(0xFFE65100)

    // Surface Colors
    val Surface0 = Color(0xFFFFFFFF)
    val Surface1 = Color(0xFFFBFBFB)
    val Surface2 = Color(0xFFF6F6F6)
    val Surface3 = Color(0xFFEEEEEE)

    // Dark Surface Colors
    val DarkSurface0 = Color(0xFF0F0F0F)
    val DarkSurface1 = Color(0xFF1A1A1A)
    val DarkSurface2 = Color(0xFF252525)
    val DarkSurface3 = Color(0xFF303030)

    // Text Colors
    val TextPrimary = Color(0xFF1A1A1A)
    val TextSecondary = Color(0xFF666666)
    val TextTertiary = Color(0xFF999999)

    val DarkTextPrimary = Color(0xFFE8E8E8)
    val DarkTextSecondary = Color(0xFFB3B3B3)
    val DarkTextTertiary = Color(0xFF808080)
}

private val LightColorScheme = lightColorScheme(
    primary = DailyListColors.Green500,
    onPrimary = Color.White,
    primaryContainer = DailyListColors.Green100,
    onPrimaryContainer = DailyListColors.Green900,

    secondary = DailyListColors.Blue500,
    onSecondary = Color.White,
    secondaryContainer = DailyListColors.Blue50,
    onSecondaryContainer = DailyListColors.Blue700,

    tertiary = DailyListColors.Orange500,
    onTertiary = Color.White,
    tertiaryContainer = DailyListColors.Orange50,
    onTertiaryContainer = DailyListColors.Orange700,

    error = DailyListColors.Red500,
    onError = Color.White,
    errorContainer = DailyListColors.Red50,
    onErrorContainer = DailyListColors.Red700,

    background = DailyListColors.Surface0,
    onBackground = DailyListColors.TextPrimary,

    surface = DailyListColors.Surface0,
    onSurface = DailyListColors.TextPrimary,
    surfaceVariant = DailyListColors.Surface2,
    onSurfaceVariant = DailyListColors.TextSecondary,

    outline = DailyListColors.Surface3,
    outlineVariant = DailyListColors.Surface1,

    scrim = Color.Black.copy(alpha = 0.32f)
)

private val DarkColorScheme = darkColorScheme(
    primary = DailyListColors.Green500,
    onPrimary = Color.White,
    primaryContainer = DailyListColors.Green700,
    onPrimaryContainer = DailyListColors.Green100,

    secondary = DailyListColors.Blue500,
    onSecondary = Color.White,
    secondaryContainer = DailyListColors.Blue700,
    onSecondaryContainer = DailyListColors.Blue50,

    tertiary = DailyListColors.Orange500,
    onTertiary = Color.White,
    tertiaryContainer = DailyListColors.Orange700,
    onTertiaryContainer = DailyListColors.Orange50,

    error = DailyListColors.Red500,
    onError = Color.White,
    errorContainer = DailyListColors.Red700,
    onErrorContainer = DailyListColors.Red50,

    background = DailyListColors.DarkSurface0,
    onBackground = DailyListColors.DarkTextPrimary,

    surface = DailyListColors.DarkSurface1,
    onSurface = DailyListColors.DarkTextPrimary,
    surfaceVariant = DailyListColors.DarkSurface2,
    onSurfaceVariant = DailyListColors.DarkTextSecondary,

    outline = DailyListColors.DarkSurface3,
    outlineVariant = DailyListColors.DarkSurface2,

    scrim = Color.Black.copy(alpha = 0.5f)
)

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

@Composable
fun DailyListTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemInDarkTheme = isSystemInDarkTheme()

    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemInDarkTheme
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Animated color transitions
    val animatedColorScheme = androidx.compose.material3.ColorScheme(
        primary = animateColorAsState(
            targetValue = colorScheme.primary,
            animationSpec = tween(300), label = "primary"
        ).value,
        onPrimary = animateColorAsState(
            targetValue = colorScheme.onPrimary,
            animationSpec = tween(300), label = "onPrimary"
        ).value,
        primaryContainer = animateColorAsState(
            targetValue = colorScheme.primaryContainer,
            animationSpec = tween(300), label = "primaryContainer"
        ).value,
        onPrimaryContainer = animateColorAsState(
            targetValue = colorScheme.onPrimaryContainer,
            animationSpec = tween(300), label = "onPrimaryContainer"
        ).value,
        secondary = animateColorAsState(
            targetValue = colorScheme.secondary,
            animationSpec = tween(300), label = "secondary"
        ).value,
        onSecondary = animateColorAsState(
            targetValue = colorScheme.onSecondary,
            animationSpec = tween(300), label = "onSecondary"
        ).value,
        secondaryContainer = animateColorAsState(
            targetValue = colorScheme.secondaryContainer,
            animationSpec = tween(300), label = "secondaryContainer"
        ).value,
        onSecondaryContainer = animateColorAsState(
            targetValue = colorScheme.onSecondaryContainer,
            animationSpec = tween(300), label = "onSecondaryContainer"
        ).value,
        tertiary = animateColorAsState(
            targetValue = colorScheme.tertiary,
            animationSpec = tween(300), label = "tertiary"
        ).value,
        onTertiary = animateColorAsState(
            targetValue = colorScheme.onTertiary,
            animationSpec = tween(300), label = "onTertiary"
        ).value,
        tertiaryContainer = animateColorAsState(
            targetValue = colorScheme.tertiaryContainer,
            animationSpec = tween(300), label = "tertiaryContainer"
        ).value,
        onTertiaryContainer = animateColorAsState(
            targetValue = colorScheme.onTertiaryContainer,
            animationSpec = tween(300), label = "onTertiaryContainer"
        ).value,
        error = animateColorAsState(
            targetValue = colorScheme.error,
            animationSpec = tween(300), label = "error"
        ).value,
        onError = animateColorAsState(
            targetValue = colorScheme.onError,
            animationSpec = tween(300), label = "onError"
        ).value,
        errorContainer = animateColorAsState(
            targetValue = colorScheme.errorContainer,
            animationSpec = tween(300), label = "errorContainer"
        ).value,
        onErrorContainer = animateColorAsState(
            targetValue = colorScheme.onErrorContainer,
            animationSpec = tween(300), label = "onErrorContainer"
        ).value,
        background = animateColorAsState(
            targetValue = colorScheme.background,
            animationSpec = tween(300), label = "background"
        ).value,
        onBackground = animateColorAsState(
            targetValue = colorScheme.onBackground,
            animationSpec = tween(300), label = "onBackground"
        ).value,
        surface = animateColorAsState(
            targetValue = colorScheme.surface,
            animationSpec = tween(300), label = "surface"
        ).value,
        onSurface = animateColorAsState(
            targetValue = colorScheme.onSurface,
            animationSpec = tween(300), label = "onSurface"
        ).value,
        surfaceVariant = animateColorAsState(
            targetValue = colorScheme.surfaceVariant,
            animationSpec = tween(300), label = "surfaceVariant"
        ).value,
        onSurfaceVariant = animateColorAsState(
            targetValue = colorScheme.onSurfaceVariant,
            animationSpec = tween(300), label = "onSurfaceVariant"
        ).value,
        outline = animateColorAsState(
            targetValue = colorScheme.outline,
            animationSpec = tween(300), label = "outline"
        ).value,
        outlineVariant = animateColorAsState(
            targetValue = colorScheme.outlineVariant,
            animationSpec = tween(300), label = "outlineVariant"
        ).value,
        scrim = animateColorAsState(
            targetValue = colorScheme.scrim,
            animationSpec = tween(300), label = "scrim"
        ).value,
        inverseSurface = animateColorAsState(
            targetValue = colorScheme.inverseSurface,
            animationSpec = tween(300), label = "inverseSurface"
        ).value,
        inverseOnSurface = animateColorAsState(
            targetValue = colorScheme.inverseOnSurface,
            animationSpec = tween(300), label = "inverseOnSurface"
        ).value,
        inversePrimary = animateColorAsState(
            targetValue = colorScheme.inversePrimary,
            animationSpec = tween(300), label = "inversePrimary"
        ).value,
        surfaceDim = animateColorAsState(
            targetValue = colorScheme.surfaceDim,
            animationSpec = tween(300), label = "surfaceDim"
        ).value,
        surfaceBright = animateColorAsState(
            targetValue = colorScheme.surfaceBright,
            animationSpec = tween(300), label = "surfaceBright"
        ).value,
        surfaceContainerLowest = animateColorAsState(
            targetValue = colorScheme.surfaceContainerLowest,
            animationSpec = tween(300), label = "surfaceContainerLowest"
        ).value,
        surfaceContainerLow = animateColorAsState(
            targetValue = colorScheme.surfaceContainerLow,
            animationSpec = tween(300), label = "surfaceContainerLow"
        ).value,
        surfaceContainer = animateColorAsState(
            targetValue = colorScheme.surfaceContainer,
            animationSpec = tween(300), label = "surfaceContainer"
        ).value,
        surfaceContainerHigh = animateColorAsState(
            targetValue = colorScheme.surfaceContainerHigh,
            animationSpec = tween(300), label = "surfaceContainerHigh"
        ).value,
        surfaceContainerHighest = animateColorAsState(
            targetValue = colorScheme.surfaceContainerHighest,
            animationSpec = tween(300), label = "surfaceContainerHighest"
        ).value
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = animatedColorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = animatedColorScheme,
        typography = EnhancedTypography,
        content = content
    )
}