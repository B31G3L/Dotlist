package de.beigel.list.ui.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.beigel.list.data.TaskEntity
import de.beigel.list.data.getTags
import de.beigel.list.data.isQuickWin
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun SmartTaskDetailsDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onEdit: (TaskEntity) -> Unit,
    onDelete: (TaskEntity) -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                TaskHeaderSection(
                    task = task,
                    onDismiss = onDismiss
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Status Section
                TaskStatusSection(task = task)

                Spacer(modifier = Modifier.height(20.dp))

                // Description
                if (task.description.isNotBlank()) {
                    TaskDescriptionSection(task = task)
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Metadata
                TaskMetadataSection(task = task)

                Spacer(modifier = Modifier.height(20.dp))

                // Timeline
                TaskTimelineSection(task = task)

                Spacer(modifier = Modifier.height(20.dp))

                // Properties
                TaskPropertiesSection(task = task)

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                TaskActionButtons(
                    task = task,
                    onEdit = { onEdit(task) },
                    onDelete = { showDeleteConfirmation = true },
                    onDismiss = onDismiss
                )
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Aufgabe löschen?") },
            text = {
                Text("Möchtest du die Aufgabe '${task.title}' wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(task)
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
fun TaskHeaderSection(
    task: TaskEntity,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Priority Indicator
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(task.priority.color))
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Title
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = task.priority.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(task.priority.color),
                    fontWeight = FontWeight.Medium
                )

                task.context?.let { context ->
                    Text(
                        text = "${context.icon} ${context.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Close Button
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Schließen")
        }
    }
}

@Composable
fun TaskStatusSection(task: TaskEntity) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (task.isCompleted) "Erledigt" else "Offen",
                tint = if (task.isCompleted)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (task.isCompleted) "✅ Erledigt!" else "⏳ Ausstehend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (task.isCompleted)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSecondaryContainer
                )

                Text(
                    text = if (task.isCompleted)
                        "Großartige Arbeit! Diese Aufgabe ist abgeschlossen."
                    else "Diese Aufgabe wartet noch auf dich.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (task.isCompleted)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun TaskDescriptionSection(task: TaskEntity) {
    Column {
        Text(
            text = "📝 Beschreibung",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = task.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun TaskMetadataSection(task: TaskEntity) {
    Column {
        Text(
            text = "📊 Details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Due Date
        task.dueDate?.let { dueDate ->
            val days = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(dueDate))
            MetadataItem(
                icon = "📅",
                title = "Fälligkeitsdatum",
                value = LocalDate.parse(dueDate).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                subtitle = when {
                    days < 0 -> "Überfällig seit ${-days} Tag(en)"
                    days == 0L -> "Heute fällig"
                    days == 1L -> "Morgen fällig"
                    days <= 7 -> "Fällig in $days Tagen"
                    else -> "Fällig in $days Tagen"
                },
                color = when {
                    days < 0 -> MaterialTheme.colorScheme.error
                    days <= 1 -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        }

        // Estimated Time
        task.estimatedMinutes?.let { minutes ->
            MetadataItem(
                icon = "⏱️",
                title = "Geschätzte Zeit",
                value = when {
                    minutes < 60 -> "$minutes Minuten"
                    minutes == 60 -> "1 Stunde"
                    minutes < 120 -> "1h ${minutes % 60}min"
                    else -> "${minutes / 60}h ${minutes % 60}min"
                },
                subtitle = when {
                    minutes <= 15 -> "Quick Win - perfekt für Zwischendurch"
                    minutes <= 30 -> "Kurze Aufgabe"
                    minutes <= 60 -> "Normale Aufgabe"
                    minutes <= 120 -> "Längere Aufgabe - plane genug Zeit ein"
                    else -> "Große Aufgabe - eventuell aufteilen"
                },
                color = when {
                    minutes <= 15 -> Color(0xFF4CAF50)
                    minutes <= 60 -> MaterialTheme.colorScheme.primary
                    else -> Color(0xFFFF9800)
                }
            )
        }

        // Actual Time (if completed)
        if (task.isCompleted && task.actualMinutes != null) {
            MetadataItem(
                icon = "⏰",
                title = "Tatsächliche Zeit",
                value = "${task.actualMinutes} Minuten",
                subtitle = task.estimatedMinutes?.let { estimated ->
                    val diff = task.actualMinutes!! - estimated
                    when {
                        diff > 0 -> "$diff Minuten länger als geschätzt"
                        diff < 0 -> "${-diff} Minuten schneller als geschätzt"
                        else -> "Genau wie geschätzt!"
                    }
                } ?: "Keine Schätzung vorhanden",
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Energy Level
        MetadataItem(
            icon = task.energyLevel.icon,
            title = "Benötigte Energie",
            value = task.energyLevel.displayName,
            subtitle = when (task.energyLevel) {
                de.beigel.list.data.EnergyLevel.LOW -> "Kann auch bei niedriger Energie erledigt werden"
                de.beigel.list.data.EnergyLevel.MEDIUM -> "Benötigt normale Konzentration"
                de.beigel.list.data.EnergyLevel.HIGH -> "Am besten bei hoher Energie und Konzentration"
            },
            color = when (task.energyLevel) {
                de.beigel.list.data.EnergyLevel.LOW -> Color(0xFF4CAF50)
                de.beigel.list.data.EnergyLevel.MEDIUM -> MaterialTheme.colorScheme.primary
                de.beigel.list.data.EnergyLevel.HIGH -> Color(0xFFE53E3E)
            }
        )

        // Tags
        if (task.getTags().isNotEmpty()) {
            MetadataItem(
                icon = "🏷️",
                title = "Tags",
                value = task.getTags().joinToString(", "),
                subtitle = "${task.getTags().size} Tag(s)",
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun MetadataItem(
    icon: String,
    title: String,
    value: String,
    subtitle: String,
    color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun TaskTimelineSection(task: TaskEntity) {
    Column {
        Text(
            text = "⏳ Zeitlinie",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Created
                TimelineItem(
                    icon = Icons.Default.Add,
                    title = "Erstellt",
                    time = try {
                        LocalDateTime.parse(task.createdAt).format(
                            DateTimeFormatter.ofPattern("dd.MM.yyyy 'um' HH:mm")
                        )
                    } catch (e: Exception) {
                        "Unbekannt"
                    },
                    description = "Aufgabe wurde hinzugefügt",
                    color = MaterialTheme.colorScheme.primary,
                    isCompleted = true
                )

                if (task.lastModified != task.createdAt) {
                    TimelineItem(
                        icon = Icons.Default.Edit,
                        title = "Zuletzt bearbeitet",
                        time = try {
                            LocalDateTime.parse(task.lastModified).format(
                                DateTimeFormatter.ofPattern("dd.MM.yyyy 'um' HH:mm")
                            )
                        } catch (e: Exception) {
                            "Unbekannt"
                        },
                        description = "Aufgabe wurde geändert",
                        color = MaterialTheme.colorScheme.secondary,
                        isCompleted = true
                    )
                }

                if (task.isCompleted && task.completedAt != null) {
                    TimelineItem(
                        icon = Icons.Default.CheckCircle,
                        title = "Erledigt",
                        time = try {
                            LocalDateTime.parse(task.completedAt!!).format(
                                DateTimeFormatter.ofPattern("dd.MM.yyyy 'um' HH:mm")
                            )
                        } catch (e: Exception) {
                            "Unbekannt"
                        },
                        description = "Aufgabe wurde abgeschlossen",
                        color = Color(0xFF4CAF50),
                        isCompleted = true
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    time: String,
    description: String,
    color: Color,
    isCompleted: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    if (isCompleted) color else color.copy(alpha = 0.3f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (isCompleted)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun TaskPropertiesSection(task: TaskEntity) {
    if (task.isRecurring || task.smartScore > 0) {
        Column {
            Text(
                text = "⚙️ Eigenschaften",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (task.isRecurring) {
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = "🔄 Wiederkehrend",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                }

                if (task.smartScore > 0) {
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = "🎯 Score: ${task.smartScore.toInt()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                }

                if (task.isQuickWin()) {
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = "⚡ Quick Win",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskActionButtons(
    task: TaskEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Edit Button
            Button(
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Bearbeiten")
            }

            // Delete Button
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Löschen")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Close Button
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Schließen")
        }
    }
}