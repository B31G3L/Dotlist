package de.beigel.list.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
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
    onSave: (String, String, TaskPriority) -> Unit
) {
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var selectedPriority by remember { mutableStateOf(initialTask?.priority ?: TaskPriority.MEDIUM) }

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