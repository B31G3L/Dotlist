package de.beigel.list.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.beigel.list.data.DeviceIdManager
import de.beigel.list.data.TodoList
import de.beigel.list.repository.TodoRepository
import de.beigel.list.ui.theme.AccentColor
import de.beigel.list.ui.theme.AccentColorPreferences
import de.beigel.list.ui.theme.ThemeMode
import de.beigel.list.ui.theme.ThemePreferences
import de.beigel.list.utils.HapticFeedback
import de.beigel.list.viewmodel.TodosViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfilScreen(
    lists      : List<TodoList>,
    repository : TodoRepository,
    haptic     : HapticFeedback,
    padding    : PaddingValues,
) {
    val context     = LocalContext.current
    val scope       = rememberCoroutineScope()
    val themeMode   by ThemePreferences.getThemeMode(context).collectAsState(initial = ThemeMode.SYSTEM)
    val accentColor by AccentColorPreferences.getAccentColor(context).collectAsState(initial = AccentColor.VIOLET)

    var deviceName     by remember { mutableStateOf(DeviceIdManager.getDeviceName(context)) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText     by remember { mutableStateOf(deviceName) }

    val isDark = themeMode == ThemeMode.DARK || themeMode == ThemeMode.SYSTEM

    // Pro Liste ein TodosViewModel, nur um offen/erledigt über alle Listen zu summieren
    // Pro Liste ein TodosViewModel, nur um offen/erledigt über alle Listen zu summieren
    val todosViewModels: List<TodosViewModel> = lists.map { list ->
        viewModel<TodosViewModel>(
            key     = "profil_${list.id}",
            factory = TodosViewModel.Factory(repository, list.id)
        )
    }
    val allTodos = todosViewModels.flatMap { it.uiState.collectAsStateWithLifecycle().value.todos }
    val openCount = allTodos.count { !it.isDone }
    val doneCount = allTodos.count { it.isDone }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
    ) {
        // Titel
        Text(
            text       = "Profil",
            fontSize   = 34.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.5).sp,
            color      = MaterialTheme.colorScheme.onSurface,
            modifier   = Modifier.padding(horizontal = 22.dp, vertical = 16.dp)
        )

        // Avatar + Name
        Column(
            modifier            = Modifier.fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 22.dp)
                .clickable { renameText = deviceName; showRenameDialog = true },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier         = Modifier.size(84.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = deviceName.take(1).uppercase().ifEmpty { "?" },
                    fontSize   = 32.sp,
                    fontWeight = FontWeight.Medium,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text       = deviceName,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurface,
                modifier   = Modifier.padding(top = 14.dp)
            )
            Text(
                text     = "Tippen zum Ändern",
                fontSize = 13.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp)
            )
        }

        // Statistik-Cards
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard("$openCount", "Offen",    Modifier.weight(1f))
            StatCard("$doneCount", "Erledigt", Modifier.weight(1f))
            StatCard("${lists.size}", "Listen", Modifier.weight(1f))
        }

        Spacer(Modifier.height(22.dp))

        // Settings-Gruppe 1: Toggles
        SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsToggleRow(
                icon    = Icons.Default.Notifications,
                title   = "Benachrichtigungen",
                checked = true,
                onToggle = { haptic.tick() }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 52.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
            SettingsToggleRow(
                icon     = Icons.Default.DarkMode,
                title    = "Dunkles Design",
                checked  = themeMode == ThemeMode.DARK,
                onToggle = {
                    haptic.tick()
                    scope.launch {
                        ThemePreferences.setThemeMode(
                            context,
                            if (themeMode == ThemeMode.DARK) ThemeMode.SYSTEM else ThemeMode.DARK
                        )
                    }
                }
            )
        }

        Spacer(Modifier.height(14.dp))

        // Settings-Gruppe 2: Navigation
        SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsNavRow(
                icon  = Icons.Default.Group,
                title = "Geteilte Listen",
                onClick = { haptic.tick() }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 52.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
            SettingsNavRow(
                icon  = Icons.Default.AccountCircle,
                title = "Konto verwalten",
                onClick = { haptic.tick() }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 52.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
            SettingsNavRow(
                icon  = Icons.Default.Help,
                title = "Hilfe & Feedback",
                onClick = { haptic.tick() }
            )
        }

        Spacer(Modifier.height(20.dp))

        // Abmelden
        Text(
            text     = "Abmelden",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color    = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 22.dp).clickable { haptic.heavy() }
        )

        Spacer(Modifier.height(32.dp))
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title   = { Text("Dein Name") },
            text    = {
                TextField(
                    value         = renameText,
                    onValueChange = { renameText = it },
                    singleLine    = true,
                    placeholder   = { Text("z.B. Alex") },
                    modifier      = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = renameText.isNotBlank(),
                    onClick = {
                        haptic.click()
                        DeviceIdManager.setDeviceName(context, renameText.trim())
                        deviceName = renameText.trim()
                        showRenameDialog = false
                    }
                ) { Text("Speichern") }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Abbrechen") } }
        )
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(18.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface)
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(18.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsToggleRow(
    icon    : ImageVector,
    title   : String,
    checked : Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Text(title, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun SettingsNavRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier          = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Text(title, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
    }
}