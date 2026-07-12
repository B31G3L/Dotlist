package com.beigel.dotlist.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List as ListIcon
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Segment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.Timestamp
import com.beigel.dotlist.R
import com.beigel.dotlist.data.Priority
import com.beigel.dotlist.data.TodoList
import com.beigel.dotlist.data.displayLabel
import com.beigel.dotlist.data.displayNameFor
import com.beigel.dotlist.ui.theme.priorityColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Voreingestellte Erinnerungs-Optionen (Anzeigename zu Minuten-Vorlauf). */
@Composable
private fun reminderOptions(): List<Pair<String, Int?>> = listOf(
    stringResource(R.string.reminder_none)    to null,
    stringResource(R.string.reminder_10_min)  to 10,
    stringResource(R.string.reminder_30_min)  to 30,
    stringResource(R.string.reminder_1_hour)  to 60,
    stringResource(R.string.reminder_1_day)   to 1440,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeueAufgabeScreen(
    lists           : List<TodoList>,
    initialListId   : String,
    currentDeviceId : String,
    onDismiss       : () -> Unit,
    onSave          : (
        listId          : String,
        title           : String,
        description     : String,
        priority        : Priority,
        dueDate         : Timestamp?,
        assignedTo      : String?,
        reminderMinutes : Int?,
    ) -> Unit,
) {
    var title       by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var showDescriptionField by remember { mutableStateOf(false) }
    var priority    by remember { mutableStateOf(Priority.MITTEL) }
    var selectedListId by remember { mutableStateOf(initialListId) }
    var dueDateCal  by remember { mutableStateOf<Calendar?>(null) }
    var dueHasTime  by remember { mutableStateOf(false) }
    var assignedTo  by remember { mutableStateOf<String?>(null) }
    var reminderMinutes by remember { mutableStateOf<Int?>(null) }

    var showListMenu     by remember { mutableStateOf(false) }
    var showDatePicker   by remember { mutableStateOf(false) }
    var showTimePicker   by remember { mutableStateOf(false) }
    var showAssignMenu   by remember { mutableStateOf(false) }
    var showReminderMenu by remember { mutableStateOf(false) }

    val ReminderOptions = reminderOptions()
    val context = androidx.compose.ui.platform.LocalContext.current

    val selectedList = lists.find { it.id == selectedListId } ?: lists.firstOrNull()
    val members = selectedList?.memberIds ?: emptyList()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color    = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Kopfzeile ────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close))
                        }
                        Text(
                            text       = stringResource(R.string.title_new_task),
                            fontSize   = 19.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    TextButton(
                        enabled = title.isNotBlank() && selectedList != null,
                        onClick = {
                            val list = selectedList ?: return@TextButton
                            val ts = dueDateCal?.let { Timestamp(it.time) }
                            onSave(list.id, title, description, priority, ts, assignedTo, reminderMinutes)
                        }
                    ) {
                        Text(stringResource(R.string.action_save), fontWeight = FontWeight.SemiBold)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── Titel ────────────────────────────────────────────
                    TextField(
                        value         = title,
                        onValueChange = { title = it },
                        placeholder   = { Text(stringResource(R.string.placeholder_task_title), fontSize = 22.sp) },
                        textStyle     = androidx.compose.ui.text.TextStyle(fontSize = 22.sp),
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor   = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor   = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )

                    Spacer(Modifier.height(20.dp))

                    // ── Beschreibung ─────────────────────────────────────
                    if (showDescriptionField) {
                        OutlinedTextField(
                            value         = description,
                            onValueChange = { description = it },
                            placeholder   = { Text(stringResource(R.string.placeholder_description)) },
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
                                stringResource(R.string.action_add_description),
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Priorität ────────────────────────────────────────
                    Text(
                        stringResource(R.string.section_priority),
                        fontSize      = 12.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color         = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Priority.entries.forEach { p ->
                            PriorityChip(
                                priority = p,
                                selected = priority == p,
                                onClick  = { priority = p }
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── Detail-Karte ─────────────────────────────────────
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ) {
                        Column {
                            DetailRow(
                                icon      = Icons.Default.ListIcon,
                                label     = stringResource(R.string.label_list),
                                value     = selectedList?.name ?: "–",
                                valueDot  = selectedList?.let { listColor(it.color) },
                                onClick   = { if (lists.size > 1) showListMenu = true }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            DetailRow(
                                icon    = Icons.Default.DateRange,
                                label   = stringResource(R.string.label_due),
                                value   = dueDateCal?.let { formatDueDateOnly(it) } ?: stringResource(R.string.label_no_date),
                                onClick = { showDatePicker = true },
                                onClear = if (dueDateCal != null) {
                                    { dueDateCal = null; dueHasTime = false }
                                } else null
                            )
                            if (dueDateCal != null) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                DetailRow(
                                    icon    = Icons.Default.Schedule,
                                    label   = stringResource(R.string.label_time),
                                    value   = if (dueHasTime) formatDueTimeOnly(dueDateCal!!) else stringResource(R.string.reminder_none),
                                    onClick = { showTimePicker = true },
                                    onClear = if (dueHasTime) {
                                        { dueHasTime = false }
                                    } else null
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            DetailRow(
                                icon    = Icons.Default.Person,
                                label   = stringResource(R.string.dialog_assign_to),
                                value   = assigneeLabel(assignedTo, currentDeviceId, selectedList, context),
                                onClick = { if (members.isNotEmpty()) showAssignMenu = true }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            DetailRow(
                                icon    = Icons.Default.Notifications,
                                label   = stringResource(R.string.label_reminder),
                                value   = ReminderOptions.firstOrNull { it.second == reminderMinutes }?.first ?: stringResource(R.string.reminder_none),
                                onClick = { showReminderMenu = true }
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }

            // ── Liste wählen ─────────────────────────────────────────────
            if (showListMenu) {
                ChoiceDialog(
                    title    = stringResource(R.string.dialog_choose_list),
                    onDismiss = { showListMenu = false }
                ) {
                    lists.forEach { l ->
                        ChoiceRow(
                            label   = l.name,
                            dotColor = listColor(l.color),
                            onClick = { selectedListId = l.id; showListMenu = false }
                        )
                    }
                }
            }

            // ── Zuweisen wählen ──────────────────────────────────────────
            if (showAssignMenu) {
                ChoiceDialog(
                    title    = stringResource(R.string.dialog_assign_to),
                    onDismiss = { showAssignMenu = false }
                ) {
                    ChoiceRow(
                        label   = stringResource(R.string.label_none),
                        onClick = { assignedTo = null; showAssignMenu = false }
                    )
                    members.forEach { memberId ->
                        ChoiceRow(
                            label   = if (memberId == currentDeviceId) stringResource(R.string.label_me) else selectedList?.displayNameFor(memberId) ?: memberId.take(4).uppercase(),
                            onClick = { assignedTo = memberId; showAssignMenu = false }
                        )
                    }
                }
            }

            // ── Erinnerung wählen ────────────────────────────────────────
            if (showReminderMenu) {
                ChoiceDialog(
                    title    = stringResource(R.string.label_reminder),
                    onDismiss = { showReminderMenu = false }
                ) {
                    ReminderOptions.forEach { (label, minutes) ->
                        ChoiceRow(
                            label   = label,
                            onClick = { reminderMinutes = minutes; showReminderMenu = false }
                        )
                    }
                }
            }

            // ── Datum wählen ─────────────────────────────────────────────
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
                                if (existing != null && dueHasTime) {
                                    cal.set(Calendar.HOUR_OF_DAY, existing.get(Calendar.HOUR_OF_DAY))
                                    cal.set(Calendar.MINUTE, existing.get(Calendar.MINUTE))
                                } else {
                                    cal.set(Calendar.HOUR_OF_DAY, 9)
                                    cal.set(Calendar.MINUTE, 0)
                                }
                                dueDateCal = cal
                            }
                            showDatePicker = false
                        }) { Text(stringResource(R.string.action_apply)) }
                    },
                    dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) } }
                ) {
                    DatePicker(state = state)
                }
            }

            // ── Uhrzeit wählen (optional) ──────────────────────────────────
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
                            dueHasTime = true
                            showTimePicker = false
                        }) { Text(stringResource(R.string.action_apply)) }
                    },
                    dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.action_cancel)) } },
                    text = { TimePicker(state = timeState) }
                )
            }
        }
    }
}

