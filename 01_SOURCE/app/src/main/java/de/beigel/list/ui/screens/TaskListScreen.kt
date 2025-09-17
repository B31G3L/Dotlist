package de.beigel.list.ui.screens
import de.beigel.list.ui.components.*

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import de.beigel.list.viewmodel.*
import de.beigel.list.ui.dialogs.AddTaskBottomSheet
import de.beigel.list.ui.dialogs.TaskDetailsDialog
import de.beigel.list.ui.components.*
import de.beigel.list.settings.SettingsManager
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
    val filteredTodayTasks by viewModel.filteredTodayTasks.collectAsState()
    val filteredBacklogTasks by viewModel.filteredBacklogTasks.collectAsState()
    val updatingTaskIds by viewModel.updatingTaskIds.collectAsState()

    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }

    val currentTasks = when (uiState.currentView) {
        ViewType.DAILY -> filteredTodayTasks
        ViewType.BACKLOG -> filteredBacklogTasks
    }

    val allCurrentTasks = when (uiState.currentView) {
        ViewType.DAILY -> todayTasks
        ViewType.BACKLOG -> backlogTasks
    }

    // Handle Dialog States
    when (val dialogState = uiState.dialogState) {
        is DialogState.AddTask -> {
            AddTaskBottomSheet(
                isVisible = true,
                onDismiss = { viewModel.setDialogState(DialogState.None) },
                onSave = { title, description, priority, addToDaily ->
                    viewModel.addTask(title, description, priority, addToDaily)
                },
                showDestinationChoice = true,
                initialAddToDaily = dialogState.addToDaily
            )
        }
        is DialogState.EditTask -> {
            AddTaskBottomSheet(
                isVisible = true,
                isEditing = true,
                initialTask = dialogState.task,
                onDismiss = { viewModel.setDialogState(DialogState.None) },
                onSave = { title, description, priority, _ ->
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
        is DialogState.ContextMenu -> { /* Handled by TaskItem itself */ }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (uiState.isSelectionMode) {
                    SelectionTopBar(
                        selectedCount = uiState.selectedTaskIds.size,
                        totalCount = allCurrentTasks.size,
                        onClearSelection = { viewModel.clearSelection() },
                        onSelectAll = {
                            allCurrentTasks.forEach { task ->
                                if (task.id !in uiState.selectedTaskIds) {
                                    viewModel.toggleTaskSelection(task.id)
                                }
                            }
                        }
                    )
                } else {
                    // Use the new menu version instead of the old one
                    MainTopBar( // or just MainTopBar for simpler version
                        uiState = uiState,
                        todayTasksCount = todayTasks.size,
                        backlogTasksCount = backlogTasks.size,
                        maxDailyTasks = settingsManager.maxDailyTasks,
                        onNavigateToHistory = onNavigateToHistory,
                        onNavigateToSettings = onNavigateToSettings,
                        onToggleCompleted = { viewModel.setShowCompleted(!uiState.showCompletedTasks) },
                        onFillFromBacklog = { viewModel.fillFromBacklog(settingsManager.maxDailyTasks) },
                        onInteractionModeChange = { viewModel.setInteractionMode(it) }
                    )
                }
            },
            floatingActionButton = {
                if (uiState.isSelectionMode) {
                    SelectionFloatingActionMenu(
                        selectedTasks = viewModel.getSelectedTasks(),
                        onEditSelected = {
                            val selectedTask = viewModel.getSelectedTasks().firstOrNull()
                            selectedTask?.let {
                                viewModel.setDialogState(DialogState.EditTask(it))
                            }
                        },
                        onDeleteSelected = { viewModel.deleteSelectedTasks() },
                        onMoveToDaily = { viewModel.moveSelectedTasksToDaily() },
                        onMoveToBacklog = { viewModel.moveSelectedTasksToBacklog() },
                        onClearSelection = { viewModel.clearSelection() }
                    )
                } else {
                    FloatingActionButton(
                        onClick = {
                            viewModel.setDialogState(
                                DialogState.AddTask(addToDaily = uiState.currentView == ViewType.DAILY)
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Aufgabe hinzufügen")
                    }
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
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = uiState.currentView == ViewType.DAILY,
                        onClick = { viewModel.setCurrentView(ViewType.DAILY) },
                        text = {
                            Text(
                                "Heute",
                                fontWeight = if (uiState.currentView == ViewType.DAILY) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = uiState.currentView == ViewType.BACKLOG,
                        onClick = { viewModel.setCurrentView(ViewType.BACKLOG) },
                        text = {
                            Text(
                                "Backlog",
                                fontWeight = if (uiState.currentView == ViewType.BACKLOG) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }

                // Selection Header (wenn im Selection Mode)
                AnimatedVisibility(
                    visible = uiState.isSelectionMode,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    SelectionHeader(
                        selectedCount = uiState.selectedTaskIds.size,
                        totalCount = allCurrentTasks.size,
                        onSelectAll = {
                            allCurrentTasks.forEach { task ->
                                if (task.id !in uiState.selectedTaskIds) {
                                    viewModel.toggleTaskSelection(task.id)
                                }
                            }
                        },
                        onClearSelection = { viewModel.clearSelection() }
                    )
                }

                // Content
                when (uiState.currentView) {
                    ViewType.DAILY -> DailyTasksContent(
                        tasks = currentTasks,
                        totalTasks = todayTasks,
                        maxTasks = settingsManager.maxDailyTasks,
                        uiState = uiState,
                        viewModel = viewModel,
                        updatingTaskIds = updatingTaskIds
                    )
                    ViewType.BACKLOG -> BacklogTasksContent(
                        tasks = currentTasks,
                        uiState = uiState,
                        viewModel = viewModel,
                        updatingTaskIds = updatingTaskIds
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    uiState: TaskUiState,
    todayTasksCount: Int,
    backlogTasksCount: Int,
    maxDailyTasks: Int,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onToggleCompleted: () -> Unit,
    onFillFromBacklog: () -> Unit,
    onInteractionModeChange: (InteractionMode) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

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
                        ViewType.DAILY -> "$todayTasksCount/$maxDailyTasks heute"
                        ViewType.BACKLOG -> "$backlogTasksCount im Backlog"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        actions = {
            // Single Menu Button
            Box {
                IconButton(
                    onClick = { showMenu = true }
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Menü",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.width(250.dp)
                ) {
                    // Fill from Backlog (nur bei Daily View und wenn Backlog nicht leer)
                    if (uiState.currentView == ViewType.DAILY && backlogTasksCount > 0) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PlaylistAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text("Aus Backlog auffüllen")
                                }
                            },
                            onClick = {
                                onFillFromBacklog()
                                showMenu = false
                            }
                        )
                        HorizontalDivider()
                    }

                    // Interaction Mode
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    when (uiState.interactionMode) {
                                        InteractionMode.MINIMAL -> Icons.Default.TouchApp
                                        InteractionMode.CONTEXT_MENU -> Icons.Default.MoreVert
                                        InteractionMode.SELECTION -> Icons.Default.CheckBox
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text("Interaktionsmodus")
                                    Text(
                                        text = when (uiState.interactionMode) {
                                            InteractionMode.MINIMAL -> "Minimal"
                                            InteractionMode.CONTEXT_MENU -> "Kontextmenü"
                                            InteractionMode.SELECTION -> "Auswahl"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        },
                        onClick = {
                            val nextMode = when (uiState.interactionMode) {
                                InteractionMode.MINIMAL -> InteractionMode.CONTEXT_MENU
                                InteractionMode.CONTEXT_MENU -> InteractionMode.SELECTION
                                InteractionMode.SELECTION -> InteractionMode.MINIMAL
                            }
                            onInteractionModeChange(nextMode)
                            showMenu = false
                        }
                    )

                    // Show/Hide Completed
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    if (uiState.showCompletedTasks) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    if (uiState.showCompletedTasks) "Erledigte ausblenden" else "Erledigte anzeigen"
                                )
                            }
                        },
                        onClick = {
                            onToggleCompleted()
                            showMenu = false
                        }
                    )

                    HorizontalDivider()

                    // History
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text("Historie")
                            }
                        },
                        onClick = {
                            onNavigateToHistory()
                            showMenu = false
                        }
                    )

                    // Settings
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text("Einstellungen")
                            }
                        },
                        onClick = {
                            onNavigateToSettings()
                            showMenu = false
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    selectedCount: Int,
    totalCount: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "$selectedCount von $totalCount ausgewählt",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Auswahl aufheben"
                )
            }
        },
        actions = {
            if (selectedCount < totalCount) {
                TextButton(onClick = onSelectAll) {
                    Text("Alle", color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    )
}

@Composable
fun DailyTasksContent(
    tasks: List<TaskEntity>,
    totalTasks: List<TaskEntity>,
    maxTasks: Int,
    uiState: TaskUiState,
    viewModel: TaskViewModel,
    updatingTaskIds: Set<String>
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
                        text = "Fortschritt heute",
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
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )

                if (totalCount > maxTasks && maxTasks > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ ${totalCount - maxTasks} Aufgaben über dem Limit ($maxTasks)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Task List or Empty State
        if (tasks.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskItemByMode(
                        task = task,
                        uiState = uiState,
                        viewModel = viewModel,
                        isUpdating = updatingTaskIds.contains(task.id)
                    )
                }
            }
        } else {
            EmptyStateCard(
                icon = Icons.Default.Task,
                title = "Keine Aufgaben für heute",
                subtitle = "Tippe auf '+' um eine neue Aufgabe hinzuzufügen"
            )
        }
    }
}

