package de.beigel.list.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import java.util.Calendar
import java.util.Locale

@Composable
fun KalenderScreen(
    lists      : List<TodoList>,
    selectedIds: Set<String>,
    repository : TodoRepository,
    haptic     : HapticFeedback,
    padding    : PaddingValues,
) {
    val context = LocalContext.current
    val actorName = remember { de.beigel.list.data.DeviceIdManager.getDeviceName(context) }
    val activeLists = remember(lists, selectedIds) { lists.filter { it.id in selectedIds } }

    val allVms: List<Pair<TodoList, TodosViewModel>> = activeLists.map { list ->
        list to viewModel(key = "kal_${list.id}", factory = TodosViewModel.Factory(repository, list.id))
    }
    val allTodos: List<Pair<TodoList, TodoItem>> = allVms.flatMap { (list, vm) ->
        vm.uiState.collectAsStateWithLifecycle().value.todos.map { list to it }
    }

    val today = remember { Calendar.getInstance() }
    var year  by remember { mutableIntStateOf(today.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(today.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableIntStateOf(today.get(Calendar.DAY_OF_MONTH)) }

    val cal = remember(year, month) {
        Calendar.getInstance().apply { set(year, month, 1) }
    }
    val daysInMonth  = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstWeekday = ((cal.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7) // 0=Mon

    val monthName = remember(year, month) {
        val c = Calendar.getInstance().apply { set(year, month, 1) }
        val fmt = java.text.SimpleDateFormat("MMMM yyyy", Locale.GERMAN)
        fmt.format(c.time).replaceFirstChar { it.uppercase() }
    }

    val weekdays = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")

    // Offene Aufgaben mit Fälligkeitsdatum, gruppiert nach Tag-im-Monat (nur aktueller Monat/Jahr)
    val todosByDay: Map<Int, List<Pair<TodoList, TodoItem>>> = remember(allTodos, year, month) {
        allTodos
            .filter { (_, todo) -> !todo.isDone && todo.dueDate != null }
            .filter { (_, todo) ->
                val c = Calendar.getInstance().apply { time = todo.dueDate!!.toDate() }
                c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month
            }
            .groupBy { (_, todo) ->
                Calendar.getInstance().apply { time = todo.dueDate!!.toDate() }.get(Calendar.DAY_OF_MONTH)
            }
    }
    val selectedDayTodos = todosByDay[selectedDay] ?: emptyList()

    val selectedDayLabel = remember(year, month, selectedDay) {
        val c = Calendar.getInstance().apply { set(year, month, selectedDay) }
        val fmt = java.text.SimpleDateFormat("EEEE, d. MMMM", Locale.GERMAN)
        fmt.format(c.time).replaceFirstChar { it.uppercase() }
    }

    LazyColumn(
        modifier       = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Titel
        item {
            Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp)) {
                Text("Kalender", fontSize = 34.sp, fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.5).sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        // Monats-Header
        item {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(monthName, fontSize = 18.sp, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = {
                        haptic.tick()
                        if (month == 0) { month = 11; year-- } else month--
                    }) {
                        Icon(Icons.Default.ChevronLeft, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = {
                        haptic.tick()
                        if (month == 11) { month = 0; year++ } else month++
                    }) {
                        Icon(Icons.Default.ChevronRight, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        // Wochentags-Header
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp)) {
                weekdays.forEach { wd ->
                    Text(wd, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        // Kalender-Raster
        val cells = buildList {
            repeat(firstWeekday) { add(null) }
            (1..daysInMonth).forEach { add(it) }
            while (size % 7 != 0) add(null)
        }
        val rows = cells.chunked(7)
        items(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                row.forEach { day ->
                    Box(
                        modifier         = Modifier.weight(1f).aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != null) {
                            val isToday    = day == today.get(Calendar.DAY_OF_MONTH) &&
                                    month == today.get(Calendar.MONTH) &&
                                    year == today.get(Calendar.YEAR)
                            val isSelected = day == selectedDay
                            val dayTasks   = todosByDay[day]
                            val dotColor   = dayTasks?.firstOrNull()?.let { (_, todo) ->
                                priorityColor(Priority.fromString(todo.priority))
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier            = Modifier.clickable { selectedDay = day; haptic.tick() }
                            ) {
                                Box(
                                    modifier         = Modifier.size(34.dp).clip(CircleShape)
                                        .background(when {
                                            isSelected && isToday -> MaterialTheme.colorScheme.primary
                                            isSelected            -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                            else                  -> Color.Transparent
                                        }),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text       = "$day",
                                        fontSize   = 14.sp,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        color      = when {
                                            isSelected && isToday -> MaterialTheme.colorScheme.onPrimary
                                            isSelected            -> MaterialTheme.colorScheme.primary
                                            else                  -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                                // Punkt nur, wenn an dem Tag tatsächlich Aufgaben fällig sind
                                Box(
                                    modifier = Modifier.size(5.dp).clip(CircleShape)
                                        .background(dotColor ?: Color.Transparent)
                                )
                            }
                        }
                    }
                }
            }
        }
        // Tages-Label
        item {
            SectionLabel(selectedDayLabel, modifier = Modifier.padding(top = 8.dp))
        }
        // Aufgaben für den ausgewählten Tag
        if (selectedDayTodos.isEmpty()) {
            item {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Keine Aufgaben an diesem Tag",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        } else {
            items(selectedDayTodos, key = { "${it.first.id}_${it.second.id}" }) { (list, todo) ->
                val vm = allVms.find { it.first.id == list.id }?.second
                AufgabenTaskRow(
                    todo      = todo,
                    listName  = list.name,
                    onToggle  = { vm?.toggleTodo(todo, actorName); haptic.tick() }
                )
            }
        }
    }
}