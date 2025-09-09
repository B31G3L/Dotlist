package de.beigel.list.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.beigel.list.data.*
import de.beigel.list.viewmodel.SmartFocusViewModel
import de.beigel.list.viewmodel.ViewMode
import de.beigel.list.ui.dialogs.SmartAddEditTaskDialog
import de.beigel.list.ui.dialogs.TaskReasoningDialog
import de.beigel.list.ui.dialogs.SmartTaskDetailsDialog
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartFocusScreen(
    viewModel: SmartFocusViewModel = viewModel(),
    onNavigateToAllTasks: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusTasks by viewModel.focusTasks.collectAsState()
    val quickWins by viewModel.quickWins.collectAsState()
    val overdueTasks by viewModel.overdueTasks.collectAsState()
    val todaysDueTasks by viewModel.todaysDueTasks.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()
    val productivityInsights by viewModel.productivityInsights.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val availableTags by viewModel.availableTags.collectAsState()

    // Dialogs
    if (uiState.showReasoningDialog && uiState.currentTaskReasoning != null) {
        TaskReasoningDialog(
            reasoning = uiState.currentTaskReasoning!!,
            onDismiss = viewModel::hideTaskReasoning
        )
    }

    if (uiState.showTaskDetailsDialog && uiState.selectedTask != null) {
        SmartTaskDetailsDialog(
            task = uiState.selectedTask!!,
            onDismiss = viewModel::hideTaskDetails,
            onEdit = { task ->
                viewModel.hideTaskDetails()
                viewModel.showEditTaskDialog(task)
            },
            onDelete = { task ->
                viewModel.hideTaskDetails()
                viewModel.deleteTask(task)
            }
        )
    }

    if (uiState.showAddTaskDialog) {
        SmartAddEditTaskDialog(
            task = uiState.taskToEdit,
            availableTags = availableTags,
            onDismiss = viewModel::hideAddTaskDialog,
            onSave = { title, description, priority, dueDate, estimatedMinutes, energyLevel, context, tags ->
                if (uiState.taskToEdit != null) {
                    val updatedTask = uiState.taskToEdit!!.copy(
                        title = title,
                        description = description,
                        priority = priority,
                        dueDate = dueDate,
                        estimatedMinutes = estimatedMinutes,
                        energyLevel = energyLevel,
                        context = context,
                        tags = tags.joinToString(",")
                    )
                    viewModel.updateTask(updatedTask)
                } else {
                    viewModel.addTask(title, description, priority, dueDate, estimatedMinutes, energyLevel, context, tags)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            SmartFocusTopBar(
                currentViewMode = uiState.viewMode,
                searchQuery = searchQuery,
                onViewModeChanged = viewModel::setViewMode,
                onSearchQueryChanged = viewModel::setSearchQuery,
                onRefresh = viewModel::refreshData,
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToSettings = onNavigateToSettings
            )
        },
        floatingActionButton = {
            SmartFAB(
                onAddTask = viewModel::showAddTaskDialog,
                quickActions = quickWins.take(2)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main Content
            when (uiState.viewMode) {
                ViewMode.FOCUS -> SmartFocusContent(
                    focusTasks = focusTasks,
                    recommendations = recommendations,
                    productivityInsights = productivityInsights,
                    currentContext = uiState.currentContext,
                    onContextChanged = viewModel::setCurrentContext,
                    onTaskClick = viewModel::showTaskDetails,
                    onTaskComplete = viewModel::toggleTaskCompletion,
                    onShowReasoning = viewModel::showTaskReasoning,
                    onNavigateToAllTasks = onNavigateToAllTasks,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                ViewMode.SEARCH -> SearchContent(
                    searchQuery = searchQuery,
                    searchResults = searchResults,
                    availableTags = availableTags,
                    selectedTag = uiState.selectedTag,
                    onTaskClick = viewModel::showTaskDetails,
                    onTaskComplete = viewModel::toggleTaskCompletion,
                    onTagSelected = viewModel::getTasksByTag,
                    onClearTagFilter = viewModel::clearTagFilter,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                ViewMode.OVERDUE -> OverdueContent(
                    overdueTasks = overdueTasks,
                    onTaskClick = viewModel::showTaskDetails,
                    onTaskComplete = viewModel::toggleTaskCompletion,
                    onShowReasoning = viewModel::showTaskReasoning,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                ViewMode.TODAY -> TodayContent(
                    todaysTasks = todaysDueTasks,
                    onTaskClick = viewModel::showTaskDetails,
                    onTaskComplete = viewModel::toggleTaskCompletion,
                    onShowReasoning = viewModel::showTaskReasoning,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                ViewMode.QUICK_WINS -> QuickWinsContent(
                    quickWins = quickWins,
                    onTaskClick = viewModel::showTaskDetails,
                    onTaskComplete = viewModel::toggleTaskCompletion,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                else -> SmartFocusContent(
                    focusTasks = focusTasks,
                    recommendations = recommendations,
                    productivityInsights = productivityInsights,
                    currentContext = uiState.currentContext,
                    onContextChanged = viewModel::setCurrentContext,
                    onTaskClick = viewModel::showTaskDetails,
                    onTaskComplete = viewModel::toggleTaskCompletion,
                    onShowReasoning = viewModel::showTaskReasoning,
                    onNavigateToAllTasks = onNavigateToAllTasks,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Loading Overlay
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            // Success/Error Messages
            uiState.successMessage?.let { message ->
                SuccessMessage(
                    message = message,
                    onDismiss = viewModel::clearMessages,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            uiState.error?.let { error ->
                ErrorMessage(
                    message = error,
                    onDismiss = viewModel::clearMessages,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartFocusTopBar(
    currentViewMode: ViewMode,
    searchQuery: String,
    onViewModeChanged: (ViewMode) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var showViewModeSelector by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            if (currentViewMode == ViewMode.SEARCH) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("Aufgaben suchen...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Suchen")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Löschen")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showViewModeSelector = true }
                ) {
                    Text(
                        text = currentViewMode.icon + " " + currentViewMode.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = "Ansicht wechseln",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        actions = {
            if (currentViewMode != ViewMode.SEARCH) {
                IconButton(onClick = { onViewModeChanged(ViewMode.SEARCH) }) {
                    Icon(Icons.Default.Search, contentDescription = "Suchen")
                }
            }

            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
            }

            IconButton(onClick = onNavigateToHistory) {
                Icon(Icons.Default.History, contentDescription = "Historie")
            }

            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )

    // View Mode Selector Dropdown
    if (showViewModeSelector) {
        ViewModeSelectorDialog(
            currentMode = currentViewMode,
            onModeSelected = { mode ->
                onViewModeChanged(mode)
                showViewModeSelector = false
            },
            onDismiss = { showViewModeSelector = false }
        )
    }
}

@Composable
fun SmartFocusContent(
    focusTasks: List<TaskEntity>,
    recommendations: de.beigel.list.repository.ContextualRecommendations?,
    productivityInsights: de.beigel.list.repository.ProductivityInsights?,
    currentContext: TaskContext?,
    onContextChanged: (TaskContext?) -> Unit,
    onTaskClick: (TaskEntity) -> Unit,
    onTaskComplete: (TaskEntity) -> Unit,
    onShowReasoning: (TaskEntity) -> Unit,
    onNavigateToAllTasks: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Greeting Header
        item {
            GreetingHeader()
        }

        // Quick Stats
        item {
            productivityInsights?.let { insights ->
                QuickStatsCard(insights = insights)
            }
        }

        // Recommendations
        item {
            recommendations?.let { rec ->
                RecommendationsCard(recommendations = rec)
            }
        }

        // Context Filter
        item {
            ContextFilterSection(
                currentContext = currentContext,
                onContextChanged = onContextChanged
            )
        }

        // Main Focus Tasks
        item {
            Text(
                text = "🎯 Deine Top ${focusTasks.size} für jetzt",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (focusTasks.isEmpty()) {
            item {
                EmptyFocusState(onNavigateToAllTasks = onNavigateToAllTasks)
            }
        } else {
            itemsIndexed(focusTasks) { index, task ->
                SmartFocusTaskCard(
                    task = task,
                    rank = index + 1,
                    onTaskClick = { onTaskClick(task) },
                    onCompleteTask = { onTaskComplete(task) },
                    onShowReasoning = { onShowReasoning(task) }
                )
            }

            item {
                ShowMoreTasksCard(onNavigateToAllTasks = onNavigateToAllTasks)
            }
        }
    }
}

@Composable
fun GreetingHeader() {
    val currentTime = LocalTime.now()
    val greeting = when (currentTime.hour) {
        in 5..11 -> "Guten Morgen! 🌅"
        in 12..17 -> "Guten Tag! ☀️"
        in 18..22 -> "Guten Abend! 🌆"
        else -> "Gute Nacht! 🌙"
    }

    Column {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Was möchtest du heute erreichen?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Text(
            text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy")),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun QuickStatsCard(insights: de.beigel.list.repository.ProductivityInsights) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📊 Deine Bilanz",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickStat(
                    icon = "🔥",
                    value = "${insights.currentStreak}",
                    label = "Tage Streak",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                QuickStat(
                    icon = "✅",
                    value = "${insights.totalCompletedTasks}",
                    label = "Diese Woche",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                QuickStat(
                    icon = "⚡",
                    value = "${insights.averageDailyCompletion.toInt()}",
                    label = "Ø pro Tag",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = insights.getMotivationalMessage(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun QuickStat(
    icon: String,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun RecommendationsCard(recommendations: de.beigel.list.repository.ContextualRecommendations) {
    val action = recommendations.recommendedAction

    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (action) {
                de.beigel.list.repository.RecommendedAction.FOCUS_ON_URGENT ->
                    MaterialTheme.colorScheme.errorContainer
                de.beigel.list.repository.RecommendedAction.START_WITH_QUICK_WIN ->
                    MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = action.icon,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = action.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun ContextFilterSection(
    currentContext: TaskContext?,
    onContextChanged: (TaskContext?) -> Unit
) {
    Column {
        Text(
            text = "Arbeitsbereich:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = currentContext == null,
                    onClick = { onContextChanged(null) },
                    label = { Text("Alle") }
                )
            }

            items(TaskContext.values()) { context ->
                FilterChip(
                    selected = currentContext == context,
                    onClick = { onContextChanged(context) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(context.icon)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(context.displayName)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SmartFocusTaskCard(
    task: TaskEntity,
    rank: Int,
    onTaskClick: () -> Unit,
    onCompleteTask: () -> Unit,
    onShowReasoning: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTaskClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = when (rank) {
                            1 -> Color(0xFFFFD700) // Gold
                            2 -> Color(0xFFC0C0C0) // Silver
                            3 -> Color(0xFFCD7F32) // Bronze
                            else -> MaterialTheme.colorScheme.primary
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Task Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Task Metadata
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Priority Indicator
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                Color(task.priority.color),
                                CircleShape
                            )
                    )

                    // Estimated Time
                    task.estimatedMinutes?.let { minutes ->
                        Text(
                            text = "${minutes}min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    // Due Date
                    task.dueDate?.let { dueDate ->
                        val days = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(dueDate))
                        val (text, color) = when {
                            days < 0 -> "Überfällig!" to MaterialTheme.colorScheme.error
                            days == 0L -> "Heute!" to Color(0xFFFF9800)
                            days == 1L -> "Morgen" to Color(0xFFFF9800)
                            else -> "in ${days}d" to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        }

                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall,
                            color = color,
                            fontWeight = if (days <= 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    // Context
                    task.context?.let { context ->
                        Text(
                            text = context.icon,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Energy Level
                    Text(
                        text = task.energyLevel.icon,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onShowReasoning,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Warum wichtig?",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onCompleteTask,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Erledigt",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyFocusState(onNavigateToAllTasks: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎉",
                style = MaterialTheme.typography.displayMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Alle wichtigen Aufgaben erledigt!",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Du bist heute richtig produktiv gewesen. Zeit für eine Pause oder neue Aufgaben?",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNavigateToAllTasks,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Alle Aufgaben anzeigen")
            }
        }
    }
}

@Composable
fun ShowMoreTasksCard(onNavigateToAllTasks: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToAllTasks() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = "Mehr anzeigen",
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Alle Aufgaben anzeigen",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SmartFAB(
    onAddTask: () -> Unit,
    quickActions: List<TaskEntity>
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Quick Action Mini FABs
        AnimatedVisibility(
            visible = expanded,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickActions.forEach { task ->
                    SmallFloatingActionButton(
                        onClick = { /* Complete quick task */ },
                        containerColor = MaterialTheme.colorScheme.secondary
                    ) {
                        Text(
                            text = task.context?.icon ?: "⚡",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                SmallFloatingActionButton(
                    onClick = onAddTask,
                    containerColor = MaterialTheme.colorScheme.tertiary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Neue Aufgabe")
                }
            }
        }

        // Main FAB
        FloatingActionButton(
            onClick = {
                if (expanded) {
                    onAddTask()
                } else {
                    expanded = true
                }
            },
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            AnimatedContent(
                targetState = expanded,
                transitionSpec = { fadeIn() with fadeOut() }
            ) { isExpanded ->
                if (isExpanded) {
                    Icon(Icons.Default.Close, contentDescription = "Schließen")
                } else {
                    Icon(Icons.Default.Add, contentDescription = "Hinzufügen")
                }
            }
        }
    }

    // Auto-collapse after delay
    LaunchedEffect(expanded) {
        if (expanded) {
            kotlinx.coroutines.delay(5000)
            expanded = false
        }
    }
}

@Composable
fun SuccessMessage(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(16.dp)
            .clickable { onDismiss() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun ErrorMessage(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(16.dp)
            .clickable { onDismiss() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}