package de.beigel.list.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.beigel.list.R
import de.beigel.list.data.*
import de.beigel.list.repository.TaskProgress
import de.beigel.list.repository.ContextualRecommendations
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun TaskCard(
    task: TaskEntity,
    modifier: Modifier = Modifier,
    isOverdue: Boolean = false,
    showReasoning: Boolean = false,
    showQuickWinBadge: Boolean = false,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onShowReasoning: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                task.isCompleted -> MaterialTheme.colorScheme.surfaceVariant
                isOverdue -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Checkbox
            TaskCheckbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                priority = task.priority
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Task Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Title Row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Priority Indicator
                    PriorityIndicator(priority = task.priority)
                }

                // Description
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Metadata Row
                TaskMetadataRow(
                    task = task,
                    isOverdue = isOverdue,
                    showQuickWinBadge = showQuickWinBadge
                )

                // Tags
                if (task.tags.isNotEmpty()) {
                    TaskTagsRow(
                        tags = task.tags,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Actions
            Column {
                if (showReasoning && onShowReasoning != null) {
                    IconButton(
                        onClick = onShowReasoning,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Help,
                            contentDescription = stringResource(R.string.accessibility_show_reasoning),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (task.estimatedMinutes != null) {
                    Text(
                        text = "${task.estimatedMinutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TaskCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    priority: TaskPriority,
    modifier: Modifier = Modifier
) {
    val checkboxColor = when (priority) {
        TaskPriority.HIGH -> MaterialTheme.colorScheme.error
        TaskPriority.MEDIUM -> MaterialTheme.colorScheme.primary
        TaskPriority.LOW -> MaterialTheme.colorScheme.outline
    }

    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = CheckboxDefaults.colors(
            checkedColor = checkboxColor,
            checkmarkColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
fun PriorityIndicator(
    priority: TaskPriority,
    modifier: Modifier = Modifier
) {
    val color = when (priority) {
        TaskPriority.HIGH -> MaterialTheme.colorScheme.error
        TaskPriority.MEDIUM -> MaterialTheme.colorScheme.primary
        TaskPriority.LOW -> MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier
            .size(12.dp)
            .background(color, CircleShape)
    )
}

@Composable
fun TaskMetadataRow(
    task: TaskEntity,
    isOverdue: Boolean = false,
    showQuickWinBadge: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Due Date
        task.dueDate?.let { dueDate ->
            DueDateChip(
                dueDate = dueDate,
                isOverdue = isOverdue
            )
        }

        // Context
        if (task.context != TaskContext.NONE) {
            ContextChip(context = task.context)
        }

        // Energy Level
        EnergyLevelChip(energyLevel = task.energyLevel)

        // Quick Win Badge
        if (showQuickWinBadge) {
            QuickWinBadge()
        }

        // Recurring Indicator
        if (task.recurrencePattern != null) {
            Icon(
                Icons.Default.Repeat,
                contentDescription = "Wiederkehrend",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun DueDateChip(
    dueDate: LocalDate,
    isOverdue: Boolean = false,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val daysUntil = ChronoUnit.DAYS.between(today, dueDate)

    val (text, color) = when {
        daysUntil < 0 -> "Überfällig" to MaterialTheme.colorScheme.error
        daysUntil == 0L -> "Heute" to MaterialTheme.colorScheme.primary
        daysUntil == 1L -> "Morgen" to MaterialTheme.colorScheme.primary
        daysUntil <= 7 -> "${daysUntil}d" to MaterialTheme.colorScheme.onSurfaceVariant
        else -> dueDate.format(DateTimeFormatter.ofPattern("dd.MM")) to MaterialTheme.colorScheme.onSurfaceVariant
    }

    AssistChip(
        onClick = { },
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            labelColor = color,
            leadingIconContentColor = color
        ),
        modifier = modifier
    )
}

@Composable
fun ContextChip(
    context: TaskContext,
    modifier: Modifier = Modifier
) {
    val (text, icon) = when (context) {
        TaskContext.WORK -> "Arbeit" to Icons.Default.Work
        TaskContext.HOME -> "Zuhause" to Icons.Default.Home
        TaskContext.ERRANDS -> "Besorgungen" to Icons.Default.ShoppingCart
        TaskContext.CALLS -> "Anrufe" to Icons.Default.Phone
        TaskContext.COMPUTER -> "Computer" to Icons.Default.Computer
        TaskContext.EXERCISE -> "Sport" to Icons.Default.FitnessCenter
        TaskContext.LEARNING -> "Lernen" to Icons.Default.School
        TaskContext.CREATIVE -> "Kreativ" to Icons.Default.Palette
        TaskContext.NONE -> return
    }

    AssistChip(
        onClick = { },
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall
            )
        },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
        },
        modifier = modifier
    )
}

@Composable
fun EnergyLevelChip(
    energyLevel: EnergyLevel,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (energyLevel) {
        EnergyLevel.HIGH -> "⚡" to MaterialTheme.colorScheme.primary
        EnergyLevel.MEDIUM -> "🔋" to MaterialTheme.colorScheme.onSurfaceVariant
        EnergyLevel.LOW -> "💤" to MaterialTheme.colorScheme.outline
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
    )
}

@Composable
fun QuickWinBadge(
    modifier: Modifier = Modifier
) {
    Badge(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.tertiary
    ) {
        Text(
            text = "⚡",
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun TaskTagsRow(
    tags: List<String>,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(tags) { tag ->
            SuggestionChip(
                onClick = { },
                label = {
                    Text(
                        text = "#$tag",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
        }
    }
}

@Composable
fun GreetingHeader(
    progress: TaskProgress,
    hasOverdue: Boolean,
    modifier: Modifier = Modifier
) {
    val currentHour = LocalDateTime.now().hour
    val greeting = when (currentHour) {
        in 5..11 -> stringResource(R.string.greeting_morning)
        in 12..17 -> stringResource(R.string.greeting_day)
        in 18..21 -> stringResource(R.string.greeting_evening)
        else -> stringResource(R.string.greeting_night)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = stringResource(R.string.greeting_question),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (hasOverdue) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Du hast überfällige Aufgaben",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressCard(
    progress: TaskProgress,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.progress_today),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = stringResource(R.string.progress_completed_count, progress.completed, progress.total),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = if (progress.total > 0) progress.completed.toFloat() / progress.total else 0f,
                modifier = Modifier.fillMaxWidth()
            )

            if (progress.total > 0) {
                Text(
                    text = stringResource(R.string.progress_percentage, progress.percentage),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyFocusCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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

            Text(
                text = stringResource(R.string.smart_focus_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = stringResource(R.string.smart_focus_empty_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    message: String,
    icon: ImageVector = Icons.Default.CheckCircle,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun ContextualRecommendationsCard(
    recommendations: ContextualRecommendations,
    modifier: Modifier = Modifier
) {
    if (recommendations.recommendedTasks.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Text(
                    text = "Empfehlung",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Text(
                text = recommendations.context,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(top = 8.dp)
            )

            recommendations.recommendedTasks.take(3).forEach { task ->
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )

                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(stringResource(R.string.search_tasks))
        },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Löschen")
                }
            }
        },
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}