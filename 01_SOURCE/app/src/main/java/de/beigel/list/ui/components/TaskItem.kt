package de.beigel.list.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.beigel.list.data.TaskEntity

/**
 * Moderne TaskItem-Komponente basierend auf Material 3 Design-Prinzipien
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTaskItem(
    task: TaskEntity,
    onToggleComplete: () -> Unit,
    onLongPress: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    isUpdating: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    // Material 3 Animationen
    val containerColor by animateColorAsState(
        targetValue = when {
            isUpdating -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
            task.isCompleted -> MaterialTheme.colorScheme.surfaceContainer
            isPressed -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(200, easing = EaseInOut),
        label = "container_color"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed && !isUpdating) 8.dp else 1.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "elevation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isUpdating) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(isUpdating) {
                if (!isUpdating) {
                    detectTapGestures(
                        onTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleComplete()
                        },
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPress()
                        },
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        }
                    )
                }
            }
            .semantics {
                role = Role.Button
                contentDescription = if (task.isCompleted) {
                    "Aufgabe ${task.title} ist erledigt"
                } else {
                    "Aufgabe ${task.title} ist noch offen"
                }
                onClick {
                    onToggleComplete()
                    true
                }
            },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Modern Priority Indicator
            PriorityIndicator(
                priority = task.priority,
                isCompleted = task.isCompleted,
                isUpdating = isUpdating
            )

            // Modern Checkbox
            ModernTaskCheckbox(
                checked = task.isCompleted,
                onCheckedChange = { if (!isUpdating) onToggleComplete() },
                isUpdating = isUpdating
            )

            // Task Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Task Title
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = when {
                        isUpdating -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        task.isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Task Description
                AnimatedVisibility(
                    visible = task.description.isNotBlank(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (isUpdating) 0.5f else 1f
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Metadata Row
                TaskMetadata(
                    task = task,
                    isUpdating = isUpdating
                )
            }

            // More Button
            ModernMoreButton(
                onClick = onMoreClick,
                enabled = !isUpdating,
                isUpdating = isUpdating
            )
        }
    }
}

@Composable
private fun PriorityIndicator(
    priority: de.beigel.list.data.TaskPriority,
    isCompleted: Boolean,
    isUpdating: Boolean
) {
    val color by animateColorAsState(
        targetValue = Color(priority.color).copy(
            alpha = when {
                isUpdating -> 0.4f
                isCompleted -> 0.6f
                else -> 1f
            }
        ),
        animationSpec = tween(200),
        label = "priority_color"
    )

    Surface(
        modifier = Modifier.size(12.dp),
        shape = CircleShape,
        color = color
    ) {}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernTaskCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isUpdating: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "checkbox_progress"
    )

    val scale by animateFloatAsState(
        targetValue = when {
            isUpdating -> 0.9f
            checked -> 1.1f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "checkbox_scale"
    )

    Box(
        modifier = modifier
            .size(32.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        if (isUpdating) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            FilledIconButton(
                onClick = { onCheckedChange(!checked) },
                modifier = Modifier.size(28.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (checked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = if (checked) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            ) {
                AnimatedVisibility(
                    visible = checked,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskMetadata(
    task: TaskEntity,
    isUpdating: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Priority Chip
        AssistChip(
            onClick = { },
            label = {
                Text(
                    text = task.priority.displayName,
                    style = MaterialTheme.typography.labelSmall
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = Color(task.priority.color).copy(alpha = 0.12f),
                labelColor = Color(task.priority.color).copy(
                    alpha = if (isUpdating) 0.6f else 1f
                )
            ),
            modifier = Modifier.height(24.dp)
        )

        // Location Chip
        if (!task.isInDailyList) {
            AssistChip(
                onClick = { },
                label = {
                    Text(
                        text = "Backlog",
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (isUpdating) 0.6f else 1f
                    )
                ),
                modifier = Modifier.height(24.dp)
            )
        }
    }
}

@Composable
private fun ModernMoreButton(
    onClick: () -> Unit,
    enabled: Boolean,
    isUpdating: Boolean
) {
    if (isUpdating) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
    } else {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Mehr Optionen",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Kompakte TaskItem-Variante für Listen mit vielen Elementen
 */
@Composable
fun CompactTaskItem(
    task: TaskEntity,
    onToggleComplete: () -> Unit,
    onLongPress: () -> Unit = {},
    isUpdating: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val containerColor by animateColorAsState(
        targetValue = when {
            isUpdating -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            task.isCompleted -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f)
            isPressed -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "compact_container_color"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .pointerInput(isUpdating) {
                if (!isUpdating) {
                    detectTapGestures(
                        onTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleComplete()
                        },
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPress()
                        },
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        }
                    )
                }
            },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Compact Priority Indicator
            Surface(
                modifier = Modifier.size(8.dp),
                shape = CircleShape,
                color = Color(task.priority.color).copy(
                    alpha = if (isUpdating) 0.5f else 1f
                )
            ) {}

            // Compact Checkbox
            if (isUpdating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                IconButton(
                    onClick = onToggleComplete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (task.isCompleted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Task Title
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                color = when {
                    isUpdating -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    task.isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Compact metadata
            if (!task.isInDailyList) {
                Icon(
                    Icons.Default.Inventory,
                    contentDescription = "Im Backlog",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * TaskItem mit erweiterten Interaktionsmöglichkeiten
 */
@Composable
fun InteractiveTaskItem(
    task: TaskEntity,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveToDaily: () -> Unit = {},
    onMoveToBacklog: () -> Unit = {},
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onToggleSelection: () -> Unit = {},
    isUpdating: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Box {
        ModernTaskItem(
            task = task,
            onToggleComplete = onToggleComplete,
            onLongPress = {
                if (!isSelectionMode) {
                    showMenu = true
                } else {
                    onToggleSelection()
                }
            },
            onMoreClick = {
                if (isSelectionMode) {
                    onToggleSelection()
                } else {
                    showMenu = true
                }
            },
            isUpdating = isUpdating,
            modifier = modifier
        )

        // Selection Overlay
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                        } else {
                            Color.Transparent
                        }
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onToggleSelection() }
                        )
                    }
            )

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Ausgewählt",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(24.dp)
                )
            }
        }

        // Context Menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Bearbeiten") },
                onClick = {
                    onEdit()
                    showMenu = false
                },
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = null)
                }
            )

            if (!task.isInDailyList) {
                DropdownMenuItem(
                    text = { Text("Zu Heute") },
                    onClick = {
                        onMoveToDaily()
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Today, contentDescription = null)
                    }
                )
            } else {
                DropdownMenuItem(
                    text = { Text("Zu Backlog") },
                    onClick = {
                        onMoveToBacklog()
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Inventory, contentDescription = null)
                    }
                )
            }

            HorizontalDivider()

            DropdownMenuItem(
                text = {
                    Text(
                        "Löschen",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    onDelete()
                    showMenu = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
        }
    }
}