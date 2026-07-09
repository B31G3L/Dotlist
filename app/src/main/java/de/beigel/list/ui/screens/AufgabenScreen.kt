package de.beigel.list.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.beigel.list.data.Priority
import de.beigel.list.data.TodoItem
import de.beigel.list.data.TodoList
import de.beigel.list.repository.TodoRepository
import de.beigel.list.ui.theme.priorityColor
import de.beigel.list.utils.HapticFeedback
import de.beigel.list.viewmodel.TodosViewModel

private enum class AufgabenFilter { ALLE, OFFEN, ERLEDIGT }
private enum class SortOption(val label: String) {
    STANDARD("Standard"),
    FAELLIGKEIT("Fälligkeit"),
    PRIORITAET("Priorität"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AufgabenScreen(
    lists      : List<TodoList>,
    selectedIds: Set<String>,
    isLoading  : Boolean,
    repository : TodoRepository,
    haptic     : HapticFeedback,
    padding    : PaddingValues,
    deviceId   : String,
    onGoListen : () -> Unit,
    onSearch   : () -> Unit,
    onNotifications: () -> Unit,
    onOpenTask : (TodoList, TodoItem) -> Unit = { _, _ -> },
    unreadNotifications: Int = 0,
) {
    val context = LocalContext.current
    val actorName = remember { de.beigel.list.data.DeviceIdManager.getDeviceName(context) }

    val activeLists = remember(lists, selectedIds) {
        lists.filter { it.id in selectedIds }
    }

    val viewModels: List<Pair<TodoList, TodosViewModel>> = activeLists.map { list ->
        list to viewModel(
            key     = "detail_${list.id}",
            factory = TodosViewModel.Factory(repository, list.id)
        )
    }

    val allPairs: List<Pair<TodoList, TodoItem>> = viewModels.map { (list, vm) ->
        list to vm.uiState.collectAsStateWithLifecycle().value
    }.flatMap { (list, state) ->
        state.todos.map { list to it }
    }

    var filter        by remember { mutableStateOf(AufgabenFilter.ALLE) }
    var sortOption     by remember { mutableStateOf(SortOption.STANDARD) }
    var onlyMine       by remember { mutableStateOf(false) }
    var onlyOverdue    by remember { mutableStateOf(false) }
    var showSortMenu   by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val errorEntry = viewModels.firstNotNullOfOrNull { (_, vm) ->
        vm.uiState.collectAsStateWithLifecycle().value.error?.let { vm to it }
    }
    LaunchedEffect(errorEntry) {
        errorEntry?.let { (vm, message) -> snackbarHostState.showSnackbar(message); vm.clearError() }
    }

    val deletedEntry = viewModels.firstNotNullOfOrNull { (_, vm) ->
        vm.uiState.collectAsStateWithLifecycle().value.recentlyDeleted?.let { vm to it }
    }
    LaunchedEffect(deletedEntry) {
        val (vm, deleted) = deletedEntry ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message     = "„${deleted.title}“ gelöscht",
            actionLabel = "Rückgängig",
            duration    = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) vm.undoDelete() else vm.dismissRecentlyDeleted()
    }

    val now = remember { java.util.Date() }
    val filteredPairs = remember(allPairs, onlyMine, onlyOverdue, deviceId) {
        allPairs
            .let { p -> if (onlyMine) p.filter { it.second.assignedTo == deviceId } else p }
            .let { p ->
                if (onlyOverdue) p.filter { !it.second.isDone && it.second.dueDate != null && it.second.dueDate!!.toDate().before(now) }
                else p
            }
    }

    fun sorted(pairs: List<Pair<TodoList, TodoItem>>): List<Pair<TodoList, TodoItem>> = when (sortOption) {
        SortOption.STANDARD    -> pairs
        SortOption.FAELLIGKEIT -> pairs.sortedWith(compareBy(nullsLast()) { it.second.dueDate?.toDate() })
        SortOption.PRIORITAET  -> pairs.sortedBy { Priority.fromString(it.second.priority).ordinal }
    }

    val openPairs = remember(filteredPairs, sortOption) { sorted(filteredPairs.filter { !it.second.isDone }) }
    val donePairs = remember(filteredPairs, sortOption) { sorted(filteredPairs.filter { it.second.isDone }) }

    val shownOpen = when (filter) {
        AufgabenFilter.ALLE, AufgabenFilter.OFFEN -> openPairs
        AufgabenFilter.ERLEDIGT                   -> emptyList()
    }
    val shownDone = when (filter) {
        AufgabenFilter.ALLE, AufgabenFilter.ERLEDIGT -> donePairs
        AufgabenFilter.OFFEN                          -> emptyList()
    }

    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            return@Box
        }

