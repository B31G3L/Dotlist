package de.beigel.todo.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class AccentColor(
    val hex      : String,
    val light    : Color,
    val dark     : Color,
    val onAccent : Color
) {
    VIOLET  ("#5B3A8C", Color(0xFF5B3A8C), Color(0xFFCBB8F5), Color(0xFFFFFFFF)),
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
    onPrimary            = accent.onAccent.copy(
        red   = (accent.onAccent.red   * 0.15f).coerceAtLeast(0.05f),
        green = (accent.onAccent.green * 0.15f).coerceAtLeast(0.05f),
        blue  = (accent.onAccent.blue  * 0.15f).coerceAtLeast(0.05f),
    ),
    primaryContainer     = accent.dark.copy(alpha = 0.18f),
    onPrimaryContainer   = accent.dark,
    secondary            = Color(0xFF9AA0B0),
    onSecondary          = Color(0xFF1A1C24),
    secondaryContainer   = Color(0xFF2E3040),
    onSecondaryContainer = Color(0xFFCDD0DC),
    tertiary             = Color(0xFF8A96AA),
    onTertiary           = Color(0xFF0E1220),
    tertiaryContainer    = Color(0xFF252C3C),
    onTertiaryContainer  = Color(0xFFBDCAD5),
    background           = Color(0xFF211D17),
    onBackground         = Color(0xFFEDE8DF),
    surface              = Color(0xFF211D17),
    onSurface            = Color(0xFFEDE8DF),
    surfaceVariant       = Color(0xFF2E2920),
    onSurfaceVariant     = Color(0xFFCDC8BF),
    outline              = Color(0xFF6A6458),
    outlineVariant       = Color(0xFF453F35),
    error                = Color(0xFFFFB4AB),
    onError              = Color(0xFF690005),
    errorContainer       = Color(0xFF93000A),
    onErrorContainer     = Color(0xFFFFDAD6),
    scrim                = Color(0xFF000000),
    inverseSurface       = Color(0xFFEDE8DF),
    inverseOnSurface     = Color(0xFF171410),
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