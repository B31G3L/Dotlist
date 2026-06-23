package de.beigel.todo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import de.beigel.todo.ui.theme.AccentColor
import de.beigel.todo.ui.theme.AccentColorPreferences
import de.beigel.todo.ui.theme.ThemeMode
import de.beigel.todo.ui.theme.ThemePreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    val themeMode   by ThemePreferences.getThemeMode(context).collectAsState(initial = ThemeMode.SYSTEM)
    val accentColor by AccentColorPreferences.getAccentColor(context).collectAsState(initial = AccentColor.VIOLET)

    var activeSheet by remember { mutableStateOf<SettingsSheet?>(null) }

    val themeSummary = when (themeMode) {
        ThemeMode.SYSTEM -> "Systemeinstellung"
        ThemeMode.LIGHT  -> "Hell"
        ThemeMode.DARK   -> "Dunkel"
    }
    val accentSummary = when (accentColor) {
        AccentColor.VIOLET  -> "Violett"
        AccentColor.ORANGE  -> "Orange"
        AccentColor.SAGE    -> "Salbei"
        AccentColor.CRIMSON -> "Karmesin"
        AccentColor.TEAL    -> "Petrol"
        AccentColor.GOLD    -> "Gold"
        AccentColor.SLATE   -> "Schiefer"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsListItem(
                title   = "Farbschema",
                value   = accentSummary,
                dot     = accentColor.light,
                onClick = { activeSheet = SettingsSheet.ACCENT }
            )
            SettingsDivider()
            SettingsListItem(
                title   = "Design",
                value   = themeSummary,
                onClick = { activeSheet = SettingsSheet.THEME }
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    activeSheet?.let { sheet ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState       = sheetState,
            containerColor   = MaterialTheme.colorScheme.surface,
            shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (sheet) {

                    SettingsSheet.ACCENT -> {
                        Text(
                            "Farbschema",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            "Wähle deine Akzentfarbe. Der neutrale Hintergrund bleibt in Hell und Dunkel gleich.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val accentLabels = mapOf(
                            AccentColor.VIOLET  to "Violett",
                            AccentColor.ORANGE  to "Orange",
                            AccentColor.SAGE    to "Salbei",
                            AccentColor.CRIMSON to "Karmesin",
                            AccentColor.TEAL    to "Petrol",
                            AccentColor.GOLD    to "Gold",
                            AccentColor.SLATE   to "Schiefer",
                        )

                        val rows = AccentColor.values().toList().chunked(4)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            rows.forEach { row ->
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    row.forEach { accent ->
                                        val isSelected = accentColor == accent
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape    = RoundedCornerShape(12.dp),
                                            color    = if (isSelected) accent.light.copy(alpha = 0.08f)
                                            else MaterialTheme.colorScheme.surface,
                                            border   = BorderStroke(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) accent.light
                                                else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            onClick  = {
                                                scope.launch { AccentColorPreferences.setAccentColor(context, accent) }
                                            }
                                        ) {
                                            Column(
                                                modifier            = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier         = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(accent.light),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isSelected) {
                                                        Icon(
                                                            Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint     = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text       = accentLabels[accent] ?: "",
                                                    style      = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color      = if (isSelected) accent.light
                                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines   = 1,
                                                    fontSize   = 10.sp
                                                )
                                            }
                                        }
                                    }
                                    repeat(4 - row.size) { Box(modifier = Modifier.weight(1f)) }
                                }
                            }
                        }
                    }

                    SettingsSheet.THEME -> {
                        Text(
                            "Design",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.padding(top = 4.dp)
                        )
                        listOf(
                            Triple(ThemeMode.SYSTEM, "Systemeinstellung", Icons.Default.BrightnessAuto),
                            Triple(ThemeMode.LIGHT,  "Hell",              Icons.Default.Brightness7),
                            Triple(ThemeMode.DARK,   "Dunkel",            Icons.Default.Brightness4),
                        ).forEach { (mode, label, icon) ->
                            val isSelected = themeMode == mode
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape    = MaterialTheme.shapes.medium,
                                color    = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                else MaterialTheme.colorScheme.surface,
                                border   = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                ),
                                onClick  = { scope.launch { ThemePreferences.setThemeMode(context, mode) } }
                            ) {
                                Row(
                                    modifier              = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment     = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector        = icon,
                                            contentDescription = null,
                                            modifier           = Modifier.size(20.dp),
                                            tint               = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text       = label,
                                            style      = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color      = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    RadioButton(
                                        selected  = isSelected,
                                        onClick   = { scope.launch { ThemePreferences.setThemeMode(context, mode) } }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class SettingsSheet { ACCENT, THEME }

@Composable
private fun SettingsListItem(
    title   : String,
    value   : String,
    dot     : Color? = null,
    onClick : () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(title, style = MaterialTheme.typography.bodyLarge)
        },
        trailingContent = {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text  = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (dot != null) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(dot)
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier  = Modifier.padding(horizontal = 16.dp),
        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        thickness = 0.5.dp
    )
}