        if (activeLists.isEmpty()) {
            EmptyAufgabenHint(onGoListen = onGoListen)
            return@Box
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            // Titel-Zeile inkl. Suche & Benachrichtigungen
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text          = "Aufgaben",
                        fontSize      = 32.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        color         = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = "Suche",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { haptic.tick(); onSearch() })
                        Box(modifier = Modifier.clickable { haptic.tick(); onNotifications() }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Benachrichtigungen",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (unreadNotifications > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error)
                                )
                            }
                        }
                    }
                }
            }
            // Untertitel
            item {
                Text(
                    text      = "${openPairs.size} offen · ${donePairs.size} erledigt",
                    fontSize  = 13.sp,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier  = Modifier.padding(horizontal = 22.dp)
                )
            }
            // Filter-Chips + Sortierung
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AufgabenFilter.entries.forEach { f ->
                            val active = filter == f
                            val label  = when (f) {
                                AufgabenFilter.ALLE     -> "Alle"
                                AufgabenFilter.OFFEN    -> "Offen"
                                AufgabenFilter.ERLEDIGT -> "Erledigt"
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .clickable { filter = f }
                                    .padding(horizontal = 16.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text       = label,
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color      = if (active) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Box {
                        val sortActive = sortOption != SortOption.STANDARD || onlyMine || onlyOverdue
                        IconButton(onClick = { showSortMenu = true }, modifier = Modifier.size(30.dp)) {
                            Icon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sortieren & Filtern",
                                tint = if (sortActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            Text("Sortieren nach", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    leadingIcon = {
                                        if (sortOption == option) {
                                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                        } else {
                                            Spacer(Modifier.size(24.dp))
                                        }
                                    },
                                    onClick = { sortOption = option; showSortMenu = false }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Nur meine Aufgaben") },
                                leadingIcon = {
                                    if (onlyMine) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                    else Spacer(Modifier.size(24.dp))
                                },
                                onClick = { onlyMine = !onlyMine }
                            )
                            DropdownMenuItem(
                                text = { Text("Nur überfällige") },
                                leadingIcon = {
                                    if (onlyOverdue) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                    else Spacer(Modifier.size(24.dp))
                                },
                                onClick = { onlyOverdue = !onlyOverdue }
                            )
                        }
                    }
                }
            }
            // Anstehend-Section
            if (shownOpen.isNotEmpty()) {
                item {
                    SectionLabel("Anstehend")
                }
                items(shownOpen, key = { "${it.first.id}_${it.second.id}" }) { (list, todo) ->
                    val vm = viewModels.find { it.first.id == list.id }?.second
                    AufgabenTaskRow(
                        todo      = todo,
                        listName  = list.name,
                        onToggle  = { vm?.toggleTodo(todo, actorName); haptic.tick() },
                        onClick   = { haptic.tick(); onOpenTask(list, todo) }
                    )
                }
            }
            // Erledigt-Section
            if (shownDone.isNotEmpty()) {
                item {
                    SectionLabel("Erledigt", modifier = Modifier.padding(top = 8.dp))
                }
                items(shownDone, key = { "done_${it.first.id}_${it.second.id}" }) { (list, todo) ->
                    val vm = viewModels.find { it.first.id == list.id }?.second
                    AufgabenTaskRow(
                        todo      = todo,
                        listName  = list.name,
                        onToggle  = { vm?.toggleTodo(todo, actorName); haptic.tick() },
                        onClick   = { haptic.tick(); onOpenTask(list, todo) }
                    )
                }
            }
            // Empty state per filter
            if (shownOpen.isEmpty() && shownDone.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Keine Aufgaben",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // Extended FAB
        ExtendedFloatingActionButton(
            onClick          = { haptic.click(); showAddDialog = true },
            modifier         = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 18.dp),
            containerColor   = MaterialTheme.colorScheme.primary,
            contentColor     = MaterialTheme.colorScheme.onPrimary,
            shape            = RoundedCornerShape(18.dp),
            icon             = { Icon(Icons.Default.Add, contentDescription = null) },
            text             = { Text("Aufgabe", fontWeight = FontWeight.Medium, fontSize = 15.sp) }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showAddDialog && activeLists.isNotEmpty()) {
        NeueAufgabeScreen(
            lists           = activeLists,
            initialListId   = activeLists.first().id,
            currentDeviceId = deviceId,
            onDismiss       = { showAddDialog = false },
            onSave          = { listId, title, description, priority, dueDate, assignedTo, reminderMinutes ->
                viewModels.find { it.first.id == listId }?.second?.addTodo(
                    title, description, priority, dueDate, assignedTo, reminderMinutes, actorName = actorName
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AufgabenTaskRow(
    todo     : TodoItem,
    listName : String,
    onToggle : () -> Unit,
    onClick  : () -> Unit = {},
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Checkbox
        Box(
            modifier         = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (todo.isDone) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { onToggle() }
                .then(
                    if (!todo.isDone) Modifier.clip(CircleShape) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (todo.isDone) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (!todo.isDone) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape    = CircleShape,
                            color    = Color.Transparent,
                            border   = androidx.compose.foundation.BorderStroke(
                                2.dp, MaterialTheme.colorScheme.outline
                            )
                        ) {}
                    }
                }
            }
        }
        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text           = todo.title,
                fontSize       = 15.sp,
                fontWeight     = FontWeight.Medium,
                color          = if (todo.isDone) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (todo.isDone) TextDecoration.LineThrough else TextDecoration.None,
                lineHeight     = 20.sp
            )
            val subtitle = buildString {
                todo.dueDate?.let { append(formatRelativeDue(it)); append(" · ") }
                append(listName)
            }
            Text(
                text     = subtitle,
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Prioritätspunkt
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (todo.isDone) Color.Transparent else priorityColor(Priority.fromString(todo.priority)))
        )
    }
}

