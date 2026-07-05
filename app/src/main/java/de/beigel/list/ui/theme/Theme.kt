package de.beigel.list.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val AppTypography = Typography(
    displayLarge  = TextStyle(fontSize = 57.sp, lineHeight = 64.sp,  fontWeight = FontWeight.Normal),
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 40.sp,  fontWeight = FontWeight.Bold),
    titleLarge    = TextStyle(fontSize = 22.sp, lineHeight = 28.sp,  fontWeight = FontWeight.SemiBold),
    titleMedium   = TextStyle(fontSize = 16.sp, lineHeight = 24.sp,  fontWeight = FontWeight.SemiBold),
    titleSmall    = TextStyle(fontSize = 14.sp, lineHeight = 20.sp,  fontWeight = FontWeight.SemiBold),
    bodyLarge     = TextStyle(fontSize = 16.sp, lineHeight = 24.sp,  fontWeight = FontWeight.Normal),
    bodyMedium    = TextStyle(fontSize = 14.sp, lineHeight = 20.sp,  fontWeight = FontWeight.Normal),
    bodySmall     = TextStyle(fontSize = 12.sp, lineHeight = 16.sp,  fontWeight = FontWeight.Normal),
    labelMedium   = TextStyle(fontSize = 12.sp, lineHeight = 16.sp,  fontWeight = FontWeight.Medium),
    labelSmall    = TextStyle(fontSize = 11.sp, lineHeight = 16.sp,  fontWeight = FontWeight.Medium),
)

@Composable
fun TodoSharedTheme(
    darkTheme   : Boolean     = isSystemInDarkTheme(),
    accentColor : AccentColor = AccentColor.VIOLET,
    content     : @Composable () -> Unit
) {
    val config      = getThemeConfig(accentColor)
    val colorScheme = if (darkTheme) config.darkColorScheme else config.lightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content     = content
    )
}

val ListColors = listOf(
    "#D0BCFF", "#7FD1BE", "#FFB4AB", "#FFD8A8", "#5B8DEF", "#E06FA0"
)

/**
 * Feste Farben je Priorität, unabhängig vom gewählten Akzent –
 * so bleiben "Hoch" (koralle), "Mittel" (violett) und "Niedrig" (grün)
 * immer wiedererkennbar, wie im Design-Mockup.
 */
fun priorityColor(priority: de.beigel.list.data.Priority): androidx.compose.ui.graphics.Color = when (priority) {
    de.beigel.list.data.Priority.HOCH    -> androidx.compose.ui.graphics.Color(0xFFF2B8B5)
    de.beigel.list.data.Priority.MITTEL  -> androidx.compose.ui.graphics.Color(0xFFCCC2DC)
    de.beigel.list.data.Priority.NIEDRIG -> androidx.compose.ui.graphics.Color(0xFF9FD8B5)
}