package de.beigel.list.ui.components

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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.beigel.list.data.TaskEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
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
    Card(
        modifier = modifier
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
            // Priority Indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(task.priority.color))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Checkbox
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF009966)
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Task Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF3C3C3C),
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Daily/Backlog Indicator
                    if (!task.isInDailyList) {
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

                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF3C3C3C).copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Priority Badge
                Text(
                    text = task.priority.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(task.priority.color)
                )
            }

            // Action Buttons
            Row {
                // Move Buttons
                if (showMoveToDaily) {
                    IconButton(onClick = onMoveToDaily) {
                        Icon(
                            Icons.Default.Today,
                            contentDescription = "Zu Heute",
                            tint = Color(0xFF009966)
                        )
                    }
                }

                if (showMoveToBacklog) {
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

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Löschen",
                        tint = Color(0xFFE53E3E)
                    )
                }
            }
        }
    }
}