package com.beigel.dotlist.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class AccentColor(
    val hex      : String,
    val light    : Color,
    val dark     : Color,
    val onAccent : Color,
    val onDark   : Color? = null
) {
    VIOLET  ("#5B3A8C", Color(0xFF5B3A8C), Color(0xFFD0BCFF), Color(0xFFFFFFFF), Color(0xFF381E72)),
    ORANGE  ("#C85A18", Color(0xFFC85A18), Color(0xFFFFB07A), Color(0xFFFFFFFF)),
    SAGE    ("#4A8A6F", Color(0xFF4A8A6F), Color(0xFF8EC8B0), Color(0xFFFFFFFF)),
    CRIMSON ("#6B1220", Color(0xFF6B1220), Color(0xFFFFB3B8), Color(0xFFFFFFFF)),
    TEAL    ("#2E7E7A", Color(0xFF2E7E7A), Color(0xFF80D0CC), Color(0xFFFFFFFF)),
    GOLD    ("#A07800", Color(0xFFA07800), Color(0xFFEDD060), Color(0xFF1A1200)),
    SLATE   ("#4A5878", Color(0xFF4A5878), Color(0xFF9AAAC8), Color(0xFFFFFFFF)),
}

private fun buildLightScheme(accent: AccentColor): ColorScheme = lightColorScheme(
    primary              = accent.light,
    onPrimary            = accent.onAccent,
    primaryContainer     = accent.light.copy(alpha = 0.15f),
    onPrimaryContainer   = accent.light,
    secondary            = Color(0xFF6A7080),
    onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = Color(0xFFE8EAF0),
    onSecondaryContainer = Color(0xFF2A2D38),
    tertiary             = Color(0xFF8A96AA),
    onTertiary           = Color(0xFFFFFFFF),
    tertiaryContainer    = Color(0xFFDDE2EC),
    onTertiaryContainer  = Color(0xFF1A2030),
    background           = Color(0xFFFAF8F2),
    onBackground         = Color(0xFF1E1A16),
    surface              = Color(0xFFFAF8F2),
    onSurface            = Color(0xFF1E1A16),
    surfaceVariant       = Color(0xFFEDE8DF),
    onSurfaceVariant     = Color(0xFF4A4540),
    outline              = Color(0xFFB8B0A8),
    outlineVariant       = Color(0xFFD8D0C8),
    error                = Color(0xFFBA1A1A),
    onError              = Color(0xFFFFFFFF),
    errorContainer       = Color(0xFFFFDAD6),
    onErrorContainer     = Color(0xFF410002),
    scrim                = Color(0xFF000000),
    inverseSurface       = Color(0xFF1E1A16),
    inverseOnSurface     = Color(0xFFFAF8F2),
    inversePrimary       = accent.dark,
)

private fun buildDarkScheme(accent: AccentColor): ColorScheme = darkColorScheme(
    primary              = accent.dark,
    onPrimary            = accent.onDark ?: accent.onAccent.copy(
        red   = (accent.onAccent.red   * 0.15f).coerceAtLeast(0.05f),
        green = (accent.onAccent.green * 0.15f).coerceAtLeast(0.05f),
        blue  = (accent.onAccent.blue  * 0.15f).coerceAtLeast(0.05f),
    ),
    primaryContainer     = accent.onDark?.copy(alpha = 0.6f) ?: accent.dark.copy(alpha = 0.18f),
    onPrimaryContainer   = accent.dark,
    secondary            = Color(0xFFCCC2DC),
    onSecondary          = Color(0xFF332D41),
    secondaryContainer   = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary             = Color(0xFFEFB8C8),
    onTertiary           = Color(0xFF492532),
    tertiaryContainer    = Color(0xFF633B48),
    onTertiaryContainer  = Color(0xFFFFD8E4),
    background           = Color(0xFF141218),
    onBackground         = Color(0xFFE6E0E9),
    surface              = Color(0xFF141218),
    onSurface            = Color(0xFFE6E0E9),
    surfaceVariant       = Color(0xFF49454F),
    onSurfaceVariant     = Color(0xFFCAC4D0),
    outline              = Color(0xFF938F99),
    outlineVariant       = Color(0xFF49454F),
    error                = Color(0xFFFFB4AB),
    onError              = Color(0xFF690005),
    errorContainer       = Color(0xFF93000A),
    onErrorContainer     = Color(0xFFFFDAD6),
    scrim                = Color(0xFF000000),
    inverseSurface       = Color(0xFFE6E0E9),
    inverseOnSurface     = Color(0xFF322F37),
    inversePrimary       = accent.light,
)

data class ThemeConfig(
    val lightColorScheme: ColorScheme,
    val darkColorScheme : ColorScheme,
)

fun getThemeConfig(accent: AccentColor): ThemeConfig = ThemeConfig(
    lightColorScheme = buildLightScheme(accent),
    darkColorScheme  = buildDarkScheme(accent),
)