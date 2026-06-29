package de.beigel.todo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.PaddingValues
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.beigel.todo.data.TodoItem
import de.beigel.todo.data.TodoList
import de.beigel.todo.repository.TodoRepository
import de.beigel.todo.utils.HapticFeedback
import de.beigel.todo.viewmodel.TodosViewModel
import java.text.DateFormatSymbols
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

    // Open todos for selected day (using all open todos since no date on model)
    val openTodos = allTodos.filter { !it.second.isDone }

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
                                // Dot wenn Aufgaben (immer für heutigen Tag)
                                Box(
                                    modifier = Modifier.size(5.dp).clip(CircleShape).background(
                                        if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                        else Color.Transparent
                                    )
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
        // Aufgaben für den Tag
        if (openTodos.isEmpty()) {
            item {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Keine offenen Aufgaben",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        } else {
            items(openTodos, key = { "${it.first.id}_${it.second.id}" }) { (list, todo) ->
                val vm = allVms.find { it.first.id == list.id }?.second
                AufgabenTaskRow(
                    todo      = todo,
                    listColor = listColor(list.color),
                    onToggle  = { vm?.toggleTodo(todo); haptic.tick() }
                )
            }
        }
    }
}
