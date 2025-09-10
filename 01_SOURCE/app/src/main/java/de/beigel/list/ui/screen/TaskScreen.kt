package de.beigel.list.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.beigel.list.R
import de.beigel.list.data.*
import de.beigel.list.viewmodel.*
import de.beigel.list.ui.components.*
import de.beigel.list.ui.dialogs.*
import de.beigel.list.utils.RecommendedAction
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    viewModel: TaskViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val smartFocusState by viewModel.smartFocusState.collectAsState()
    val todayTasks by viewModel.todayTasks.collectAsState()
    val backlogTasks by viewModel.backlogTasks.collectAsState()
    val overdueTasks by viewModel.overdueTasks.collectAsState()
    val quickWinTasks by viewModel.quickWinTasks.collectAsState()
    val dialogState by viewModel.dialogState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TaskTopAppBar(
            uiState = uiState,
            onViewModeChange = viewModel::setViewMode,
            onSearchQueryChange = viewModel::setSearchQuery,
            onRefresh = viewModel::refreshSmartFocus
        )

        // Main Content
        Box(modifier = Modifier.weight(1f)) {
            when (uiState.currentViewMode) {
                ViewMode.SMART_FOCUS -> SmartFocusContent(
                    smartFocusState = smartFocusState,
                    uiState = uiState,
                    todayTasks = todayTasks,
                    onTaskToggle = viewModel::toggleTaskCompleted,
                    onTaskClick = viewModel::showEditTaskDialog,
                    onShowReasoning = viewModel::showTaskReasoningDialog
                )

                ViewMode.ALL_TASKS -> AllTasksContent(
                    backlogTasks = backlogTasks,
                    uiState = uiState,
                    onTaskToggle = viewModel::toggleTaskCompleted,
                    onTaskClick = viewModel::showEditTaskDialog
                )

                ViewMode.OVERDUE -> OverdueTasksContent(
                    overdueTasks = overdueTasks,
                    onTaskToggle = viewModel::toggleTaskCompleted,
                    onTaskClick = viewModel::showEditTaskDialog
                )

                ViewMode.QUICK_WINS -> QuickWinsContent(
                    quickWinTasks = quickWinTasks,
                    onTaskToggle = viewModel::toggleTaskCompleted,
                    onTaskClick = viewModel::showEditTaskDialog
                )

                ViewMode.SEARCH -> SearchResultsContent(
                    searchResults = uiState.searchResults,
                    searchQuery = uiState.searchQuery,
                    onTaskToggle = viewModel::toggleTaskCompleted,
                    onTaskClick = viewModel::showEditTaskDialog
                )

                else -> SmartFocusContent(
                    smartFocusState = smartFocusState,
                    uiState = uiState,
                    todayTasks = todayTasks,
                    onTaskToggle = viewModel::toggleTaskCompleted,
                    onTaskClick = viewModel::showEditTaskDialog,
                    onShowReasoning = viewModel::showTaskReasoningDialog
                )
            }

            // Loading Overlay
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { viewModel.showCreateTaskDialog() },
            modifier = Modifier
                .align(Alignment.End)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_task))
        }
    }

    // Dialogs
    dialogState?.let { state ->
        when (state) {
            is DialogState.CreateTask -> CreateTaskDialog(
                onDismiss = viewModel::dismissDialog,
                onCreateTask = { title, description, priority, dueDate, estimatedMinutes, energyLevel, context, tags ->
                    viewModel.createTask(
                        title = title,
                        description = description,
                        priority = priority,
                        dueDate = dueDate,
                        estimatedMinutes = estimatedMinutes,
                        energyLevel = energyLevel,
                        context = context,
                        tags = tags
                    )
                    viewModel.dismissDialog()
                }
            )

            is DialogState.EditTask -> EditTaskDialog(
                task = state.task,
                onDismiss = viewModel::dismissDialog,
                onUpdateTask = { task ->
                    viewModel.updateTask(task)
                    viewModel.dismissDialog()
                },
                onDeleteTask = { task ->
                    viewModel.showDeleteConfirmDialog(task)
                }
            )

            is DialogState.DeleteConfirm -> DeleteConfirmDialog(
                task = state.task,
                onDismiss = viewModel::dismissDialog,
                onConfirmDelete = { taskId ->
                    viewModel.deleteTask(taskId)
                    viewModel.dismissDialog()
                }
            )

            is DialogState.TaskReasoning -> TaskReasoningDialog(
                reasoning = state.reasoning,
                onDismiss = viewModel::dismissDialog
            )
        }
    }

    // Snackbar Messages
    LaunchedEffect(uiState.error, uiState.successMessage) {
        if (uiState.error != null) {
            // Zeige Error Snackbar
            viewModel.clearError()
        }
        if (uiState.successMessage != null) {
            // Zeige Success Snackbar
            viewModel.clearSuccessMessage()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskTopAppBar(
    uiState: TaskUiState,
    onViewModeChange: (ViewMode) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit
) {
    TopAppBar(
        title = {
            when (uiState.currentViewMode) {
                ViewMode.SMART_FOCUS -> Text(stringResource(R.string.smart_focus))
                ViewMode.ALL_TASKS -> Text(stringResource(R.string.view_all))
                ViewMode.OVERDUE -> Text(stringResource(R.string.view_overdue))
                ViewMode.QUICK_WINS -> Text(stringResource(R.string.quick_wins))
                ViewMode.SEARCH -> SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = onSearchQueryChange
                )
                else -> Text(stringResource(R.string.app_name))
            }
        },
        actions = {
            // View Mode Dropdown
            var showMenu by remember { mutableStateOf(false) }

            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.FilterList, contentDescription = "Ansicht")
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                ViewMode.values().forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(getViewModeText(mode)) },
                        onClick = {
                            onViewModeChange(mode)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(getViewModeIcon(mode), contentDescription = null)
                        }
                    )
                }
            }

            // Refresh Button
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun SmartFocusContent(
    smartFocusState: SmartFocusUiState,
    uiState: TaskUiState,
    todayTasks: List<TaskEntity>,
    onTaskToggle: (Long) -> Unit,
    onTaskClick: (TaskEntity) -> Unit,
    onShowReasoning: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Greeting Header
        item {
            GreetingHeader(
                progress = uiState.todayProgress,
                hasOverdue = uiState.hasOverdueTasks
            )
        }

        // Progress Card
        item {
            ProgressCard(progress = uiState.todayProgress)
        }

        // Focus Tasks
        item {
            Text(
                text = stringResource(R.string.smart_focus_subtitle),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (smartFocusState.focusTasks.isEmpty()) {
            item {
                EmptyFocusCard()
            }
        } else {
            items(smartFocusState.focusTasks) { task ->
                TaskCard(
                    task = task,
                    showReasoning = true,
                    onToggle = { onTaskToggle(task.id) },
                    onClick = { onTaskClick(task) },
                    onShowReasoning = { onShowReasoning(task.id) }
                )
            }
        }

        // Contextual Recommendations
        uiState.contextualRecommendations?.let { recommendations ->
            item {
                ContextualRecommendationsCard(recommendations = recommendations)
            }
        }
    }
}

@Composable
private fun AllTasksContent(
    backlogTasks: List<TaskEntity>,
    uiState: TaskUiState,
    onTaskToggle: (Long) -> Unit,
    onTaskClick: (TaskEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Alle Aufgaben (${backlogTasks.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (backlogTasks.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Keine Aufgaben im Backlog",
                    message = "Alle Aufgaben erledigt oder erstelle neue Aufgaben"
                )
            }
        } else {
            items(backlogTasks) { task ->
                TaskCard(
                    task = task,
                    onToggle = { onTaskToggle(task.id) },
                    onClick = { onTaskClick(task) }
                )
            }
        }
    }
}

@Composable
private fun OverdueTasksContent(
    overdueTasks: List<TaskEntity>,
    onTaskToggle: (Long) -> Unit,
    onTaskClick: (TaskEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Überfällige Aufgaben (${overdueTasks.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (overdueTasks.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Keine überfälligen Aufgaben! 🎉",
                    message = "Du bist up-to-date mit deinen Deadlines"
                )
            }
        } else {
            items(overdueTasks) { task ->
                TaskCard(
                    task = task,
                    isOverdue = true,
                    onToggle = { onTaskToggle(task.id) },
                    onClick = { onTaskClick(task) }
                )
            }
        }
    }
}

@Composable
private fun QuickWinsContent(
    quickWinTasks: List<TaskEntity>,
    onTaskToggle: (Long) -> Unit,
    onTaskClick: (TaskEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "⚡ Quick Wins",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.quick_wins_description),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        if (quickWinTasks.isEmpty()) {
            item {
                EmptyStateCard(
                    title = stringResource(R.string.quick_wins_empty),
                    message = stringResource(R.string.quick_wins_empty_message)
                )
            }
        } else {
            items(quickWinTasks) { task ->
                TaskCard(
                    task = task,
                    showQuickWinBadge = true,
                    onToggle = { onTaskToggle(task.id) },
                    onClick = { onTaskClick(task) }
                )
            }
        }
    }
}

