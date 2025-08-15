package de.beigel.list.ui.dialogs

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.beigel.list.data.TaskEntity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun TaskDetailsDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
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
            ) {
                // Header with Priority
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(task.priority.color))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFF3C3C3C),
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = task.priority.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(task.priority.color)
                        )
                    }

                    // Status Icon
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (task.isCompleted) "Erledigt" else "Offen",
                        tint = if (task.isCompleted) Color(0xFF009966) else Color(0xFF3C3C3C).copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Description
                if (task.description.isNotBlank()) {
                    Text(
                        text = "Beschreibung",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF3C3C3C),
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF009966).copy(alpha = 0.05f)
                        )
                    ) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF3C3C3C),
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Timestamps
                Column {
                    Text(
                        text = "Details",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF3C3C3C),
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Erstellt:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF3C3C3C).copy(alpha = 0.7f)
                        )

                        Text(
                            text = try {
                                LocalDateTime.parse(task.createdAt).format(
                                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                                )
                            } catch (e: Exception) {
                                "Unbekannt"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF3C3C3C)
                        )
                    }

                    if (task.isCompleted && task.completedAt != null) {
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Erledigt:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF3C3C3C).copy(alpha = 0.7f)
                            )

                            Text(
                                text = try {
                                    LocalDateTime.parse(task.completedAt).format(
                                        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                                    )
                                } catch (e: Exception) {
                                    "Unbekannt"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF009966)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Schließen", color = Color(0xFF3C3C3C))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            onEdit()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF009966)
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
                }
            }
        }
    }
}