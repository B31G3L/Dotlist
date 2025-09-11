package de.beigel.list.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.beigel.list.data.TaskEntity
import de.beigel.list.data.TaskPriority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskDialog(
    isEditing: Boolean = false,
    initialTask: TaskEntity? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, TaskPriority) -> Unit,
    showDestinationChoice: Boolean = false,
    initialAddToDaily: Boolean = true
) {
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var selectedPriority by remember { mutableStateOf(initialTask?.priority ?: TaskPriority.MEDIUM) }
    var addToDaily by remember { mutableStateOf(initialAddToDaily) }

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
                Text(
                    text = if (isEditing) "Aufgabe bearbeiten" else "Neue Aufgabe",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF3C3C3C)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF009966),
                        focusedLabelColor = Color(0xFF009966)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Beschreibung (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF009966),
                        focusedLabelColor = Color(0xFF009966)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Priority Selection
                Text(
                    text = "Priorität",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF3C3C3C)
                )

                Spacer(modifier = Modifier.height(8.dp))

                TaskPriority.values().forEach { priority ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedPriority == priority,
                                onClick = { selectedPriority = priority }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPriority == priority,
                            onClick = { selectedPriority = priority },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF009966)
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = priority.displayName,
                            color = Color(priority.color),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Destination Choice (nur beim Hinzufügen neuer Aufgaben)
                if (showDestinationChoice && !isEditing) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Hinzufügen zu",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF3C3C3C)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = addToDaily,
                            onClick = { addToDaily = true },
                            label = { Text("Heute") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Today,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF009966),
                                selectedLabelColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        FilterChip(
                            selected = !addToDaily,
                            onClick = { addToDaily = false },
                            label = { Text("Backlog") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Inventory,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF009966),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Abbrechen", color = Color(0xFF3C3C3C))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(title, description, selectedPriority)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF009966)
                        )
                    ) {
                        Text(if (isEditing) "Speichern" else "Hinzufügen")
                    }
                }
            }
        }
    }
}