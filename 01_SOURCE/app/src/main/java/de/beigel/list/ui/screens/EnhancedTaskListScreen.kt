package de.beigel.list.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import de.beigel.list.ui.dialogs.AddTaskBottomSheet
import de.beigel.list.ui.components.SwipeableTaskItem
import de.beigel.list.ui.components.PullToRefreshLazyColumn
import de.beigel.list.ui.animations.*
import de.beigel.list.ui.utils.rememberHapticFeedback
import de.beigel.list.ui.theme.TaskTypography
import de.beigel.list.settings.SettingsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedTaskListScreen(
    viewModel: TaskViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val todayTasks by viewModel.todayTasks.collectAsState()
    val backlogTasks by viewModel.backlogTasks.collectAsState()
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val hapticFeedback = rememberHapticFeedback()
    val composeHapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Refresh state
    var isRefreshing by remember { mutableStateOf(false) }

    // Celebration state
    var showCelebration by remember { mutableStateOf(false) }

    // Check if all tasks completed for celebration
    LaunchedEffect(todayTasks) {
        val completedCount = todayTasks.count { it.isCompleted }
        val totalCount = todayTasks.size
        if (totalCount > 0 && completedCount == totalCount && completedCount > 2) {
            showCelebration = true
            hapticFeedback.celebration()
        }
    }

    val currentTasks = when (uiState.currentView) {
        ViewType.DAILY -> if (uiState.showCompletedTasks) todayTasks else todayTasks.filter { !it.isCompleted }
        ViewType.BACKLOG -> if (uiState.showCompletedTasks) backlogTasks else backlogTasks.filter { !it.isCompleted }
    }

    // Handle Dialog States
    when (val dialogState = uiState.dialogState) {
        is DialogState.AddTask -> {
            AddTaskBottomSheet(
                isVisible = true,
                onDismiss = {
                    viewModel.setDialogState(DialogState.None)
                    hapticFeedback.buttonPress()
                },
                onSave = { title, description, priority, addToDaily ->
                    viewModel.addTask(title, description, priority, addToDaily)
                    hapticFeedback.taskAdded()
                },
                showDestinationChoice = true,
                initialAddToDaily = dialogState.addToDaily
            )
        }
        is DialogState.EditTask -> {
            AddEditTaskDialog(
                isEditing = true,
                initialTask = dialogState.task,
                onDismiss = {
                    viewModel.setDialogState(DialogState.None)
                    hapticFeedback.buttonPress()
                },
                onSave = { title, description, priority ->
                    viewModel.updateTask(
                        dialogState.task.copy(
                            title = title,
                            description = description,
                            priority = priority
                        )
                    )
                    hapticFeedback.taskAdded()
                }
            )
        }
        is DialogState.TaskDetails -> {
            TaskDetailsDialog(
                task = dialogState.task,
                onDismiss = {
                    viewModel.setDialogState(DialogState.None)
                    hapticFeedback.buttonPress()
                },
                onEdit = {
                    viewModel.setDialogState(DialogState.EditTask(dialogState.task))
                    hapticFeedback.buttonPress()
                },
                onMoveToDaily = {
                    viewModel.moveTaskToDaily(dialogState.task)
                    hapticFeedback.taskAdded()
                },
                onMoveToBacklog = {
                    viewModel.moveTaskToBacklog(dialogState.task)
                    hapticFeedback.buttonPress()
                }
            )
        }
        DialogState.None -> { /* No dialog */ }
    }

    // Celebration overlay
    if (showCelebration) {
        TaskCompletionCelebration(
            isVisible = showCelebration,
            onAnimationEnd = { showCelebration = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Daily List",
                            style = TaskTypography.SectionHeader,
                            fontWeight = FontWeight.Bold
                        )
                        AnimatedContent(
                            targetState = when (uiState.currentView) {
                                ViewType.DAILY -> "${todayTasks.size}/${settingsManager.maxDailyTasks} heute"
                                ViewType.BACKLOG -> "${backlogTasks.size} im Backlog"
                            },
                            transitionSpec = {
                                slideInVertically { it } + fadeIn() togetherWith
                                        slideOutVertically { -it } + fadeOut()
                            }, label = "subtitle"
                        ) { text ->
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                actions = {
                    // Fill from Backlog Button (nur bei Daily View)
                    if (uiState.currentView == ViewType.DAILY && backlogTasks.isNotEmpty()) {
                        PulsingIcon(scale = 1.05f) {
                            IconButton(
                                onClick = {
                                    viewModel.fillFromBacklog(settingsManager.maxDailyTasks)
                                    hapticFeedback.buttonPress()
                                }
                            ) {
                                Icon(
                                    Icons.Default.PlaylistAdd,
                                    contentDescription = "Aus Backlog auffüllen",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    BouncyButton(
                        onClick = {
                            onNavigateToHistory()
                            hapticFeedback.buttonPress()
                        }
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "Historie",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    BouncyButton(
                        onClick = {
                            onNavigateToSettings()
                            hapticFeedback.buttonPress()
                        }
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Einstellungen",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    BouncyButton(
                        onClick = {
                            viewModel.setShowCompleted(!uiState.showCompletedTasks)
                            hapticFeedback.buttonPress()
                        }
                    ) {
                        AnimatedContent(
                            targetState = uiState.showCompletedTasks,
                            transitionSpec = {
                                scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut()
                            }, label = "visibility_icon"
                        ) { showCompleted ->
                            Icon(
                                if (showCompleted) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showCompleted) "Erledigte ausblenden" else "Erledigte anzeigen",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingAnimation {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.setDialogState(
                            DialogState.AddTask(addToDaily = uiState.currentView == ViewType.DAILY)
                        )
                        hapticFeedback.buttonPress()
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    AnimatedContent(
                        targetState = uiState.currentView,
                        transitionSpec = {
                            slideInHorizontally { it } + fadeIn() togetherWith
                                    slideOutHorizontally { -it } + fadeOut()
                        }, label = "fab_content"
                    ) { viewType ->
                        Row {
                            Icon(Icons.Default.Add, contentDescription = "Aufgabe hinzufügen")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                when (viewType) {
                                    ViewType.DAILY -> "Zur Liste"
                                    ViewType.BACKLOG -> "Zum Backlog"
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Enhanced Tab Row
            TabRow(
                selectedTabIndex = uiState.currentView.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.currentView.ordinal]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = uiState.currentView == ViewType.DAILY,
                    onClick = {
                        viewModel.setCurrentView(ViewType.DAILY)
                        hapticFeedback.buttonPress()
                    },
                    text = {
                        Text(
                            "Heute",
                            style = if (uiState.currentView == ViewType.DAILY)
                                TaskTypography.ButtonText
                            else MaterialTheme.typography.bodyMedium
                        )
                    }
                )
                Tab(
                    selected = uiState.currentView == ViewType.BACKLOG,
                    onClick = {
                        viewModel.setCurrentView(ViewType.BACKLOG)
                        hapticFeedback.buttonPress()
                    },
                    text = {
                        Text(
                            "Backlog",
                            style = if (uiState.currentView == ViewType.BACKLOG)
                                TaskTypography.ButtonText
                            else MaterialTheme.typography.bodyMedium
                        )
                    }
                )
            }

            // Enhanced Content with Pull-to-Refresh
            PullToRefreshLazyColumn(
                onRefresh = {
                    isRefreshing = true
                    scope.launch {
                        delay(1000) // Simulate refresh
                        isRefreshing = false
                        hapticFeedback.buttonPress()
                    }
                },
                refreshing = isRefreshing
            ) {
                when (uiState.currentView) {
                    ViewType.DAILY -> EnhancedDailyTasksContent(
                        tasks = currentTasks,
                        totalTasks = todayTasks,
                        maxTasks = settingsManager.maxDailyTasks,
                        viewModel = viewModel,
                        hapticFeedback = hapticFeedback
                    )
                    ViewType.BACKLOG -> EnhancedBacklogTasksContent(
                        tasks = currentTasks,
                        viewModel = viewModel,
                        hapticFeedback = hapticFeedback
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedDailyTasksContent(
    tasks: List<TaskEntity>,
    totalTasks: List<TaskEntity>,
    maxTasks: Int,
    viewModel: TaskViewModel,
    hapticFeedback: de.beigel.list.ui.utils.HapticFeedbackManager
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Enhanced Progress Card
        val completedCount = totalTasks.count { it.isCompleted }
        val totalCount = totalTasks.size
        val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

        AnimatedVisibility(
            visible = totalCount > 0,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Fortschritt",
                                style = TaskTypography.SectionHeader,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Heute",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.Baseline
                        ) {
                            CountingNumber(
                                targetValue = completedCount,
                                textStyle = TaskTypography.StatNumber.copy(
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = " / $totalCount",
                                style = TaskTypography.StatNumber.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    AnimatedProgress(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(progress * 100).toInt()}% erledigt",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "max. $maxTasks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    // Warning if over limit
                    if (totalCount > maxTasks) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${totalCount - maxTasks} Aufgaben über dem Limit",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Enhanced Task List
        if (tasks.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(tasks, key = { _, task -> task.id }) { index, task ->
                    AnimatedListItem(
                        visible = true,
                        delay = index * 50
                    ) {
                        SwipeableTaskItem(
                            task = task,
                            onToggleComplete = {
                                viewModel.toggleTaskCompletion(task)
                                if (!task.isCompleted) {
                                    hapticFeedback.taskCompleted()
                                }
                            },
                            onEdit = {
                                viewModel.setDialogState(DialogState.EditTask(task))
                                hapticFeedback.buttonPress()
                            },
                            onDelete = {
                                viewModel.deleteTask(task)
                                hapticFeedback.taskDeleted()
                            },
                            onShowDetails = {
                                viewModel.setDialogState(DialogState.TaskDetails(task))
                                hapticFeedback.buttonPress()
                            },
                            showMoveToBacklog = true,
                            onMoveToBacklog = {
                                viewModel.moveTaskToBacklog(task)
                                hapticFeedback.buttonPress()
                            }
                        )
                    }
                }
            }
        } else {
            // Enhanced Empty State
            EnhancedEmptyState(
                icon = Icons.Default.Task,
                title = "Keine Aufgaben für heute",
                description = "Tippe auf '+' um eine neue Aufgabe hinzuzufügen",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun EnhancedBacklogTasksContent(
    tasks: List<TaskEntity>,
    viewModel: TaskViewModel,
    hapticFeedback: de.beigel.list.ui.utils.HapticFeedbackManager
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
                itemsIndexed(tasks, key = { _, task -> task.id }) { index, task ->
                    AnimatedListItem(
                        visible = true,
                        delay = index * 50
                    ) {
                        SwipeableTaskItem(
                            task = task,
                            onToggleComplete = {
                                viewModel.toggleTaskCompletion(task)
                                hapticFeedback.taskCompleted()
                            },
                            onEdit = {
                                viewModel.setDialogState(DialogState.EditTask(task))
                                hapticFeedback.buttonPress()
                            },
                            onDelete = {
                                viewModel.deleteTask(task)
                                hapticFeedback.taskDeleted()
                            },
                            onShowDetails = {
                                viewModel.setDialogState(DialogState.TaskDetails(task))
                                hapticFeedback.buttonPress()
                            },
                            showMoveToDaily = true,
                            onMoveToDaily = {
                                viewModel.moveTaskToDaily(task)
                                hapticFeedback.taskAdded()
                            }
                        )
                    }
                }
            }
        } else {
            EnhancedEmptyState(
                icon = Icons.Default.Inventory,
                title = "Backlog ist leer",
                description = "Alle zusätzlichen Aufgaben werden hier gespeichert",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun EnhancedEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PulsingIcon(scale = 1.1f, duration = 2000) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = title,
                style = TaskTypography.CardTitle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}