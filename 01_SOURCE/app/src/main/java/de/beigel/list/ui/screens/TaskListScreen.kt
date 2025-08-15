
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.beigel.list.data.TaskEntity
import de.beigel.list.data.TaskPriority
import de.beigel.list.viewmodel.TaskViewModel
import de.beigel.list.viewmodel.DialogState
import de.beigel.list.ui.dialogs.AddEditTaskDialog
import de.beigel.list.ui.dialogs.TaskDetailsDialog
import de.beigel.list.ui.components.TaskItem
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
    val tasks by viewModel.todayTasks.collectAsState()

    val filteredTasks = if (uiState.showCompletedTasks) {
        tasks
    } else {
        tasks.filter { !it.isCompleted }
    }

    // Handle Dialog States
    when (val dialogState = uiState.dialogState) {
        is DialogState.AddTask -> {
            AddEditTaskDialog(
                onDismiss = { viewModel.setDialogState(DialogState.None) },
                onSave = { title, description, priority ->
                    viewModel.addTask(title, description, priority)
                }
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
                onEdit = { viewModel.setDialogState(DialogState.EditTask(dialogState.task)) }
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
                            text = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = {
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
                onClick = { viewModel.setDialogState(DialogState.AddTask) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aufgabe hinzufügen")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Aufgabe hinzufügen")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Progress Card
            val completedCount = tasks.count { it.isCompleted }
            val totalCount = tasks.size
            val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

            Card(
                modifier = Modifier.fillMaxWidth(),
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
                            text = "$completedCount von $totalCount",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Task List
            if (filteredTasks.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskItem(
                            task = task,
                            onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                            onEdit = { viewModel.setDialogState(DialogState.EditTask(task)) },
                            onDelete = { viewModel.deleteTask(task) },
                            onShowDetails = { viewModel.setDialogState(DialogState.TaskDetails(task)) }
                        )
                    }
                }
            } else {
                // Empty State
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
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
                            text = if (uiState.showCompletedTasks) "Keine Aufgaben für heute" else "Alle Aufgaben erledigt!",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (uiState.showCompletedTasks) "Tippe auf '+' um eine neue Aufgabe hinzuzufügen" else "Großartige Arbeit! 🎉",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}