/** Formatiert ein Fälligkeitsdatum locker relativ ("Heute", "Morgen", "Fr, 3. Juli"). */
private fun formatRelativeDue(ts: com.google.firebase.Timestamp): String {
    val due   = java.util.Calendar.getInstance().apply { time = ts.toDate() }
    val today = java.util.Calendar.getInstance()
    val diffDays = ((due.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
    return when (diffDays) {
        0    -> "Heute"
        1    -> "Morgen"
        else -> java.text.SimpleDateFormat("E, d. MMM", java.util.Locale.GERMAN).format(due.time)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text          = text.uppercase(),
        fontSize      = 12.sp,
        fontWeight    = FontWeight.Bold,
        letterSpacing = 0.9.sp,
        color         = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier      = modifier.padding(horizontal = 22.dp, vertical = 6.dp)
    )
}

@Composable
private fun EmptyAufgabenHint(onGoListen: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier            = Modifier.padding(32.dp)
        ) {
            Icon(Icons.Default.CheckCircle, null,
                modifier = Modifier.size(64.dp),
                tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            Text("Keine Aufgaben", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Erstelle zuerst eine Liste.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onGoListen, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.List, null)
                Spacer(Modifier.width(8.dp))
                Text("Zu den Listen")
            }
        }
    }
}

fun listColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color(0xFFD0BCFF)
}