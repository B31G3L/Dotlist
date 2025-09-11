package de.beigel.list.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.beigel.list.data.TaskEntity
import de.beigel.list.data.TaskPriority
import de.beigel.list.viewmodel.TaskViewModel
import de.beigel.list.viewmodel.DialogState
import de.beigel.list.viewmodel.ViewType
import de.beigel.list.ui.dialogs.AddEditTaskDialog
import de.beigel.list.ui.dialogs.TaskDetailsDialog
import de.beigel.list.ui.components.TaskItem
import de.beigel.list.settings.SettingsManager
import de.beigel.list.ui.components.SwipeableTaskItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val todayTasks by viewModel.todayTasks.collectAsState()
    val backlogTasks by viewModel.backlogTasks.collectAsState()
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }

    val currentTasks = when (uiState.currentView) {
        ViewType.DAILY -> if (uiState.showCompletedTasks) todayTasks else todayTasks.filter { !it.isCompleted }
        ViewType.BACKLOG -> if (uiState.showCompletedTasks) backlogTasks else backlogTasks.filter { !it.isCompleted }
    }

    // Handle Dialog States
    when (val dialogState = uiState.dialogState) {
        is DialogState.AddTask -> {
            AddEditTaskDialog(
                onDismiss = { viewModel.setDialogState(DialogState.None) },
                onSave = { title, description, priority ->
                    viewModel.addTask(title, description, priority, dialogState.addToDaily)
                },
                showDestinationChoice = true,
                initialAddToDaily = dialogState.addToDaily
            )
        }
        is DialogState.EditTask -> {
            AddEditTaskDialog(
                isEditing = true,
                initialTask = dialogState.task,
                onDismiss = { viewModel.setDialogState(DialogState.None) },
                onSave = { title, description, priority ->
                    viewModel.updateTask(
                        dialogState.task.copy(
                            title = title,
                            description = description,
                            priority = priority
                        )
                    )
                }
            )
        }
        is DialogState.TaskDetails -> {
            TaskDetailsDialog(
                task = dialogState.task,
                onDismiss = { viewModel.setDialogState(DialogState.None) },
                onEdit = { viewModel.setDialogState(DialogState.EditTask(dialogState.task)) },
                onMoveToDaily = { viewModel.moveTaskToDaily(dialogState.task) },
                onMoveToBacklog = { viewModel.moveTaskToBacklog(dialogState.task) }
            )
        }
        DialogState.None -> { /* No dialog */ }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Daily List",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (uiState.currentView) {
                                ViewType.DAILY -> "${todayTasks.size}/${settingsManager.maxDailyTasks} heute"
                                ViewType.BACKLOG -> "${backlogTasks.size} im Backlog"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = {
                    // Fill from Backlog Button (nur bei Daily View)
                    if (uiState.currentView == ViewType.DAILY && backlogTasks.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                viewModel.fillFromBacklog(settingsManager.maxDailyTasks)
                            }
                        ) {
                            Icon(
                                Icons.Default.PlaylistAdd,
                                contentDescription = "Aus Backlog auffüllen",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "Historie",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Einstellungen",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { viewModel.setShowCompleted(!uiState.showCompletedTasks) }
                    ) {
                        Icon(
                            if (uiState.showCompletedTasks) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (uiState.showCompletedTasks) "Erledigte ausblenden" else "Erledigte anzeigen",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.setDialogState(
                        DialogState.AddTask(addToDaily = uiState.currentView == ViewType.DAILY)
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aufgabe hinzufügen")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    when (uiState.currentView) {
                        ViewType.DAILY -> "Zur Liste"
                        ViewType.BACKLOG -> "Zum Backlog"
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = uiState.currentView.ordinal,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Tab(
                    selected = uiState.currentView == ViewType.DAILY,
                    onClick = { viewModel.setCurrentView(ViewType.DAILY) },
                    text = { Text("Heute") }
                )
                Tab(
                    selected = uiState.currentView == ViewType.BACKLOG,
                    onClick = { viewModel.setCurrentView(ViewType.BACKLOG) },
                    text = { Text("Backlog") }
                )
            }

            // Content
            when (uiState.currentView) {
                ViewType.DAILY -> DailyTasksContent(
                    tasks = currentTasks,
                    totalTasks = todayTasks,
                    maxTasks = settingsManager.maxDailyTasks,
                    viewModel = viewModel
                )
                ViewType.BACKLOG -> BacklogTasksContent(
                    tasks = currentTasks,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun DailyTasksContent(
    tasks: List<TaskEntity>,
    totalTasks: List<TaskEntity>,
    maxTasks: Int,
    viewModel: TaskViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Progress Card
        val completedCount = totalTasks.count { it.isCompleted }
        val totalCount = totalTasks.size
        val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Fortschritt",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$completedCount von $totalCount (max. $maxTasks)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )

                // Warnung wenn Limit überschritten
                if (totalCount > maxTasks) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ Du hast ${totalCount - maxTasks} Aufgaben über dem Limit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Task List or Empty State
        if (tasks.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f), // ✅ FIXED: Entfernt die zusätzlichen Klammern
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    SwipeableTaskItem(
                        task = task,
                        onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                        onEdit = { viewModel.setDialogState(DialogState.EditTask(task)) },
                        onDelete = { viewModel.deleteTask(task) },
                        onShowDetails = { viewModel.setDialogState(DialogState.TaskDetails(task)) },
                        showMoveToBacklog = true,
                        onMoveToBacklog = { viewModel.moveTaskToBacklog(task) }
                    )
                }
            }
        } else {
            // Empty State
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // ✅ FIXED: Entfernt die zusätzlichen Klammern
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Task,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Keine Aufgaben für heute",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Tippe auf '+' um eine neue Aufgabe hinzuzufügen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun BacklogTasksContent(
    tasks: List<TaskEntity>,
    viewModel: TaskViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (tasks.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f), // ✅ FIXED: Entfernt die zusätzlichen Klammern
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    SwipeableTaskItem(
                        task = task,
                        onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                        onEdit = { viewModel.setDialogState(DialogState.EditTask(task)) },
                        onDelete = { viewModel.deleteTask(task) },
                        onShowDetails = { viewModel.setDialogState(DialogState.TaskDetails(task)) },
                        showMoveToBacklog = true,
                        onMoveToBacklog = { viewModel.moveTaskToBacklog(task) }
                    )
                }
            }
        } else {
            // Empty State
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // ✅ FIXED: Entfernt die zusätzlichen Klammern
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Task,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Backlog ist leer",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Alle zusätzlichen Aufgaben werden hier gespeichert",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}