package de.beigel.list.ui.dialogs

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.beigel.list.data.TaskPriority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, TaskPriority, Boolean) -> Unit,
    showDestinationChoice: Boolean = false,
    initialAddToDaily: Boolean = true
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var addToDaily by remember { mutableStateOf(initialAddToDaily) }

    // Expansion states für die verschiedenen Bereiche
    var showDescription by remember { mutableStateOf(false) }
    var showPriority by remember { mutableStateOf(false) }
    var showDestination by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current

    // Reset state when dialog closes
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            title = ""
            description = ""
            selectedPriority = TaskPriority.MEDIUM
            addToDaily = initialAddToDaily
            showDescription = false
            showPriority = false
            showDestination = false
        }
    }

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 32.dp, height = 4.dp)
                    )
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Header
                Text(
                    text = "Neue Aufgabe hinzufügen",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Haupteingabefeld für den Titel
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Was möchtest du erledigen?") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Icons für erweiterte Optionen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ExpandableOptionButton(
                        icon = Icons.Default.Description,
                        label = "Beschreibung",
                        isExpanded = showDescription,
                        onClick = { showDescription = !showDescription }
                    )

                    ExpandableOptionButton(
                        icon = Icons.Default.Flag,
                        label = "Priorität",
                        isExpanded = showPriority,
                        onClick = { showPriority = !showPriority },
                        badgeColor = Color(selectedPriority.color)
                    )

                    if (showDestinationChoice) {
                        ExpandableOptionButton(
                            icon = if (addToDaily) Icons.Default.Today else Icons.Default.Inventory,
                            label = if (addToDaily) "Heute" else "Backlog",
                            isExpanded = showDestination,
                            onClick = { showDestination = !showDestination }
                        )
                    }
                }

                // Erweiterte Eingabebereiche mit Animationen
                AnimatedVisibility(
                    visible = showDescription,
                    enter = expandVertically(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    ) + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Beschreibung (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showPriority,
                    enter = expandVertically(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    ) + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(
                            text = "Priorität auswählen",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TaskPriority.values().forEach { priority ->
                                PriorityChip(
                                    priority = priority,
                                    isSelected = selectedPriority == priority,
                                    onClick = { selectedPriority = priority },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                if (showDestinationChoice) {
                    AnimatedVisibility(
                        visible = showDestination,
                        enter = expandVertically(
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        ) + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text(
                                text = "Wo hinzufügen?",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

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
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Abbrechen")
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                keyboardController?.hide()
                                onSave(title, description, selectedPriority, addToDaily)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Hinzufügen")
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableOptionButton(
    icon: ImageVector,
    label: String,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeColor: Color? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable { onClick() }
    ) {
        Box {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onClick() }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(24.dp),
                    tint = if (isExpanded) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Badge für Prioritätsfarbe
            badgeColor?.let { color ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color, RoundedCornerShape(6.dp))
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isExpanded) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PriorityChip(
    priority: TaskPriority,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = priority.displayName,
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        Color(priority.color),
                        RoundedCornerShape(4.dp)
                    )
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(priority.color).copy(alpha = 0.2f),
            selectedLabelColor = Color(priority.color)
        ),
        modifier = modifier
    )
}