@Composable
fun BacklogTasksContent(
    tasks: List<TaskEntity>,
    uiState: TaskUiState,
    viewModel: TaskViewModel,
    updatingTaskIds: Set<String>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (tasks.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskItemByMode(
                        task = task,
                        uiState = uiState,
                        viewModel = viewModel,
                        isUpdating = updatingTaskIds.contains(task.id)
                    )
                }
            }
        } else {
            EmptyStateCard(
                icon = Icons.Default.Inventory,
                title = "Backlog ist leer",
                subtitle = "Alle zusätzlichen Aufgaben werden hier gespeichert"
            )
        }
    }
}

@Composable
fun TaskItemByMode(
    task: TaskEntity,
    uiState: TaskUiState,
    viewModel: TaskViewModel,
    isUpdating: Boolean
) {
    when (uiState.interactionMode) {
        InteractionMode.MINIMAL -> {
            MinimalTaskItem(
                task = task,
                onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                onLongPress = { viewModel.setDialogState(DialogState.TaskDetails(task)) },
                isUpdating = isUpdating
            )
        }
        InteractionMode.CONTEXT_MENU -> {
            ContextMenuTaskItem(
                task = task,
                onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                onEdit = { viewModel.setDialogState(DialogState.EditTask(task)) },
                onDelete = { viewModel.deleteTask(task) },
                onMoveToDaily = { viewModel.moveTaskToDaily(task) },
                onMoveToBacklog = { viewModel.moveTaskToBacklog(task) },
                isUpdating = isUpdating
            )
        }
        InteractionMode.SELECTION -> {
            SelectableTaskItem(
                task = task,
                isSelected = task.id in uiState.selectedTaskIds,
                isSelectionMode = uiState.isSelectionMode,
                onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                onToggleSelection = { viewModel.toggleTaskSelection(task.id) },
                onSingleTap = { viewModel.setDialogState(DialogState.TaskDetails(task)) },
                isUpdating = isUpdating
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}