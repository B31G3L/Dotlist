package de.beigel.list.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List as ListIcon
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Segment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Timestamp
import de.beigel.list.data.Priority
import de.beigel.list.data.TodoItem
import de.beigel.list.data.TodoList
import de.beigel.list.data.displayNameFor
import de.beigel.list.repository.TodoRepository
import de.beigel.list.ui.theme.priorityColor
import de.beigel.list.utils.HapticFeedback
import de.beigel.list.viewmodel.TodosViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Voreingestellte Erinnerungs-Optionen (Anzeigename zu Minuten-Vorlauf). */
private val DetailReminderOptions: List<Pair<String, Int?>> = listOf(
    "Keine"           to null,
    "10 Min vorher"   to 10,
    "30 Min vorher"   to 30,
    "1 Stunde vorher" to 60,
    "1 Tag vorher"    to 1440,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AufgabeDetailScreen(
    list            : TodoList,
    todo            : TodoItem,
    repository      : TodoRepository,
    currentDeviceId : String,
    haptic          : HapticFeedback,
    onBack          : () -> Unit,
    allLists        : List<TodoList> = listOf(list),
) {
    val todoVm: TodosViewModel = viewModel(
        key     = "detail_task_${list.id}",
        factory = TodosViewModel.Factory(repository, list.id)
    )
    val uiState by todoVm.uiState.collectAsStateWithLifecycle()

    // Immer die aktuelle Version aus dem Live-Stream nehmen, Fallback auf den übergebenen Stand
    val liveTodo = uiState.todos.find { it.id == todo.id } ?: todo

    var title       by remember(liveTodo.id) { mutableStateOf(liveTodo.title) }
    var description by remember(liveTodo.id) { mutableStateOf(liveTodo.description) }
    var showDescriptionField by remember(liveTodo.id) { mutableStateOf(liveTodo.description.isNotBlank()) }
    var priority    by remember(liveTodo.id) { mutableStateOf(Priority.fromString(liveTodo.priority)) }
    var dueDateCal  by remember(liveTodo.id) {
        mutableStateOf(liveTodo.dueDate?.let { ts -> Calendar.getInstance().apply { time = ts.toDate() } })
    }
    var assignedTo      by remember(liveTodo.id) { mutableStateOf(liveTodo.assignedTo) }
    var reminderMinutes by remember(liveTodo.id) { mutableStateOf(liveTodo.reminderMinutes) }

    var showMenu          by remember { mutableStateOf(false) }
    var showListMenu       by remember { mutableStateOf(false) }
    var showDatePicker    by remember { mutableStateOf(false) }
    var showTimePicker    by remember { mutableStateOf(false) }
    var showAssignMenu    by remember { mutableStateOf(false) }
    var showReminderMenu  by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val members = list.memberIds

    fun save() {
        val ts = dueDateCal?.let { Timestamp(it.time) }
        todoVm.updateTodo(
            todoId          = liveTodo.id,
            title           = title,
            description     = description,
            priority        = priority,
            dueDate         = ts,
            assignedTo      = assignedTo,
            reminderMinutes = reminderMinutes,
        )
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        // ── Kopfzeile ────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { haptic.tick(); save(); onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Mehr")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text        = { Text("Löschen", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick     = { showMenu = false; showDeleteConfirm = true }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Checkbox + Titel ──────────────────────────────────────
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (liveTodo.isDone) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable {
                            haptic.tick()
                            todoVm.toggleTodo(liveTodo)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (liveTodo.isDone) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                    } else {
                        Surface(
                            modifier = Modifier.size(26.dp), shape = CircleShape, color = Color.Transparent,
                            border   = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                        ) {}
                    }
                }
                TextField(
                    value         = title,
                    onValueChange = { title = it },
                    textStyle     = androidx.compose.ui.text.TextStyle(
                        fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                        textDecoration = if (liveTodo.isDone) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    singleLine    = false,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor   = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Beschreibung ──────────────────────────────────────────
            if (showDescriptionField) {
                OutlinedTextField(
                    value         = description,
                    onValueChange = { description = it },
                    placeholder   = { Text("Beschreibung") },
                    minLines      = 2,
                    modifier      = Modifier.fillMaxWidth()
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDescriptionField = true }
                        .padding(vertical = 10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(Icons.Default.Segment, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Beschreibung hinzufügen …",
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Detail-Karte ──────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Column {
                    DetailClickRow(
                        icon    = Icons.Default.ListIcon,
                        label   = "Liste",
                        value   = list.name,
                        onClick = { if (allLists.size > 1) showListMenu = true }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Priorität ─────────────────────────────────────────────
            Text(
                "PRIORITÄT",
                fontSize      = 12.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color         = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Priority.entries.forEach { p ->
                    DetailPriorityChip(
                        priority = p,
                        selected = priority == p,
                        onClick  = { priority = p; save() }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Column {
                    DetailClickRow(
                        icon    = Icons.Default.DateRange,
                        label   = "Fällig",
                        value   = dueDateCal?.let { formatDueDate(it) } ?: "Kein Datum",
                        onClick = { showDatePicker = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    DetailClickRow(
                        icon    = Icons.Default.Person,
                        label   = "Zugewiesen",
                        value   = assigneeLabelFor(assignedTo, currentDeviceId, list),
                        onClick = { if (members.isNotEmpty()) showAssignMenu = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    DetailClickRow(
                        icon    = Icons.Default.Notifications,
                        label   = "Erinnerung",
                        value   = DetailReminderOptions.firstOrNull { it.second == reminderMinutes }?.first ?: "Keine",
                        onClick = { showReminderMenu = true }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Liste wählen ─────────────────────────────────────────────────────
    if (showListMenu) {
        DetailChoiceDialog(title = "Liste wählen", onDismiss = { showListMenu = false }) {
            allLists.forEach { l ->
                DetailChoiceRow(
                    label   = l.name,
                    onClick = {
                        showListMenu = false
                        if (l.id != list.id) {
                            todoVm.moveTodo(liveTodo, l.id) { onBack() }
                        }
                    }
                )
            }
        }
    }

    // ── Zuweisen wählen ────────────────────────────────────────────────
    if (showAssignMenu) {
        DetailChoiceDialog(title = "Zuweisen an", onDismiss = { showAssignMenu = false }) {
            DetailChoiceRow(label = "Niemand", onClick = { assignedTo = null; showAssignMenu = false; save() })
            members.forEach { memberId ->
                DetailChoiceRow(
                    label   = if (memberId == currentDeviceId) "Ich" else list.displayNameFor(memberId),
                    onClick = { assignedTo = memberId; showAssignMenu = false; save() }
                )
            }
        }
    }

    // ── Erinnerung wählen ────────────────────────────────────────────────
    if (showReminderMenu) {
        DetailChoiceDialog(title = "Erinnerung", onDismiss = { showReminderMenu = false }) {
            DetailReminderOptions.forEach { (label, minutes) ->
                DetailChoiceRow(label = label, onClick = { reminderMinutes = minutes; showReminderMenu = false; save() })
            }
        }
    }

    // ── Datum wählen ─────────────────────────────────────────────────────
    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (dueDateCal ?: Calendar.getInstance()).timeInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        val cal = Calendar.getInstance().apply { timeInMillis = millis }
                        val existing = dueDateCal
                        if (existing != null) {
                            cal.set(Calendar.HOUR_OF_DAY, existing.get(Calendar.HOUR_OF_DAY))
                            cal.set(Calendar.MINUTE, existing.get(Calendar.MINUTE))
                        }
                        dueDateCal = cal
                    }
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Weiter") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") } }
        ) {
            DatePicker(state = state)
        }
    }

    // ── Uhrzeit wählen ───────────────────────────────────────────────────
    if (showTimePicker) {
        val base = dueDateCal ?: Calendar.getInstance()
        val timeState = rememberTimePickerState(
            initialHour   = base.get(Calendar.HOUR_OF_DAY),
            initialMinute = base.get(Calendar.MINUTE),
            is24Hour      = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val cal = (dueDateCal ?: Calendar.getInstance()).apply {
                        set(Calendar.HOUR_OF_DAY, timeState.hour)
                        set(Calendar.MINUTE, timeState.minute)
                    }
                    dueDateCal = cal
                    showTimePicker = false
                    save()
                }) { Text("Übernehmen") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Abbrechen") } },
            text = { TimePicker(state = timeState) }
        )
    }

    // ── Löschen bestätigen ───────────────────────────────────────────────
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title   = { Text("Aufgabe löschen?") },
            text    = { Text("„${liveTodo.title}“ wird endgültig gelöscht.") },
            confirmButton = {
                TextButton(onClick = {
                    haptic.heavy()
                    todoVm.deleteTodo(liveTodo.id)
                    showDeleteConfirm = false
                    onBack()
                }) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Abbrechen") } }
        )
    }
}

@Composable
private fun DetailClickRow(
    icon    : androidx.compose.ui.graphics.vector.ImageVector,
    label   : String,
    value   : String,
    onClick : () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DetailPriorityChip(priority: Priority, selected: Boolean, onClick: () -> Unit) {
    val color = priorityColor(priority)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!selected) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            }
            Text(
                priority.label,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                color      = if (selected) Color(0xFF201A17) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailChoiceDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text(title) },
        text             = { Column(content = content) },
        confirmButton    = { TextButton(onClick = onDismiss) { Text("Fertig") } }
    )
}

@Composable
private fun DetailChoiceRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp)
    }
}

private fun assigneeLabelFor(assignedTo: String?, currentDeviceId: String, list: TodoList): String = when {
    assignedTo == null            -> "Niemand"
    assignedTo == currentDeviceId -> "Ich"
    else                            -> list.displayNameFor(assignedTo)
}

private fun formatDueDate(cal: Calendar): String {
    val fmt = SimpleDateFormat("EEE, d. MMM · HH:mm", Locale.GERMAN)
    return fmt.format(cal.time)
}