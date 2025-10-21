package de.beigel.list.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Vordefinierte Custom Theme Varianten
 */
enum class CustomTheme {
    PLANIT,
    NEXTIME,
    LEETSPEAK,
    DAILYLIST,
    UNKNOWN
}

/**
 * Theme Konfiguration
 */
data class ThemeConfig(
    val name: String,
    val lightColorScheme: ColorScheme,
    val darkColorScheme: ColorScheme
)

// ============= PLANIT THEME (Ocean - Standard) =============

val PlanitLightTheme = lightColorScheme(
    primary = Color(0xFF00A896),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF88FBE9),
    onPrimaryContainer = Color(0xFF00211D),
    secondary = Color(0xFF536360),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD6E8E4),
    onSecondaryContainer = Color(0xFF101F1D),
    tertiary = Color(0xFF5062F0),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE0E0FF),
    onTertiaryContainer = Color(0xFF070068),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF191C1C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C1C),
    surfaceVariant = Color(0xFFDAE5E2),
    onSurfaceVariant = Color(0xFF3F4947),
    outline = Color(0xFF6F7977),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val PlanitDarkTheme = darkColorScheme(
    primary = Color(0xFF4DBCC6),
    onPrimary = Color(0xFF003730),
    primaryContainer = Color(0xFF005147),
    onPrimaryContainer = Color(0xFF88FBE9),
    secondary = Color(0xFFBCCBC8),
    onSecondary = Color(0xFF243230),
    secondaryContainer = Color(0xFF3B4947),
    onSecondaryContainer = Color(0xFFD6E8E4),
    tertiary = Color(0xFFBFC2FF),
    onTertiary = Color(0xFF2130B9),
    tertiaryContainer = Color(0xFF3946D2),
    onTertiaryContainer = Color(0xFFE0E0FF),
    background = Color(0xFF111414),
    onBackground = Color(0xFFE0E3E3),
    surface = Color(0xFF191C1C),
    onSurface = Color(0xFFE0E3E3),
    surfaceVariant = Color(0xFF3F4947),
    onSurfaceVariant = Color(0xFFBFC9C6),
    outline = Color(0xFF899391),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

// ============= NEXTIME THEME (Orange) =============

val NextimeLightTheme = lightColorScheme(
    primary = Color(0xFFFF9800),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDCC0),
    onPrimaryContainer = Color(0xFF331C00),
    secondary = Color(0xFF934B00),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBCF),
    onSecondaryContainer = Color(0xFF301300),
    tertiary = Color(0xFFFFC107),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFFFFE082),
    onTertiaryContainer = Color(0xFF201A00),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF1E1B16),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E1B16),
    surfaceVariant = Color(0xFFF2E0D0),
    onSurfaceVariant = Color(0xFF4F4539),
    outline = Color(0xFF817568),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val NextimeDarkTheme = darkColorScheme(
    primary = Color(0xFFFFB763),
    onPrimary = Color(0xFF552D00),
    primaryContainer = Color(0xFF7A4500),
    onPrimaryContainer = Color(0xFFFFDCC0),
    secondary = Color(0xFFFFB78E),
    onSecondary = Color(0xFF512400),
    secondaryContainer = Color(0xFF723500),
    onSecondaryContainer = Color(0xFFFFDBCF),
    tertiary = Color(0xFFE8C547),
    onTertiary = Color(0xFF3D3000),
    tertiaryContainer = Color(0xFF564700),
    onTertiaryContainer = Color(0xFFFFE082),
    background = Color(0xFF16130E),
    onBackground = Color(0xFFE9E1D9),
    surface = Color(0xFF1E1B16),
    onSurface = Color(0xFFE9E1D9),
    surfaceVariant = Color(0xFF4F4539),
    onSurfaceVariant = Color(0xFFD6C4B4),
    outline = Color(0xFF9C8F83),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

// ============= LEETSPEAK THEME (Purple/Violet) =============

val LeetspeakLightTheme = lightColorScheme(
    primary = Color(0xFF673AB7),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF804FB3),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF2DAFF),
    onSecondaryContainer = Color(0xFF32004B),
    tertiary = Color(0xFF00796B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF86F8E3),
    onTertiaryContainer = Color(0xFF00201B),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EB),
    onSurfaceVariant = Color(0xFF49454E),
    outline = Color(0xFF7A757F),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val LeetspeakDarkTheme = darkColorScheme(
    primary = Color(0xFFC9BCFF),
    onPrimary = Color(0xFF371777),
    primaryContainer = Color(0xFF4F318F),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFE3B9FF),
    onSecondary = Color(0xFF4E1D7B),
    secondaryContainer = Color(0xFF663595),
    onSecondaryContainer = Color(0xFFF2DAFF),
    tertiary = Color(0xFF6FE9D7),
    onTertiary = Color(0xFF003730),
    tertiaryContainer = Color(0xFF005147),
    onTertiaryContainer = Color(0xFF86F8E3),
    background = Color(0xFF111014),
    onBackground = Color(0xFFE6E1E6),
    surface = Color(0xFF1D1B20),
    onSurface = Color(0xFFE6E1E6),
    surfaceVariant = Color(0xFF49454E),
    onSurfaceVariant = Color(0xFFCAC4CF),
    outline = Color(0xFF948F99),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

