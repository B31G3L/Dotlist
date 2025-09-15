package de.beigel.list.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.beigel.list.data.TaskEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTaskItem(
    task: TaskEntity,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShowDetails: () -> Unit,
    modifier: Modifier = Modifier,
    showMoveToDaily: Boolean = false,
    showMoveToBacklog: Boolean = false,
    onMoveToDaily: () -> Unit = {},
    onMoveToBacklog: () -> Unit = {}
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Swipe nach rechts: Aufgabe als erledigt markieren
                    onToggleComplete()
                    false // Nicht dismissen, nur Action ausführen
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // Swipe nach links: Löschen
                    onDelete()
                    true // Dismissen
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            SwipeBackground(
                dismissDirection = dismissState.dismissDirection,
                isCompleted = task.isCompleted
            )
        },
        content = {
            AnimatedTaskContent(
                task = task,
                onEdit = onEdit,
                onShowDetails = onShowDetails,
                showMoveToDaily = showMoveToDaily,
                showMoveToBacklog = showMoveToBacklog,
                onMoveToDaily = onMoveToDaily,
                onMoveToBacklog = onMoveToBacklog
            )
        }
    )
}

@Composable
fun SwipeBackground(
    dismissDirection: SwipeToDismissBoxValue,
    isCompleted: Boolean
) {
    val color = when (dismissDirection) {
        SwipeToDismissBoxValue.StartToEnd -> {
            if (isCompleted) Color(0xFFFF9800) else Color(0xFF4CAF50) // Orange für uncomplete, Grün für complete
        }
        SwipeToDismissBoxValue.EndToStart -> Color(0xFFE53E3E) // Rot für Delete
        SwipeToDismissBoxValue.Settled -> Color.Transparent
    }

    val icon = when (dismissDirection) {
        SwipeToDismissBoxValue.StartToEnd -> {
            if (isCompleted) Icons.Default.Undo else Icons.Default.Check
        }
        SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
        SwipeToDismissBoxValue.Settled -> Icons.Default.Clear
    }

    val alignment = when (dismissDirection) {
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
        SwipeToDismissBoxValue.Settled -> Alignment.Center
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = 20.dp),
        contentAlignment = alignment
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun AnimatedTaskContent(
    task: TaskEntity,
    onEdit: () -> Unit,
    onShowDetails: () -> Unit,
    showMoveToDaily: Boolean,
    showMoveToBacklog: Boolean,
    onMoveToDaily: () -> Unit,
    onMoveToBacklog: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onShowDetails() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority Indicator mit Animation
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(animationSpec = tween(300))
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(task.priority.color))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Animated Checkbox
            AnimatedCheckbox(
                checked = task.isCompleted,
                onCheckedChange = { /* Handled by swipe */ }
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Task Content mit Animation
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedContent(
                        targetState = task.title,
                        transitionSpec = {
                            slideInHorizontally() togetherWith slideOutHorizontally()
                        },
                        label = "title_animation"
                    ) { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF3C3C3C),
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Daily/Backlog Indicator mit Animation
                    if (!task.isInDailyList) {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            Badge(
                                containerColor = Color(0xFF666666).copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = "B",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                if (task.description.isNotBlank()) {
                    AnimatedVisibility(
                        visible = task.description.isNotBlank(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF3C3C3C).copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Priority Badge mit Animation
                AnimatedContent(
                    targetState = task.priority,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "priority_animation"
                ) { priority ->
                    Text(
                        text = priority.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(priority.color)
                    )
                }
            }

            // Action Buttons mit Animationen
            Row {
                // Move Buttons
                AnimatedVisibility(
                    visible = showMoveToDaily,
                    enter = slideInHorizontally() + fadeIn(),
                    exit = slideOutHorizontally() + fadeOut()
                ) {
                    IconButton(onClick = onMoveToDaily) {
                        Icon(
                            Icons.Default.Today,
                            contentDescription = "Zu Heute",
                            tint = Color(0xFF009966)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showMoveToBacklog,
                    enter = slideInHorizontally() + fadeIn(),
                    exit = slideOutHorizontally() + fadeOut()
                ) {
                    IconButton(onClick = onMoveToBacklog) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = "Zu Backlog",
                            tint = Color(0xFF666666)
                        )
                    }
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Bearbeiten",
                        tint = Color(0xFF009966)
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "checkbox_animation"
    )

    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = CheckboxDefaults.colors(
            checkedColor = Color(0xFF009966),
            uncheckedColor = Color(0xFF009966).copy(alpha = animatedProgress)
        ),
        modifier = Modifier.graphicsLayer {
            scaleX = 0.8f + (0.2f * animatedProgress)
            scaleY = 0.8f + (0.2f * animatedProgress)
        }
    )
}