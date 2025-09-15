package de.beigel.list.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import de.beigel.list.data.TaskEntity

@Composable
fun ContextMenuTaskItem(
    task: TaskEntity,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveToDaily: () -> Unit = {},
    onMoveToBacklog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleComplete() },
                    onLongPress = { showContextMenu = true },
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                task.isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority Indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .background(
                        Color(task.priority.color),
                        RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Completion Checkbox
            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (task.isCompleted) "Erledigt" else "Noch offen",
                    tint = if (task.isCompleted) Color(0xFF009966) else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Task Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (task.isCompleted)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Metadata Row
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Priority Badge
                    Surface(
                        color = Color(task.priority.color).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = task.priority.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(task.priority.color),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    // Location Indicator
                    if (!task.isInDailyList) {
                        Surface(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
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
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Backlog",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }

            // Context Menu Indicator
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Kontextmenü",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }

        // Context Menu
        if (showContextMenu) {
            ContextMenu(
                task = task,
                onDismiss = { showContextMenu = false },
                onEdit = {
                    onEdit()
                    showContextMenu = false
                },
                onDelete = {
                    onDelete()
                    showContextMenu = false
                },
                onMoveToDaily = {
                    onMoveToDaily()
                    showContextMenu = false
                },
                onMoveToBacklog = {
                    onMoveToBacklog()
                    showContextMenu = false
                }
            )
        }
    }
}

@Composable
private fun ContextMenu(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveToDaily: () -> Unit,
    onMoveToBacklog: () -> Unit
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        offset = DpOffset((-16).dp, 0.dp)
    ) {
        // Edit
        DropdownMenuItem(
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Bearbeiten")
                }
            },
            onClick = onEdit
        )

        // Move actions
        if (!task.isInDailyList) {
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Today,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF009966)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Zu Heute")
                    }
                },
                onClick = onMoveToDaily
            )
        } else {
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Zu Backlog")
                    }
                },
                onClick = onMoveToBacklog
            )
        }

        Divider()

        // Delete
        DropdownMenuItem(
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Löschen",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            onClick = onDelete
        )
    }
}