// ============= DAILYLIST THEME (Lime Green) =============

val DailylistLightTheme = lightColorScheme(
    primary = Color(0xFFA5D63E),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFFD6F996),
    onPrimaryContainer = Color(0xFF1E3600),
    secondary = Color(0xFF558B2F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD6E8C0),
    onSecondaryContainer = Color(0xFF101F00),
    tertiary = Color(0xFF8BC34A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC9F07E),
    onTertiaryContainer = Color(0xFF1D3500),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1C16),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C16),
    surfaceVariant = Color(0xFFE3E4D4),
    onSurfaceVariant = Color(0xFF45483D),
    outline = Color(0xFF76796E),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val DailylistDarkTheme = darkColorScheme(
    primary = Color(0xFFB5E072),
    onPrimary = Color(0xFF2F4600),
    primaryContainer = Color(0xFF466600),
    onPrimaryContainer = Color(0xFFD6F996),
    secondary = Color(0xFFB3CC9C),
    onSecondary = Color(0xFF263319),
    secondaryContainer = Color(0xFF3C4A2E),
    onSecondaryContainer = Color(0xFFD6E8C0),
    tertiary = Color(0xFFB5E072),
    onTertiary = Color(0xFF2F4600),
    tertiaryContainer = Color(0xFF466600),
    onTertiaryContainer = Color(0xFFC9F07E),
    background = Color(0xFF12140D),
    onBackground = Color(0xFFE3E4D4),
    surface = Color(0xFF1A1C16),
    onSurface = Color(0xFFE3E4D4),
    surfaceVariant = Color(0xFF45483D),
    onSurfaceVariant = Color(0xFFC5C8B8),
    outline = Color(0xFF909387),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

// ============= UNKNOWN THEME (Red) =============

val UnknownLightTheme = lightColorScheme(
    primary = Color(0xFFD32F2F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFFC62828),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF410002),
    tertiary = Color(0xFF00B0FF),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFFBFE9FF),
    onTertiaryContainer = Color(0xFF00334A),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF1D1B1C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D1B1C),
    surfaceVariant = Color(0xFFF5DDDC),
    onSurfaceVariant = Color(0xFF524343),
    outline = Color(0xFF857373),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

val UnknownDarkTheme = darkColorScheme(
    primary = Color(0xFFFFB4A9),
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF93000A),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFFFB4A9),
    onSecondary = Color(0xFF690005),
    secondaryContainer = Color(0xFF93000A),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFF78D7FF),
    onTertiary = Color(0xFF00334A),
    tertiaryContainer = Color(0xFF004B6A),
    onTertiaryContainer = Color(0xFFBFE9FF),
    background = Color(0xFF151212),
    onBackground = Color(0xFFE7E0E1),
    surface = Color(0xFF1D1B1C),
    onSurface = Color(0xFFE7E0E1),
    surfaceVariant = Color(0xFF524343),
    onSurfaceVariant = Color(0xFFD8C2C2),
    outline = Color(0xFFA08C8C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

/**
 * Theme Map - Einfacher Zugriff auf alle Themes
 */
fun getThemeConfig(theme: CustomTheme): ThemeConfig {
    return when (theme) {
        CustomTheme.PLANIT -> ThemeConfig("PlanIt", PlanitLightTheme, PlanitDarkTheme)
        CustomTheme.NEXTIME -> ThemeConfig("NexTime", NextimeLightTheme, NextimeDarkTheme)
        CustomTheme.LEETSPEAK -> ThemeConfig("Leetspeak", LeetspeakLightTheme, LeetspeakDarkTheme)
        CustomTheme.DAILYLIST -> ThemeConfig("DailyList", DailylistLightTheme, DailylistDarkTheme)
        CustomTheme.UNKNOWN -> ThemeConfig("Unknown", UnknownLightTheme, UnknownDarkTheme)
    }
}

/**
 * Hilfsfunktion um Theme nach Namen zu bekommen
 */
fun getThemeByName(themeName: String): CustomTheme {
    return try {
        CustomTheme.valueOf(themeName.uppercase())
    } catch (e: IllegalArgumentException) {
        CustomTheme.DAILYLIST // Fallback
    }
}