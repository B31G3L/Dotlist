package de.beigel.list.ui.screens

import de.beigel.list.ui.components.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Navigation Destination Enum
enum class NavigationDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    ABOUT("about", "Über", Icons.Default.Info),
    TODAY("today", "Heute", Icons.Default.Today),
    BACKLOG("backlog", "Backlog", Icons.Default.Inventory),
    SETTINGS("settings", "Einstellungen", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    currentDestination: NavigationDestination,
    onNavigationChange: (NavigationDestination) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val todayTasks by viewModel.todayTasks.collectAsState()
    val backlogTasks by viewModel.backlogTasks.collectAsState()
    val filteredTodayTasks by viewModel.filteredTodayTasks.collectAsState()
    val filteredBacklogTasks by viewModel.filteredBacklogTasks.collectAsState()
    val updatingTaskIds by viewModel.updatingTaskIds.collectAsState()

    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }

    // Pager State für Swipe-Funktionalität zwischen allen Screens
    val pagerState = rememberPagerState(
        initialPage = when (currentDestination) {
            NavigationDestination.ABOUT -> 0
            NavigationDestination.TODAY -> 1
            NavigationDestination.BACKLOG -> 2
            NavigationDestination.SETTINGS -> 3
        },
        pageCount = { 4 }
    )
    val coroutineScope = rememberCoroutineScope()

    // Sync PagerState with Navigation
    LaunchedEffect(pagerState.currentPage) {
        val newDestination = when (pagerState.currentPage) {
            0 -> NavigationDestination.ABOUT
            1 -> NavigationDestination.TODAY
            2 -> NavigationDestination.BACKLOG
            3 -> NavigationDestination.SETTINGS
            else -> NavigationDestination.TODAY
        }
        if (currentDestination != newDestination) {
            onNavigationChange(newDestination)
        }
    }

    // Sync Navigation with PagerState
    LaunchedEffect(currentDestination) {
        val targetPage = when (currentDestination) {
            NavigationDestination.ABOUT -> 0
            NavigationDestination.TODAY -> 1
            NavigationDestination.BACKLOG -> 2
            NavigationDestination.SETTINGS -> 3
        }
        if (pagerState.currentPage != targetPage) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(targetPage)
            }
        }
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

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentDestination = currentDestination,
                onNavigationChange = onNavigationChange,
                todayTasksCount = todayTasks.size,
                backlogTasksCount = backlogTasks.size
            )
        },
        floatingActionButton = {
            if ((currentDestination == NavigationDestination.TODAY || currentDestination == NavigationDestination.BACKLOG) &&
                !uiState.isSelectionMode) {
                FloatingActionButton(
                    onClick = {
                        viewModel.setDialogState(
                            DialogState.AddTask(addToDaily = currentDestination == NavigationDestination.TODAY)
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Aufgabe hinzufügen")
                }
            } else if (uiState.isSelectionMode) {
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
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Selection Header (nur für Today und Backlog im Selection Mode)
            AnimatedVisibility(
                visible = uiState.isSelectionMode &&
                        (currentDestination == NavigationDestination.TODAY || currentDestination == NavigationDestination.BACKLOG),
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                SelectionHeader(
                    selectedCount = uiState.selectedTaskIds.size,
                    totalCount = if (currentDestination == NavigationDestination.TODAY) todayTasks.size else backlogTasks.size,
                    onSelectAll = {
                        val allTasks = if (currentDestination == NavigationDestination.TODAY) todayTasks else backlogTasks
                        allTasks.forEach { task ->
                            if (task.id !in uiState.selectedTaskIds) {
                                viewModel.toggleTaskSelection(task.id)
                            }
                        }
                    },
                    onClearSelection = { viewModel.clearSelection() }
                )
            }

            // Swipeable Content für alle Screens
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = !uiState.isSelectionMode
            ) { page ->
                when (page) {
                    0 -> AboutScreen()
                    1 -> DailyTasksContent(
                        tasks = filteredTodayTasks,
                        totalTasks = todayTasks,
                        maxTasks = settingsManager.maxDailyTasks,
                        uiState = uiState,
                        viewModel = viewModel,
                        updatingTaskIds = updatingTaskIds
                    )
                    2 -> BacklogTasksContent(
                        tasks = filteredBacklogTasks,
                        uiState = uiState,
                        viewModel = viewModel,
                        updatingTaskIds = updatingTaskIds
                    )
                    3 -> SettingsScreen(
                        onNavigateBack = { /* Not needed with swipe */ },
                        onThemeChange = { useSystem, dark, customTheme ->
                            // Theme change wird automatisch von SettingsScreen gehandelt
                        },
                        onShowOnboarding = {
                            // Onboarding show logic if needed
                        }
                    )

                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentDestination: NavigationDestination,
    onNavigationChange: (NavigationDestination) -> Unit,
    todayTasksCount: Int,
    backlogTasksCount: Int
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationDestination.values().forEach { destination ->
            NavigationBarItem(
                icon = {
                    BadgedBox(
                        badge = {
                            if (destination == NavigationDestination.TODAY && todayTasksCount > 0) {
                                Badge {
                                    Text(
                                        todayTasksCount.toString(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            } else if (destination == NavigationDestination.BACKLOG && backlogTasksCount > 0) {
                                Badge {
                                    Text(
                                        backlogTasksCount.toString(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            destination.icon,
                            contentDescription = destination.title
                        )
                    }
                },
                label = {
                    Text(
                        destination.title,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                selected = currentDestination == destination,
                onClick = {
                    if (currentDestination != destination) {
                        onNavigationChange(destination)
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
        }
    }
}

// DailyTasksContent und BacklogTasksContent bleiben unverändert
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
            ModernTaskItem(
                task = task,
                onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                onLongPress = { viewModel.setDialogState(DialogState.TaskDetails(task)) },
                onMoreClick = { viewModel.setDialogState(DialogState.TaskDetails(task)) },
                isUpdating = isUpdating
            )
        }
        InteractionMode.CONTEXT_MENU -> {
            InteractiveTaskItem(
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
            InteractiveTaskItem(
                task = task,
                onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                onEdit = { viewModel.setDialogState(DialogState.EditTask(task)) },
                onDelete = { viewModel.deleteTask(task) },
                onMoveToDaily = { viewModel.moveTaskToDaily(task) },
                onMoveToBacklog = { viewModel.moveTaskToBacklog(task) },
                isSelected = task.id in uiState.selectedTaskIds,
                isSelectionMode = uiState.isSelectionMode,
                onToggleSelection = { viewModel.toggleTaskSelection(task.id) },
                isUpdating = isUpdating
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
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

