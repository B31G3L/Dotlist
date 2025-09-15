package de.beigel.list.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.beigel.list.data.TaskEntity

@Composable
fun SelectableTaskItem(
    task: TaskEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleComplete: () -> Unit,
    onToggleSelection: () -> Unit,
    onSingleTap: () -> Unit,
    isUpdating: Boolean = false,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val animatedElevation by animateDpAsState(
        targetValue = if (isSelected && !isUpdating) 8.dp else 2.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "elevation"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected && !isUpdating) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isUpdating) 0.6f else 1f,
        animationSpec = tween(150),
        label = "alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                this.alpha = alpha
            }
            .pointerInput(isUpdating, isSelectionMode) {
                if (!isUpdating) {
                    detectTapGestures(
                        onTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (isSelectionMode) {
                                onToggleSelection()
                            } else {
                                onSingleTap()
                            }
                        },
                        onLongPress = {
                            if (!isSelectionMode) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleSelection()
                            }
                        }
                    )
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isUpdating -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                task.isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected && !isUpdating) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection Checkbox (nur im Selection Mode)
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = slideInHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(),
                exit = slideOutHorizontally() + fadeOut()
            ) {
                Row {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = {
                            if (!isUpdating) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleSelection()
                            }
                        },
                        enabled = !isUpdating,
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            // Priority Indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (isSelectionMode) 40.dp else 48.dp)
                    .background(
                        Color(task.priority.color).copy(alpha = if (isUpdating) 0.5f else 1f),
                        RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Complete/Incomplete Indicator
            AnimatedVisibility(
                visible = !isSelectionMode,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Row {
                    IconButton(
                        onClick = {
                            if (!isUpdating) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleComplete()
                            }
                        },
                        enabled = !isUpdating,
                        modifier = Modifier.size(32.dp)
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (task.isCompleted) Color(0xFF009966) else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            // Task Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isSelected && !isUpdating) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = if (isSelectionMode) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    color = when {
                        isUpdating -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        isSelected -> MaterialTheme.colorScheme.primary
                        task.isCompleted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                // Description (nur wenn nicht im Selection Mode und vorhanden)
                AnimatedVisibility(
                    visible = task.description.isNotBlank() && !isSelectionMode,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (isUpdating) 0.3f else 0.7f
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Metadata row
                AnimatedVisibility(
                    visible = !isSelectionMode,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Priority badge
                            Surface(
                                color = Color(task.priority.color).copy(
                                    alpha = if (isUpdating) 0.05f else 0.1f
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = task.priority.displayName.first().toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(task.priority.color).copy(
                                        alpha = if (isUpdating) 0.5f else 1f
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }

                            // Location indicator
                            if (!task.isInDailyList) {
                                Surface(
                                    color = MaterialTheme.colorScheme.outline.copy(
                                        alpha = if (isUpdating) 0.05f else 0.1f
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Inventory,
                                            contentDescription = null,
                                            modifier = Modifier.size(10.dp),
                                            tint = MaterialTheme.colorScheme.outline.copy(
                                                alpha = if (isUpdating) 0.3f else 1f
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "B",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline.copy(
                                                alpha = if (isUpdating) 0.3f else 1f
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Selection indicator oder More-Icon
            AnimatedVisibility(
                visible = !isSelectionMode,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                } else {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Mehr Optionen",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Selected indicator im Selection Mode
            AnimatedVisibility(
                visible = isSelectionMode && isSelected && !isUpdating,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Ausgewählt",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Loading indicator im Selection Mode
            AnimatedVisibility(
                visible = isSelectionMode && isUpdating,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// Floating Action Menu für selected Tasks
@Composable
fun SelectionFloatingActionMenu(
    selectedTasks: List<TaskEntity>,
    onEditSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onMoveToDaily: () -> Unit,
    onMoveToBacklog: () -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canMoveToDaily = selectedTasks.any { !it.isInDailyList }
    val canMoveToBacklog = selectedTasks.any { it.isInDailyList }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Individual action buttons mit Animationen
        AnimatedVisibility(
            visible = canMoveToDaily,
            enter = slideInVertically() + scaleIn() + fadeIn(),
            exit = slideOutVertically() + scaleOut() + fadeOut()
        ) {
            SmallFloatingActionButton(
                onClick = onMoveToDaily,
                containerColor = Color(0xFF009966),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Today, contentDescription = "Zu Heute")
            }
        }

        AnimatedVisibility(
            visible = canMoveToBacklog,
            enter = slideInVertically() + scaleIn() + fadeIn(),
            exit = slideOutVertically() + scaleOut() + fadeOut()
        ) {
            SmallFloatingActionButton(
                onClick = onMoveToBacklog,
                containerColor = Color(0xFF666666),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Inventory, contentDescription = "Zu Backlog")
            }
        }

        // Edit nur bei einem ausgewählten Task
        AnimatedVisibility(
            visible = selectedTasks.size == 1,
            enter = slideInVertically() + scaleIn() + fadeIn(),
            exit = slideOutVertically() + scaleOut() + fadeOut()
        ) {
            SmallFloatingActionButton(
                onClick = onEditSelected,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Bearbeiten")
            }
        }

        // Delete button
        SmallFloatingActionButton(
            onClick = onDeleteSelected,
            containerColor = Color(0xFFE53E3E),
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Löschen")
        }

        // Close selection mode
        FloatingActionButton(
            onClick = onClearSelection,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Icon(Icons.Default.Close, contentDescription = "Auswahl aufheben")
        }
    }
}

// Selection Header für Bulk-Aktionen
@Composable
fun SelectionHeader(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$selectedCount von $totalCount ausgewählt",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            Row {
                if (selectedCount < totalCount) {
                    TextButton(onClick = onSelectAll) {
                        Text("Alle auswählen", color = MaterialTheme.colorScheme.primary)
                    }
                }

                TextButton(onClick = onClearSelection) {
                    Text("Abbrechen", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}