package de.beigel.list.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.beigel.list.data.TodoItem
import de.beigel.list.data.TodoList
import de.beigel.list.repository.TodoRepository
import de.beigel.list.ui.theme.priorityColor
import de.beigel.list.data.Priority
import de.beigel.list.utils.HapticFeedback
import de.beigel.list.viewmodel.TodosViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class NotificationEntry(
    val icon    : ImageVector,
    val iconTint: Color,
    val text    : String,
    val meta    : String,
)

@Composable
fun BenachrichtigungenScreen(
    lists      : List<TodoList>,
    repository : TodoRepository,
    deviceId   : String,
    haptic     : HapticFeedback,
    onBack     : () -> Unit,
) {
    val vms: List<Pair<TodoList, TodosViewModel>> = lists.map { list ->
        list to viewModel(key = "notif_${list.id}", factory = TodosViewModel.Factory(repository, list.id))
    }
    val allTodos: List<Pair<TodoList, TodoItem>> = vms.flatMap { (list, vm) ->
        vm.uiState.collectAsStateWithLifecycle().value.todos.map { list to it }
    }

    // Dir zugewiesene, offene Aufgaben
    val assignedToMe = remember(allTodos, deviceId) {
        allTodos.filter { (_, todo) -> !todo.isDone && todo.assignedTo == deviceId }
    }
    // Aufgaben, die heute oder überfällig sind (unabhängig von Zuweisung)
    val dueSoon = remember(allTodos) {
        allTodos.filter { (_, todo) ->
            !todo.isDone && todo.dueDate != null && isTodayOrOverdue(todo.dueDate!!)
        }
    }

    val assignedEntries = assignedToMe.map { (list, todo) ->
        NotificationEntry(
            icon     = Icons.Default.PersonAdd,
            iconTint = priorityColor(Priority.fromString(todo.priority)),
            text     = "Dir zugewiesen: „${todo.title}“",
            meta     = list.name
        )
    }
    val dueEntries = dueSoon.map { (list, todo) ->
        NotificationEntry(
            icon     = Icons.Default.Schedule,
            iconTint = MaterialTheme.colorScheme.error,
            text     = "Fällig: „${todo.title}“",
            meta     = "${formatDue(todo.dueDate!!)} · ${list.name}"
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // App-Bar
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { haptic.tick(); onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("Benachrichtigungen", fontSize = 20.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        }

        if (assignedEntries.isEmpty() && dueEntries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.NotificationsNone, null,
                        modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text("Keine Benachrichtigungen", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                if (dueEntries.isNotEmpty()) {
                    item { SectionLabel("Fällig") }
                    items(dueEntries) { NotificationRow(it) }
                }
                if (assignedEntries.isNotEmpty()) {
                    item { SectionLabel("Dir zugewiesen", modifier = Modifier.padding(top = 8.dp)) }
                    items(assignedEntries) { NotificationRow(it) }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(entry: NotificationEntry) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier         = Modifier.size(40.dp).clip(CircleShape)
                .background(entry.iconTint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(entry.icon, null, tint = entry.iconTint, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(entry.text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(entry.meta, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun isTodayOrOverdue(ts: com.google.firebase.Timestamp): Boolean {
    val due   = Calendar.getInstance().apply { time = ts.toDate() }
    val today = Calendar.getInstance()
    return due.get(Calendar.YEAR) < today.get(Calendar.YEAR) ||
            (due.get(Calendar.YEAR) == today.get(Calendar.YEAR) && due.get(Calendar.DAY_OF_YEAR) <= today.get(Calendar.DAY_OF_YEAR))
}

private fun formatDue(ts: com.google.firebase.Timestamp): String {
    val due   = Calendar.getInstance().apply { time = ts.toDate() }
    val today = Calendar.getInstance()
    return if (due.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
        due.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
        "Heute"
    } else {
        SimpleDateFormat("d. MMM", Locale.GERMAN).format(due.time)
    }
}