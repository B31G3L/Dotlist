package de.beigel.list.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
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
import de.beigel.list.ui.theme.ThemeMode
import de.beigel.list.ui.animations.AnimatedListItem
import de.beigel.list.ui.animations.BouncyButton
import de.beigel.list.ui.utils.rememberHapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSettingsScreen(
    onNavigateBack: () -> Unit,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChanged: (ThemeMode) -> Unit = {}
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val hapticFeedback = rememberHapticFeedback()

    var notificationsEnabled by remember { mutableStateOf(settingsManager.notificationsEnabled) }
    var notificationHour by remember { mutableStateOf(settingsManager.notificationHour) }
    var notificationMinute by remember { mutableStateOf(settingsManager.notificationMinute) }
    var maxDailyTasks by remember { mutableIntStateOf(settingsManager.maxDailyTasks) }
    var autoBacklogEnabled by remember { mutableStateOf(settingsManager.autoBacklogEnabled) }
    var showTimePicker by remember { mutableStateOf(false) }
    var hapticFeedbackEnabled by remember { mutableStateOf(true) }
    var animationsEnabled by remember { mutableStateOf(true) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            notificationsEnabled = true
            settingsManager.notificationsEnabled = true
            NotificationWorker.scheduleDaily(context, notificationHour, notificationMinute)
            hapticFeedback.taskAdded()
        } else {
            hapticFeedback.error()
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
                hapticFeedback.buttonPress()
            },
            onDismiss = {
                showTimePicker = false
                hapticFeedback.buttonPress()
            }
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
                    BouncyButton(
                        onClick = {
                            onNavigateBack()
                            hapticFeedback.buttonPress()
                        }
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
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
            // Theme Settings Card
            AnimatedListItem(visible = true, delay = 0) {
                EnhancedSettingsCard(
                    title = "Darstellung",
                    icon = Icons.Default.Palette,
                    description = "Personalisiere das Aussehen deiner App"
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Theme Mode Selection
                        SettingsSection(
                            title = "Design-Modus",
                            description = "Wähle dein bevorzugtes Farbschema"
                        ) {
                            ThemeModeSelector(
                                selectedMode = themeMode,
                                onModeSelected = { mode ->
                                    onThemeModeChanged(mode)
                                    hapticFeedback.buttonPress()
                                }
                            )
                        }

                        HorizontalDivider()

                        // Animations Toggle
                        EnhancedSettingsToggle(
                            title = "Animationen",
                            description = "Aktiviere schöne Übergangseffekte",
                            icon = Icons.Default.Animation,
                            checked = animationsEnabled,
                            onCheckedChange = {
                                animationsEnabled = it
                                hapticFeedback.buttonPress()
                            }
                        )

                        // Haptic Feedback Toggle
                        EnhancedSettingsToggle(
                            title = "Haptisches Feedback",
                            description = "Vibrationen bei Interaktionen",
                            icon = Icons.Default.Vibration,
                            checked = hapticFeedbackEnabled,
                            onCheckedChange = {
                                hapticFeedbackEnabled = it
                                if (it) hapticFeedback.taskCompleted()
                            }
                        )
                    }
                }
            }

            // Task Management Card
            AnimatedListItem(visible = true, delay = 100) {
                EnhancedSettingsCard(
                    title = "Aufgabenverwaltung",
                    icon = Icons.Default.Task,
                    description = "Organisiere deine täglichen Aufgaben"
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Max Daily Tasks
                        EnhancedSettingsNumberPicker(
                            title = "Maximale tägliche Aufgaben",
                            description = "Anzahl der Aufgaben, die täglich angezeigt werden",
                            icon = Icons.Default.FormatListNumbered,
                            value = maxDailyTasks,
                            onValueChange = {
                                maxDailyTasks = it
                                settingsManager.maxDailyTasks = it
                                hapticFeedback.buttonPress()
                            },
                            range = 1..20
                        )

                        HorizontalDivider()

                        // Auto Backlog Toggle
                        EnhancedSettingsToggle(
                            title = "Automatisches Backlog",
                            description = "Tägliche Liste automatisch aus Backlog auffüllen",
                            icon = Icons.Default.AutoMode,
                            checked = autoBacklogEnabled,
                            onCheckedChange = {
                                autoBacklogEnabled = it
                                settingsManager.autoBacklogEnabled = it
                                hapticFeedback.buttonPress()
                            }
                        )
                    }
                }
            }

            // Notifications Card
            AnimatedListItem(visible = true, delay = 200) {
                EnhancedSettingsCard(
                    title = "Benachrichtigungen",
                    icon = Icons.Default.Notifications,
                    description = "Bleib auf dem Laufenden mit deinen Aufgaben"
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Notifications Toggle
                        EnhancedSettingsToggle(
                            title = "Tägliche Erinnerungen",
                            description = "Erhalte Benachrichtigungen für offene Aufgaben",
                            icon = Icons.Default.NotificationsActive,
                            checked = notificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    if (hasNotificationPermission) {
                                        notificationsEnabled = true
                                        settingsManager.notificationsEnabled = true
                                        NotificationWorker.scheduleDaily(context, notificationHour, notificationMinute)
                                        hapticFeedback.taskAdded()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    notificationsEnabled = false
                                    settingsManager.notificationsEnabled = false
                                    hapticFeedback.buttonPress()
                                }
                            }
                        )

                        AnimatedVisibility(
                            visible = notificationsEnabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                HorizontalDivider()

                                SettingsClickableItem(
                                    title = "Benachrichtigungszeit",
                                    description = "${String.format("%02d", notificationHour)}:${String.format("%02d", notificationMinute)} Uhr",
                                    icon = Icons.Default.Schedule,
                                    onClick = {
                                        showTimePicker = true
                                        hapticFeedback.buttonPress()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // App Info Card
            AnimatedListItem(visible = true, delay = 300) {
                EnhancedSettingsCard(
                    title = "App-Information",
                    icon = Icons.Default.Info,
                    description = "Details über Daily List"
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EnhancedInfoRow(
                            icon = Icons.Default.Info,
                            title = "Version",
                            subtitle = "1.0.0",
                            badge = "Neu"
                        )

                        EnhancedInfoRow(
                            icon = Icons.Default.Build,
                            title = "Entwickelt mit",
                            subtitle = "Jetpack Compose & Material 3"
                        )

                        EnhancedInfoRow(
                            icon = Icons.Default.Storage,
                            title = "Datenspeicherung",
                            subtitle = "Lokal auf dem Gerät (Room Database)"
                        )

                        EnhancedInfoRow(
                            icon = Icons.Default.Security,
                            title = "Datenschutz",
                            subtitle = "Alle Daten bleiben auf deinem Gerät"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedSettingsCard(
    title: String,
    icon: ImageVector,
    description: String = "",
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (description.isNotEmpty()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            content()
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    description: String? = null,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )
        }

        content()
    }
}

@Composable
fun EnhancedSettingsToggle(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
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
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun EnhancedSettingsNumberPicker(
    title: String,
    description: String,
    icon: ImageVector,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
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
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (value > range.first) {
                                onValueChange(value - 1)
                            }
                        },
                        enabled = value > range.first
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Weniger",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(
                        onClick = {
                            if (value < range.last) {
                                onValueChange(value + 1)
                            }
                        },
                        enabled = value < range.last
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Mehr",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsClickableItem(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Öffnen",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ThemeModeSelector(
    selectedMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ThemeMode.values().forEach { mode ->
            val isSelected = selectedMode == mode
            val (icon, label) = when (mode) {
                ThemeMode.LIGHT -> Icons.Default.LightMode to "Hell"
                ThemeMode.DARK -> Icons.Default.DarkMode to "Dunkel"
                ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness to "System"
            }

            FilterChip(
                selected = isSelected,
                onClick = { onModeSelected(mode) },
                label = { Text(label) },
                leadingIcon = {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun EnhancedInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
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

        badge?.let {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
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