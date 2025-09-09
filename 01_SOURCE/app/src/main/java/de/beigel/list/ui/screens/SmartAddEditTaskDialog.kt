package de.beigel.list.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.beigel.list.data.*
import de.beigel.list.utils.NaturalLanguageProcessor
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAddEditTaskDialog(
    task: TaskEntity? = null,
    availableTags: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        priority: TaskPriority,
        dueDate: String?,
        estimatedMinutes: Int?,
        energyLevel: EnergyLevel,
        context: TaskContext?,
        tags: List<String>
    ) -> Unit
) {
    val isEditing = task != null
    val nlpProcessor = remember { NaturalLanguageProcessor() }

    // Form State
    var rawInput by remember { mutableStateOf("") }
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var selectedPriority by remember { mutableStateOf(task?.priority ?: TaskPriority.MEDIUM) }
    var dueDate by remember { mutableStateOf(task?.dueDate) }
    var estimatedMinutes by remember { mutableStateOf(task?.estimatedMinutes?.toString() ?: "") }
    var selectedEnergyLevel by remember { mutableStateOf(task?.energyLevel ?: EnergyLevel.MEDIUM) }
    var selectedContext by remember { mutableStateOf(task?.context) }
    var selectedTags by remember { mutableStateOf(task?.getTags() ?: emptyList()) }

    // UI State
    var showAdvancedOptions by remember { mutableStateOf(isEditing) }
    var showDatePicker by remember { mutableStateOf(false) }
    var useNaturalInput by remember { mutableStateOf(!isEditing && title.isEmpty()) }
    var nlpSuggestion by remember { mutableStateOf<ParsedTask?>(null) }

    // Natural Language Processing
    LaunchedEffect(rawInput) {
        if (rawInput.isNotBlank() && useNaturalInput) {
            val parsed = nlpProcessor.parseTaskInput(rawInput)
            if (parsed.isValid()) {
                nlpSuggestion = parsed
                title = parsed.title
                description = parsed.description
                dueDate = parsed.dueDate
                selectedPriority = parsed.priority
                estimatedMinutes = parsed.estimatedMinutes?.toString() ?: ""
                selectedContext = parsed.context
            }
        }
    }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Aufgabe bearbeiten" else "Neue Aufgabe",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Schließen")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Natural Language Input (nur für neue Aufgaben)
                if (!isEditing) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (useNaturalInput)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Switch(
                                    checked = useNaturalInput,
                                    onCheckedChange = { useNaturalInput = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "🎙️ Smart-Eingabe",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (useNaturalInput) {
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = rawInput,
                                    onValueChange = { rawInput = it },
                                    label = { Text("Was möchtest du machen?") },
                                    placeholder = {
                                        Text("z.B. 'Morgen Präsentation vorbereiten - 2h - wichtig'")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2
                                )

                                nlpSuggestion?.let { suggestion ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "✅ Erkannt: ${suggestion.summary()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Standard Form Fields
                if (!useNaturalInput || isEditing) {
                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Titel *") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = title.isBlank(),
                        supportingText = if (title.isBlank()) {
                            { Text("Titel ist erforderlich") }
                        } else null
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Beschreibung (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Priority Selection
                Text(
                    text = "Priorität",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskPriority.values().forEach { priority ->
                        FilterChip(
                            selected = selectedPriority == priority,
                            onClick = { selectedPriority = priority },
                            label = { Text(priority.displayName) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            Color(priority.color),
                                            RoundedCornerShape(6.dp)
                                        )
                                )
                            }
                        )
                    }
                }

                // Advanced Options Toggle
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvancedOptions = !showAdvancedOptions },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Erweiterte Optionen",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        if (showAdvancedOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showAdvancedOptions) "Weniger" else "Mehr"
                    )
                }

                // Advanced Options
                if (showAdvancedOptions) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Due Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Fälligkeitsdatum",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = dueDate?.let {
                                    LocalDate.parse(it).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                                } ?: "Kein Datum",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }

                        Row {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Datum wählen")
                            }
                            if (dueDate != null) {
                                IconButton(onClick = { dueDate = null }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Datum entfernen")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Estimated Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = estimatedMinutes,
                            onValueChange = { estimatedMinutes = it.filter { char -> char.isDigit() } },
                            label = { Text("Geschätzte Zeit") },
                            placeholder = { Text("z.B. 30") },
                            trailingIcon = { Text("min") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        // Quick Time Buttons
                        QuickTimeButtons(
                            onTimeSelected = { minutes ->
                                estimatedMinutes = minutes.toString()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Energy Level
                    Text(
                        text = "Benötigte Energie",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EnergyLevel.values().forEach { level ->
                            FilterChip(
                                selected = selectedEnergyLevel == level,
                                onClick = { selectedEnergyLevel = level },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(level.icon)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(level.displayName)
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Context
                    Text(
                        text = "Arbeitsbereich",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedContext == null,
                                onClick = { selectedContext = null },
                                label = { Text("Kein") }
                            )
                        }

                        items(TaskContext.values()) { context ->
                            FilterChip(
                                selected = selectedContext == context,
                                onClick = { selectedContext = context },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(context.icon)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(context.displayName)
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tags
                    TagSelectionSection(
                        selectedTags = selectedTags,
                        availableTags = availableTags,
                        onTagsChanged = { selectedTags = it }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Abbrechen")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(
                                    title.trim(),
                                    description.trim(),
                                    selectedPriority,
                                    dueDate,
                                    estimatedMinutes.toIntOrNull(),
                                    selectedEnergyLevel,
                                    selectedContext,
                                    selectedTags
                                )
                            }
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text(if (isEditing) "Speichern" else "Hinzufügen")
                    }
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { date ->
                dueDate = date
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
            initialDate = dueDate
        )
    }
}

@Composable
fun QuickTimeButtons(onTimeSelected: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(15, 30, 60).forEach { minutes ->
            AssistChip(
                onClick = { onTimeSelected(minutes) },
                label = {
                    Text(
                        text = if (minutes < 60) "${minutes}m" else "${minutes/60}h",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                modifier = Modifier.size(width = 48.dp, height = 32.dp)
            )
        }
    }
}

@Composable
fun TagSelectionSection(
    selectedTags: List<String>,
    availableTags: List<String>,
    onTagsChanged: (List<String>) -> Unit
) {
    var newTagText by remember { mutableStateOf("") }

    Column {
        Text(
            text = "Tags",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Selected Tags
        if (selectedTags.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedTags) { tag ->
                    AssistChip(
                        onClick = {
                            onTagsChanged(selectedTags - tag)
                        },
                        label = { Text(tag) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Entfernen",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Available Tags
        if (availableTags.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableTags.filter { it !in selectedTags }) { tag ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            onTagsChanged(selectedTags + tag)
                        },
                        label = { Text(tag) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Add New Tag
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newTagText,
                onValueChange = { newTagText = it },
                label = { Text("Neuer Tag") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (newTagText.isNotBlank() && newTagText !in selectedTags) {
                        onTagsChanged(selectedTags + newTagText.trim())
                        newTagText = ""
                    }
                },
                enabled = newTagText.isNotBlank()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tag hinzufügen")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    initialDate: String? = null
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.let {
            LocalDate.parse(it).toEpochDay() * 24 * 60 * 60 * 1000
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                        onDateSelected(date.toString())
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

// Datenklasse für Natural Language Processing
data class ParsedTask(
    val title: String = "",
    val description: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDate: String? = null,
    val estimatedMinutes: Int? = null,
    val context: TaskContext? = null,
    val tags: List<String> = emptyList()
) {
    fun isValid(): Boolean = title.isNotBlank()

    fun summary(): String {
        val parts = mutableListOf<String>()
        parts.add(title)

        dueDate?.let { date ->
            val localDate = LocalDate.parse(date)
            val today = LocalDate.now()
            val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, localDate)

            parts.add(when {
                daysUntil == 0L -> "heute"
                daysUntil == 1L -> "morgen"
                daysUntil == -1L -> "gestern"
                daysUntil > 0 -> "in ${daysUntil} Tagen"
                else -> "vor ${-daysUntil} Tagen"
            })
        }

        estimatedMinutes?.let { minutes ->
            parts.add("${minutes} Min")
        }

        if (priority != TaskPriority.MEDIUM) {
            parts.add(priority.displayName.lowercase())
        }

        context?.let { ctx ->
            parts.add(ctx.displayName.lowercase())
        }

        return parts.joinToString(" • ")
    }
}