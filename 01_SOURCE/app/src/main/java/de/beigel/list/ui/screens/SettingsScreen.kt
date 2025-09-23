package de.beigel.list.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import de.beigel.list.notification.NotificationWorker
import de.beigel.list.settings.SettingsManager
import de.beigel.list.settings.ThemeMode
import de.beigel.list.viewmodel.InteractionMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onThemeChange: ((Boolean, Boolean) -> Unit)? = null,
    onShowOnboarding: (() -> Unit)? = null // Neue Parameter

) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }

    // Settings States mit sofortiger Aktualisierung
    var notificationsEnabled by remember { mutableStateOf(settingsManager.notificationsEnabled) }
    var notificationHour by remember { mutableStateOf(settingsManager.notificationHour) }
    var notificationMinute by remember { mutableStateOf(settingsManager.notificationMinute) }
    var maxDailyTasks by remember { mutableIntStateOf(settingsManager.maxDailyTasks) }
    var autoBacklogEnabled by remember { mutableStateOf(settingsManager.autoBacklogEnabled) }
    var interactionMode by remember { mutableStateOf(settingsManager.interactionMode) }
    var useSystemTheme by remember { mutableStateOf(settingsManager.useSystemTheme) }
    var isDarkMode by remember { mutableStateOf(settingsManager.isDarkMode) }
    var enableAnimations by remember { mutableStateOf(settingsManager.enableAnimations) }
    var enableHapticFeedback by remember { mutableStateOf(settingsManager.enableHapticFeedback) }

    var showTimePicker by remember { mutableStateOf(false) }

    // Theme-Update Funktion
    fun updateTheme(newUseSystemTheme: Boolean, newIsDarkMode: Boolean) {
        useSystemTheme = newUseSystemTheme
        isDarkMode = newIsDarkMode
        settingsManager.updateThemeSettings(newUseSystemTheme, newIsDarkMode)
        onThemeChange?.invoke(newUseSystemTheme, newIsDarkMode)
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            notificationsEnabled = true
            settingsManager.notificationsEnabled = true
            NotificationWorker.scheduleDaily(context, notificationHour, notificationMinute)
        }
    }

    // Check notification permission
    val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialHour = notificationHour,
            initialMinute = notificationMinute,
            onTimeSelected = { hour, minute ->
                notificationHour = hour
                notificationMinute = minute
                settingsManager.notificationHour = hour
                settingsManager.notificationMinute = minute

                if (notificationsEnabled) {
                    NotificationWorker.scheduleDaily(context, hour, minute)
                }
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Einstellungen",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ========== Aufgabenverwaltung ==========
            SettingsGroup(
                title = "Aufgabenverwaltung",
                icon = Icons.Default.Task
            ) {
                // Maximale tägliche Aufgaben
                SettingsSlider(
                    title = "Maximale tägliche Aufgaben",
                    subtitle = "Anzahl der Aufgaben, die täglich angezeigt werden",
                    value = maxDailyTasks,
                    valueRange = 1f..20f,
                    onValueChange = { value ->
                        maxDailyTasks = value.toInt()
                        settingsManager.maxDailyTasks = value.toInt()
                    }
                )

                // Auto-Backlog
                SettingsSwitch(
                    title = "Automatisches Backlog",
                    subtitle = "Tägliche Liste automatisch aus Backlog auffüllen",
                    checked = autoBacklogEnabled,
                    onCheckedChange = { enabled ->
                        autoBacklogEnabled = enabled
                        settingsManager.autoBacklogEnabled = enabled
                    }
                )
            }
            onShowOnboarding?.let { callback ->
                OnboardingSettingsSection(
                    onShowOnboarding = callback
                )
            }
            // ========== Interaktion ==========
            SettingsGroup(
                title = "Interaktion",
                icon = Icons.Default.TouchApp
            ) {
                // Interaktionsmodus
                SettingsSelection(
                    title = "Interaktionsmodus",
                    subtitle = when (interactionMode) {
                        InteractionMode.MINIMAL -> "Minimal - Nur Tipp & Gedrückt halten"
                        InteractionMode.CONTEXT_MENU -> "Kontextmenü - Gedrückt halten für Optionen"
                        InteractionMode.SELECTION -> "Auswahl - Multi-Select Modus"
                    },
                    options = InteractionMode.values().map { mode ->
                        when (mode) {
                            InteractionMode.MINIMAL -> "Minimal"
                            InteractionMode.CONTEXT_MENU -> "Kontextmenü"
                            InteractionMode.SELECTION -> "Auswahl"
                        }
                    },
                    selectedIndex = InteractionMode.values().indexOf(interactionMode),
                    onSelectionChange = { index ->
                        interactionMode = InteractionMode.values()[index]
                        settingsManager.interactionMode = interactionMode
                    }
                )

                // Animationen
                SettingsSwitch(
                    title = "Animationen",
                    subtitle = "Sanfte Übergänge und Animationen aktivieren",
                    checked = enableAnimations,
                    onCheckedChange = { enabled ->
                        enableAnimations = enabled
                        settingsManager.enableAnimations = enabled
                    }
                )

                // Haptisches Feedback
                SettingsSwitch(
                    title = "Haptisches Feedback",
                    subtitle = "Vibration bei Interaktionen",
                    checked = enableHapticFeedback,
                    onCheckedChange = { enabled ->
                        enableHapticFeedback = enabled
                        settingsManager.enableHapticFeedback = enabled
                    }
                )
            }

            // ========== Design - Verbessert ==========
            SettingsGroup(
                title = "Design",
                icon = Icons.Default.Palette
            ) {
                // Aktueller Theme-Status Anzeige
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Aktuell: ${
                                when {
                                    useSystemTheme -> "System-Theme"
                                    isDarkMode -> "Dunkler Modus"
                                    else -> "Heller Modus"
                                }
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // System Theme Switch
                SettingsSwitch(
                    title = "System-Theme verwenden",
                    subtitle = "Automatisch zwischen Hell und Dunkel wechseln",
                    checked = useSystemTheme,
                    onCheckedChange = { enabled ->
                        updateTheme(enabled, isDarkMode)
                    }
                )

                // Manual Dark Mode (nur wenn System-Theme deaktiviert)
                AnimatedVisibility(
                    visible = !useSystemTheme,
                    enter = expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))

                        SettingsSwitch(
                            title = "Dunkler Modus",
                            subtitle = "Dunkles Design aktivieren",
                            checked = isDarkMode,
                            onCheckedChange = { enabled ->
                                updateTheme(useSystemTheme, enabled)
                            }
                        )


                    }
                }
            }

            // ========== Benachrichtigungen ==========
            SettingsGroup(
                title = "Benachrichtigungen",
                icon = Icons.Default.Notifications
            ) {
                // Benachrichtigungen aktivieren
                SettingsSwitch(
                    title = "Tägliche Erinnerungen",
                    subtitle = if (hasNotificationPermission) {
                        "Erhalte tägliche Benachrichtigungen für offene Aufgaben"
                    } else {
                        "Berechtigung erforderlich"
                    },
                    checked = notificationsEnabled && hasNotificationPermission,
                    onCheckedChange = { enabled ->
                        if (enabled && !hasNotificationPermission) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            notificationsEnabled = enabled
                            settingsManager.notificationsEnabled = enabled

                            if (enabled) {
                                NotificationWorker.scheduleDaily(context, notificationHour, notificationMinute)
                            } else {
                                NotificationWorker.cancelNotifications(context)
                            }
                        }
                    }
                )

                // Benachrichtigungszeit (nur wenn aktiviert)
                AnimatedVisibility(
                    visible = notificationsEnabled && hasNotificationPermission,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SettingsClickable(
                        title = "Benachrichtigungszeit",
                        subtitle = String.format("%02d:%02d", notificationHour, notificationMinute),
                        onClick = { showTimePicker = true }
                    )
                }
            }

            // ========== App-Information ==========
            SettingsGroup(
                title = "App-Information",
                icon = Icons.Default.Info
            ) {
                InfoRow(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = "1.0.0"
                )

                InfoRow(
                    icon = Icons.Default.Build,
                    title = "Entwickelt mit",
                    subtitle = "Jetpack Compose & Material 3"
                )

                InfoRow(
                    icon = Icons.Default.Storage,
                    title = "Datenspeicherung",
                    subtitle = "Lokal auf dem Gerät (Room Database)"
                )

                InfoRow(
                    icon = Icons.Default.Security,
                    title = "Datenschutz",
                    subtitle = "Alle Daten bleiben auf deinem Gerät"
                )
            }
        }
    }
}

@Composable
fun SettingsGroup(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            content()
        }
    }
}

@Composable
fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun SettingsSlider(
    title: String,
    subtitle: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = (valueRange.endInclusive - valueRange.start).toInt() - 1,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun SettingsClickable(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun SettingsSelection(
    title: String,
    subtitle: String,
    options: List<String>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(top = 8.dp)
            ) {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectionChange(index)
                                expanded = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedIndex == index,
                            onClick = {
                                onSelectionChange(index)
                                expanded = false
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
@Composable
fun OnboardingSettingsSection(
    onShowOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsGroup(
        title = "Hilfe & Einführung",
        icon = Icons.Default.Help
    ) {
        SettingsClickable(
            title = "Onboarding erneut anzeigen",
            subtitle = "Die Einführung zur App noch einmal durchgehen",
            onClick = onShowOnboarding
        )

        InfoRow(
            icon = Icons.Default.School,
            title = "Erste Schritte",
            subtitle = "Lerne alle Features der App kennen"
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Benachrichtigungszeit auswählen",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            TimePicker(
                state = timePickerState,
                colors = TimePickerDefaults.colors(
                    timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                    timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(timePickerState.hour, timePickerState.minute)
                }
            ) {
                Text("OK", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}