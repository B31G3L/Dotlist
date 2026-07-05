package de.beigel.list.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.beigel.list.data.TodoItem
import de.beigel.list.data.TodoList
import de.beigel.list.repository.TodoRepository
import de.beigel.list.utils.HapticFeedback
import de.beigel.list.viewmodel.TodosViewModel

private enum class AufgabenFilter { ALLE, OFFEN, ERLEDIGT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AufgabenScreen(
    lists      : List<TodoList>,
    selectedIds: Set<String>,
    isLoading  : Boolean,
    repository : TodoRepository,
    haptic     : HapticFeedback,
    padding    : PaddingValues,
    onGoListen : () -> Unit,
) {
    val activeLists = remember(lists, selectedIds) {
        lists.filter { it.id in selectedIds }
    }

    val viewModels: List<Pair<TodoList, TodosViewModel>> = activeLists.map { list ->
        list to viewModel(
            key     = "aufg_${list.id}",
            factory = TodosViewModel.Factory(repository, list.id)
        )
    }

    val allPairs: List<Pair<TodoList, TodoItem>> = viewModels.map { (list, vm) ->
        list to vm.uiState.collectAsStateWithLifecycle().value
    }.flatMap { (list, state) ->
        state.todos.map { list to it }
    }

    val firstVm = viewModels.firstOrNull()?.second

    var filter        by remember { mutableStateOf(AufgabenFilter.ALLE) }
    var showAddDialog by remember { mutableStateOf(false) }

    val openPairs = remember(allPairs) { allPairs.filter { !it.second.isDone } }
    val donePairs = remember(allPairs) { allPairs.filter { it.second.isDone } }

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
            // Top action row
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Menu, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Icon(Icons.Default.Search, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(Icons.Default.MoreVert, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            // Titel + Untertitel
            item {
                Column(modifier = Modifier.padding(horizontal = 22.dp)) {
                    Text(
                        text       = "Aufgaben",
                        fontSize   = 34.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.5).sp,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text      = "${openPairs.size} offen · ${donePairs.size} erledigt",
                        fontSize  = 13.sp,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier  = Modifier.padding(top = 8.dp)
                    )
                }
            }
            // Filter-Chips
            item {
                Row(
                    modifier            = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                        listColor = listColor(list.color),
                        onToggle  = { vm?.toggleTodo(todo); haptic.tick() }
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
                        listColor = listColor(list.color),
                        onToggle  = { vm?.toggleTodo(todo); haptic.tick() }
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
    }

    if (showAddDialog && firstVm != null) {
        AddAufgabeDialog(
            onDismiss = { showAddDialog = false },
            onAdd     = { title -> firstVm.addTodo(title); showAddDialog = false }
        )
    }
}

@Composable
fun AufgabenTaskRow(
    todo     : TodoItem,
    listColor: Color,
    onToggle : () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
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
                color          = if (todo.isDone) MaterialTheme.colorScheme.onSurfaceVariant
                                 else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (todo.isDone) TextDecoration.LineThrough else TextDecoration.None,
                lineHeight     = 20.sp
            )
        }
        // Prioritätspunkt (Listfarbe)
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (todo.isDone) Color.Transparent else listColor)
        )
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
private fun AddAufgabeDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Neue Aufgabe") },
        text             = {
            OutlinedTextField(
                value         = text,
                onValueChange = { text = it },
                label         = { Text("Aufgabentitel") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { if (text.isNotBlank()) onAdd(text) }, enabled = text.isNotBlank()) {
                Text("Hinzufügen")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
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
            Text("Aktiviere zuerst eine Liste.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