@Composable
private fun SearchResultsContent(
    searchResults: List<TaskEntity>,
    searchQuery: String,
    onTaskToggle: (Long) -> Unit,
    onTaskClick: (TaskEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (searchQuery.isBlank()) {
            item {
                EmptyStateCard(
                    title = stringResource(R.string.search_empty_title),
                    message = stringResource(R.string.search_empty_message)
                )
            }
        } else if (searchResults.isEmpty()) {
            item {
                EmptyStateCard(
                    title = stringResource(R.string.search_no_results),
                    message = stringResource(R.string.search_no_results_message)
                )
            }
        } else {
            item {
                Text(
                    text = "${searchResults.size} Ergebnisse für \"$searchQuery\"",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(searchResults) { task ->
                TaskCard(
                    task = task,
                    onToggle = { onTaskToggle(task.id) },
                    onClick = { onTaskClick(task) }
                )
            }
        }
    }
}

// Helper Functions
@Composable
private fun getViewModeText(mode: ViewMode): String {
    return when (mode) {
        ViewMode.SMART_FOCUS -> stringResource(R.string.view_focus)
        ViewMode.ALL_TASKS -> stringResource(R.string.view_all)
        ViewMode.OVERDUE -> stringResource(R.string.view_overdue)
        ViewMode.TODAY -> stringResource(R.string.view_today)
        ViewMode.QUICK_WINS -> stringResource(R.string.view_quick_wins)
        ViewMode.BY_CONTEXT -> stringResource(R.string.view_by_context)
        ViewMode.SEARCH -> stringResource(R.string.view_search)
    }
}

private fun getViewModeIcon(mode: ViewMode) = when (mode) {
    ViewMode.SMART_FOCUS -> Icons.Default.Star
    ViewMode.ALL_TASKS -> Icons.Default.List
    ViewMode.OVERDUE -> Icons.Default.Warning
    ViewMode.TODAY -> Icons.Default.Today
    ViewMode.QUICK_WINS -> Icons.Default.FlashOn
    ViewMode.BY_CONTEXT -> Icons.Default.Category
    ViewMode.SEARCH -> Icons.Default.Search
}