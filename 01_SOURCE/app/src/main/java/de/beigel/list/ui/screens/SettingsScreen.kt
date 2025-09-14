package de.beigel.list.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import de.beigel.list.notification.NotificationWorker
import de.beigel.list.settings.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    var notificationsEnabled by remember { mutableStateOf(settingsManager.notificationsEnabled) }
    var notificationHour by remember { mutableStateOf(settingsManager.notificationHour) }
    var notificationMinute by remember { mutableStateOf(settingsManager.notificationMinute) }
    var maxDailyTasks by remember { mutableIntStateOf(settingsManager.maxDailyTasks) }
    var autoBacklogEnabled by remember { mutableStateOf(settingsManager.autoBacklogEnabled) }
    var showTimePicker by remember { mutableStateOf(false) }


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
                .padding(16.dp)
        ) {
            // ========== NEU: Backlog Einstellungen ==========
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Aufgabenverwaltung",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Maximale tägliche Aufgaben
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Maximale tägliche Aufgaben",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Anzahl der Aufgaben, die täglich angezeigt werden",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (maxDailyTasks > 1) {
                                        maxDailyTasks--
                                        settingsManager.maxDailyTasks = maxDailyTasks
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Weniger")
                            }

                            Text(
                                text = maxDailyTasks.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            IconButton(
                                onClick = {
                                    if (maxDailyTasks < 20) {
                                        maxDailyTasks++
                                        settingsManager.maxDailyTasks = maxDailyTasks
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Mehr")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Auto-Backlog
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Automatisches Backlog",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Tägliche Liste automatisch aus Backlog auffüllen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        Switch(
                            checked = autoBacklogEnabled,
                            onCheckedChange = { enabled ->
                                autoBacklogEnabled = enabled
                                settingsManager.autoBacklogEnabled = enabled
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Info Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "App-Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

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
                }
            }
        }
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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