@Composable
private fun PriorityChip(priority: Priority, selected: Boolean, onClick: () -> Unit) {
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
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
            Text(
                priority.displayLabel(),
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium,
                color      = if (selected) Color(0xFF201A17) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailRow(
    icon    : androidx.compose.ui.graphics.vector.ImageVector,
    label   : String,
    value   : String,
    valueDot: Color? = null,
    onClick : () -> Unit,
    onClear : (() -> Unit)? = null,
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
        if (valueDot != null) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(valueDot))
            Spacer(Modifier.width(2.dp))
        }
        Text(value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onClear != null) {
            IconButton(onClick = onClear, modifier = Modifier.size(22.dp)) {
                Icon(
                    androidx.compose.material.icons.Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_remove),
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Icon(
                androidx.compose.material.icons.Icons.Default.KeyboardArrowRight,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChoiceDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text(title) },
        text             = { Column(content = content) },
        confirmButton    = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) } }
    )
}

@Composable
private fun ChoiceRow(label: String, dotColor: Color? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (dotColor != null) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(dotColor))
        }
        Text(label, fontSize = 15.sp)
    }
}

private fun assigneeLabel(assignedTo: String?, currentDeviceId: String, list: TodoList?, context: android.content.Context): String = when {
    assignedTo == null              -> context.getString(R.string.label_none)
    assignedTo == currentDeviceId   -> context.getString(R.string.label_me)
    else                             -> list?.displayNameFor(assignedTo) ?: assignedTo.take(4).uppercase()
}

private fun formatDueDateOnly(cal: Calendar): String {
    val fmt = SimpleDateFormat("EEE, d. MMM", Locale.GERMAN)
    return fmt.format(cal.time)
}

private fun formatDueTimeOnly(cal: Calendar): String {
    val fmt = SimpleDateFormat("HH:mm", Locale.GERMAN)
    return fmt.format(cal.time)
}