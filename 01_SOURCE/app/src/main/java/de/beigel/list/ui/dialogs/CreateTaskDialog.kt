package de.beigel.list.ui.dialogs

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.beigel.list.R
import de.beigel.list.data.*
import de.beigel.list.utils.TaskReasoning
import de.beigel.list.utils.NaturalLanguageProcessor
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskDialog(
    onDismiss: () -> Unit,
    onCreateTask: (
        title: String,
        description: String,
        priority: TaskPriority,
        dueDate: LocalDate?,
        estimatedMinutes: Int?,
        energyLevel: EnergyLevel,
        context: TaskContext,
        tags: List<String>
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var dueDate by remember { mutableStateOf<LocalDate?>(null) }
    var estimatedMinutes by remember { mutableStateOf<Int?>(null) }
    var energyLevel by remember { mutableStateOf(EnergyLevel.MEDIUM) }
    var context by remember { mutableStateOf(TaskContext.NONE) }
    var tags by remember { mutableStateOf("") }
    var useSmartInput by remember { mutableStateOf(true) }

    // Smart Input Processing
    var parsedTask by remember { mutableStateOf<ParsedTask?>(null) }

    LaunchedEffect(title) {
        if (title.isNotBlank() && useSmartInput) {
            val processor = NaturalLanguageProcessor()
            parsedTask = processor.parseTaskInput(title)
        }
    }

    // Aktualisiere Felder basierend auf Smart Input
    LaunchedEffect(parsedTask) {
        parsedTask?.let { parsed ->
            if (parsed.priority != null) priority = parsed.priority
            if (parsed.dueDate != null) dueDate = parsed.dueDate
            if (parsed.estimatedMinutes != null) estimatedMinutes = parsed.estimatedMinutes
            if (parsed.energyLevel != null) energyLevel = parsed.energyLevel
            if (parsed.context != null) context = parsed.context
            if (parsed.tags.isNotEmpty()) tags = parsed.tags.joinToString(", ")
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Text(
                    text = stringResource(R.string.new_task),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Smart Input Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Switch(
                        checked = useSmartInput,
                        onCheckedChange = { useSmartInput = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.smart_input),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (useSmartInput) {
                    Text(
                        text = stringResource(R.string.smart_input_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.task_title)) },
                    placeholder = {
                        Text(
                            if (useSmartInput) stringResource(R.string.smart_input_hint)
                            else stringResource(R.string.task_title_hint)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )

                // Smart Parsing Preview
                if (useSmartInput && parsedTask != null) {
                    SmartParsingPreview(
                        parsedTask = parsedTask!!,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.task_description_optional)) },
                    placeholder = { Text(stringResource(R.string.task_description_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Priority Selection
                PrioritySelector(
                    selectedPriority = priority,
                    onPriorityChange = { priority = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Due Date
                DueDateSelector(
                    selectedDate = dueDate,
                    onDateChange = { dueDate = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Estimated Time
                EstimatedTimeSelector(
                    estimatedMinutes = estimatedMinutes,
                    onTimeChange = { estimatedMinutes = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Energy Level
                EnergyLevelSelector(
                    selectedEnergyLevel = energyLevel,
                    onEnergyLevelChange = { energyLevel = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Context
                ContextSelector(
                    selectedContext = context,
                    onContextChange = { context = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tags
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text(stringResource(R.string.task_tags)) },
                    placeholder = { Text("Tag1, Tag2, Tag3") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val tagsList = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                onCreateTask(
                                    title.trim(),
                                    description.trim(),
                                    priority,
                                    dueDate,
                                    estimatedMinutes,
                                    energyLevel,
                                    context,
                                    tagsList
                                )
                            }
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}

@Composable
fun EditTaskDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onUpdateTask: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }
    var priority by remember { mutableStateOf(task.priority) }
    var dueDate by remember { mutableStateOf(task.dueDate) }
    var estimatedMinutes by remember { mutableStateOf(task.estimatedMinutes) }
    var energyLevel by remember { mutableStateOf(task.energyLevel) }
    var context by remember { mutableStateOf(task.context) }
    var tags by remember { mutableStateOf(task.tags.joinToString(", ")) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.edit_task),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = { onDeleteTask(task) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_task),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Task Details (similar to CreateTaskDialog but with existing values)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.task_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.task_description_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                PrioritySelector(
                    selectedPriority = priority,
                    onPriorityChange = { priority = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                DueDateSelector(
                    selectedDate = dueDate,
                    onDateChange = { dueDate = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                EstimatedTimeSelector(
                    estimatedMinutes = estimatedMinutes,
                    onTimeChange = { estimatedMinutes = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                EnergyLevelSelector(
                    selectedEnergyLevel = energyLevel,
                    onEnergyLevelChange = { energyLevel = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                ContextSelector(
                    selectedContext = context,
                    onContextChange = { context = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text(stringResource(R.string.task_tags)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val tagsList = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                val updatedTask = task.copy(
                                    title = title.trim(),
                                    description = description.trim(),
                                    priority = priority,
                                    dueDate = dueDate,
                                    estimatedMinutes = estimatedMinutes,
                                    energyLevel = energyLevel,
                                    context = context,
                                    tags = tagsList,
                                    updatedAt = LocalDateTime.now()
                                )
                                onUpdateTask(updatedTask)
                            }
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onConfirmDelete: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.delete_task))
        },
        text = {
            Text("Möchtest du '${task.title}' wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.")
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmDelete(task.id) }
            ) {
                Text(
                    stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun TaskReasoningDialog(
    reasoning: TaskReasoning,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.smart_reasoning),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Task Title
                Text(
                    text = reasoning.task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                        .fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Score
                Text(
                    text = "Smart Score: ${reasoning.totalScore.toInt()}/100",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Reasons
                Text(
                    text = "Warum ist diese Aufgabe wichtig?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                reasoning.reasons.forEach { reason ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        }
    }
}

// Helper Composables for Form Elements

@Composable
private fun PrioritySelector(
    selectedPriority: TaskPriority,
    onPriorityChange: (TaskPriority) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.task_priority),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TaskPriority.values().forEach { priority ->
                val isSelected = priority == selectedPriority
                val (text, color) = when (priority) {
                    TaskPriority.HIGH -> stringResource(R.string.priority_high) to MaterialTheme.colorScheme.error
                    TaskPriority.MEDIUM -> stringResource(R.string.priority_medium) to MaterialTheme.colorScheme.primary
                    TaskPriority.LOW -> stringResource(R.string.priority_low) to MaterialTheme.colorScheme.outline
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { onPriorityChange(priority) },
                    label = { Text(text) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DueDateSelector(
    selectedDate: LocalDate?,
    onDateChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.task_due_date),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    selectedDate?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                        ?: "Datum wählen"
                )
            }

            if (selectedDate != null) {
                IconButton(onClick = { onDateChange(null) }) {
                    Icon(Icons.Default.Clear, contentDescription = "Entfernen")
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate?.toEpochDay()?.times(24 * 60 * 60 * 1000)
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                            onDateChange(date)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Abbrechen")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun EstimatedTimeSelector(
    estimatedMinutes: Int?,
    onTimeChange: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.task_estimated_time),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val quickTimes = listOf(15, 30, 60, 120)

            quickTimes.forEach { minutes ->
                FilterChip(
                    selected = estimatedMinutes == minutes,
                    onClick = {
                        onTimeChange(if (estimatedMinutes == minutes) null else minutes)
                    },
                    label = { Text("${minutes}m") }
                )
            }
        }
    }
}

@Composable
private fun EnergyLevelSelector(
    selectedEnergyLevel: EnergyLevel,
    onEnergyLevelChange: (EnergyLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.energy_required),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EnergyLevel.values().forEach { energy ->
                val isSelected = energy == selectedEnergyLevel
                val (text, icon) = when (energy) {
                    EnergyLevel.HIGH -> stringResource(R.string.energy_high) to "⚡"
                    EnergyLevel.MEDIUM -> stringResource(R.string.energy_medium) to "🔋"
                    EnergyLevel.LOW -> stringResource(R.string.energy_low) to "💤"
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { onEnergyLevelChange(energy) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(icon)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ContextSelector(
    selectedContext: TaskContext,
    onContextChange: (TaskContext) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.task_context),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Show most common contexts in chips
        val commonContexts = listOf(
            TaskContext.WORK, TaskContext.HOME, TaskContext.COMPUTER, TaskContext.CALLS
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(commonContexts) { context ->
                val (text, icon) = when (context) {
                    TaskContext.WORK -> stringResource(R.string.context_work) to Icons.Default.Work
                    TaskContext.HOME -> stringResource(R.string.context_home) to Icons.Default.Home
                    TaskContext.COMPUTER -> stringResource(R.string.context_computer) to Icons.Default.Computer
                    TaskContext.CALLS -> stringResource(R.string.context_calls) to Icons.Default.Phone
                    else -> context.name to Icons.Default.Category
                }

                FilterChip(
                    selected = selectedContext == context,
                    onClick = { onContextChange(context) },
                    label = { Text(text) },
                    leadingIcon = {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }
        }
    }
}

@Composable
private fun SmartParsingPreview(
    parsedTask: ParsedTask,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "🤖 Smart-Erkennung:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            val detectedItems = mutableListOf<String>()

            parsedTask.priority?.let { detectedItems.add("Priorität: ${it.name}") }
            parsedTask.dueDate?.let { detectedItems.add("Fällig: ${it.format(DateTimeFormatter.ofPattern("dd.MM"))}") }
            parsedTask.estimatedMinutes?.let { detectedItems.add("Zeit: ${it}min") }
            parsedTask.energyLevel?.let { detectedItems.add("Energie: ${it.name}") }
            parsedTask.context?.let { detectedItems.add("Kontext: ${it.name}") }
            if (parsedTask.tags.isNotEmpty()) { detectedItems.add("Tags: ${parsedTask.tags.joinToString(", ")}") }

            if (detectedItems.isNotEmpty()) {
                Text(
                    text = detectedItems.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Text(
                    text = "Gib Details wie 'morgen', 'wichtig', '30min' ein für automatische Erkennung",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

// Data class for parsed task information
data class ParsedTask(
    val cleanTitle: String,
    val priority: TaskPriority? = null,
    val dueDate: LocalDate? = null,
    val estimatedMinutes: Int? = null,
    val energyLevel: EnergyLevel? = null,
    val context: TaskContext? = null,
    val tags: List<String> = emptyList()
)