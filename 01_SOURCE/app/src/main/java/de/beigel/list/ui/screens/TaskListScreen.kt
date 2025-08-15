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
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    onNavigateToHistory: () -> Unit
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Daily List",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFF3C3C3C),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF3C3C3C).copy(alpha = 0.7f)
                )
            }

            Row {
                IconButton(onClick = onNavigateToHistory) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = "Historie",
                        tint = Color(0xFF009966)
                    )
                }

                IconButton(
                    onClick = { viewModel.setShowCompleted(!uiState.showCompletedTasks) }
                ) {
                    Icon(
                        if (uiState.showCompletedTasks) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (uiState.showCompletedTasks) "Erledigte ausblenden" else "Erledigte anzeigen",
                        tint = Color(0xFF009966)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress
        val completedCount = tasks.count { it.isCompleted }
        val totalCount = tasks.size
        val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF009966).copy(alpha = 0.1f))
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
                        color = Color(0xFF3C3C3C)
                    )
                    Text(
                        text = "$completedCount von $totalCount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF009966),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF009966),
                    trackColor = Color(0xFF009966).copy(alpha = 0.3f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Task List
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

            if (filteredTasks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Task,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color(0xFF009966).copy(alpha = 0.5f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (uiState.showCompletedTasks) "Keine Aufgaben für heute" else "Alle Aufgaben erledigt!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF3C3C3C).copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // Add Task Button
        ExtendedFloatingActionButton(
            onClick = { viewModel.setDialogState(DialogState.AddTask) },
            modifier = Modifier.align(Alignment.End),
            containerColor = Color(0xFF009966),
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Aufgabe hinzufügen")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Aufgabe hinzufügen")
        }
    }
}