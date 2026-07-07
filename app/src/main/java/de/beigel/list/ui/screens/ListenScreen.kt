package de.beigel.list.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.beigel.list.data.ListCounts
import de.beigel.list.data.TodoItem
import de.beigel.list.data.TodoList
import de.beigel.list.repository.TodoRepository
import de.beigel.list.utils.HapticFeedback
import de.beigel.list.viewmodel.ListsViewModel
import de.beigel.list.viewmodel.TodosViewModel

private val listIconSet: List<ImageVector> = listOf(
    Icons.Default.Work,
    Icons.Default.Home,
    Icons.Default.ShoppingCart,
    Icons.Default.Favorite,
    Icons.Default.School,
    Icons.Default.Star,
    Icons.Default.Flight,
    Icons.Default.DateRange,
)

fun listIconFor(index: Int): ImageVector = listIconSet[index % listIconSet.size]

@Composable
fun ListenScreen(
    viewModel  : ListsViewModel,
    repository : TodoRepository,
    deviceId   : String,
    haptic     : HapticFeedback,
    padding    : PaddingValues,
    onOpenList : (TodoList) -> Unit,
    onShare    : (TodoList) -> Unit,
    onErstellen: () -> Unit,
    onSearch   : () -> Unit,
    onOpenTask : (TodoList, TodoItem) -> Unit = { _, _ -> },
) {
    val uiState           by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Pro Liste ein TodosViewModel, nur um die heute fälligen Aufgaben zu ermitteln
    val todosViewModels: List<Pair<TodoList, TodosViewModel>> = uiState.lists.map { list ->
        list to viewModel(
            key     = "listen_today_${list.id}",
            factory = TodosViewModel.Factory(repository, list.id)
        )
    }
    val todayPairs: List<Pair<TodoList, TodoItem>> = todosViewModels
        .flatMap { (list, vm) ->
            vm.uiState.collectAsStateWithLifecycle().value.todos
                .filter { !it.isDone && it.dueDate != null && isToday(it.dueDate!!) }
                .map { list to it }
        }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }
    LaunchedEffect(uiState.joinSuccess) {
        uiState.joinSuccess?.let {
            snackbarHostState.showSnackbar("\"${it.name}\" beigetreten!")
            viewModel.clearJoinSuccess()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPad)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyVerticalGrid(
                    columns             = GridCells.Fixed(2),
                    contentPadding      = PaddingValues(start = 22.dp, end = 22.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier            = Modifier.fillMaxSize()
                ) {
                    // Header (full width)
                    item(span = { GridItemSpan(2) }) {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 16.dp)) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Text("Meine Listen", fontSize = 34.sp, fontWeight = FontWeight.Medium,
                                    letterSpacing = (-0.5).sp, color = MaterialTheme.colorScheme.onSurface)
                                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.clickable { haptic.tick(); onSearch() })
                            }
                            val shared = uiState.lists.count { it.memberIds.size > 1 }
                            Text(
                                "${uiState.lists.size} Listen · $shared geteilt",
                                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                    }

                    // List cards
                    items(uiState.lists.toList(), key = { it.id }) { list ->
                        val idx      = uiState.lists.indexOf(list)
                        val isShared = list.memberIds.size > 1
                        ListCard(
                            list     = list,
                            index    = idx,
                            isShared = isShared,
                            isSelected = list.id in uiState.selectedListIds,
                            counts   = uiState.listCounts[list.id] ?: ListCounts(),
                            onClick  = { haptic.click(); onOpenList(list) },
                            onShare  = { e -> haptic.tick(); onShare(list) },
                            onToggleSelect = { haptic.tick(); viewModel.toggleListSelection(list.id) }
                        )
                    }

                    // New list card
                    item {
                        NewListCard(onClick = { haptic.click(); onErstellen() })
                    }

                    // Heute fällig
                    if (todayPairs.isNotEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            SectionLabel("Heute fällig", modifier = Modifier.padding(top = 8.dp))
                        }
                        items(todayPairs, span = { GridItemSpan(2) }, key = { "today_${it.first.id}_${it.second.id}" }) { (list, todo) ->
                            val vm = todosViewModels.find { it.first.id == list.id }?.second
                            AufgabenTaskRow(
                                todo      = todo,
                                listName  = list.name,
                                onToggle  = { vm?.toggleTodo(todo); haptic.tick() },
                                onClick   = { haptic.tick(); onOpenTask(list, todo) }
                            )
                        }
                    }

                    // Beitreten per Code
                    item(span = { GridItemSpan(2) }) {
                        TextButton(
                            onClick  = { haptic.tick(); viewModel.showJoinDialog() },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Link, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Mit Code beitreten")
                        }
                    }
                }
            }

            // Extended FAB
            ExtendedFloatingActionButton(
                onClick        = { haptic.click(); onErstellen() },
                modifier       = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 18.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
                shape          = RoundedCornerShape(18.dp),
                icon           = { Icon(Icons.Default.Add, null) },
                text           = { Text("Liste", fontWeight = FontWeight.Medium, fontSize = 15.sp) }
            )
        }
    }

    if (uiState.showJoinDialog) {
        JoinListDialog(
            onDismiss = { haptic.tick(); viewModel.hideJoinDialog() },
            onJoin    = { viewModel.previewInvite(it) }
        )
    }

    uiState.invitePreview?.let { previewList ->
        Dialog(
            onDismissRequest = { viewModel.clearInvitePreview() },
            properties       = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                EinladungScreen(
                    list      = previewList,
                    haptic    = haptic,
                    onAccept  = { viewModel.confirmJoin(previewList.id) },
                    onDecline = { viewModel.clearInvitePreview() }
                )
            }
        }
    }
}

private fun isToday(ts: com.google.firebase.Timestamp): Boolean {
    val due   = java.util.Calendar.getInstance().apply { time = ts.toDate() }
    val today = java.util.Calendar.getInstance()
    return due.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) &&
            due.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR)
}

private val avatarColors: List<Color> = listOf(
    Color(0xFF5B8DEF), Color(0xFF4EC9A8), Color(0xFFFFA552), Color(0xFFE06FA0), Color(0xFF9B7EDE),
)

private fun avatarColorFor(memberId: String): Color =
    avatarColors[(memberId.hashCode() and 0x7fffffff) % avatarColors.size]

private fun avatarLetterFor(memberId: String): String =
    memberId.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?"

@Composable
private fun ListCard(
    list        : TodoList,
    index       : Int,
    isShared    : Boolean,
    isSelected  : Boolean,
    counts      : ListCounts,
    onClick     : () -> Unit,
    onShare     : (Any) -> Unit,
    onToggleSelect: () -> Unit,
) {
    val color = listColor(list.color)
    val bgAlpha = color.copy(alpha = 0.16f)

    Card(
        onClick = onClick,
        shape   = RoundedCornerShape(22.dp),
        colors  = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth().height(210.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                // Icon tile
                Box(
                    modifier         = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(bgAlpha),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(listIconFor(index), null, tint = color, modifier = Modifier.size(24.dp))
                }
                // Share indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick  = onToggleSelect,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (isSelected) "In Aufgaben-Übersicht anzeigen (aktiv)" else "In Aufgaben-Übersicht anzeigen (inaktiv)",
                            tint     = if (isSelected) color else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick  = { onShare(Unit) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (isShared) Icons.Default.Group else Icons.Default.Person,
                            null,
                            tint     = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(list.name, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${counts.done} von ${counts.total} erledigt",
                fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp))
            // Fortschrittsbalken (echte Erledigt-Quote)
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Box(modifier = Modifier.fillMaxWidth(counts.fraction).fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp)).background(color))
            }
            Spacer(Modifier.height(14.dp))
            if (isShared) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row {
                        list.memberIds.take(3).forEachIndexed { i, memberId ->
                            Box(
                                modifier = Modifier
                                    .offset(x = if (i == 0) 0.dp else (-6).dp * i)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(avatarColorFor(memberId))
                                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(avatarLetterFor(memberId), fontSize = 8.sp, fontWeight = FontWeight.Bold,
                                    color = Color.White)
                            }
                        }
                    }
                    Spacer(Modifier.width(7.dp))
                    Text("${list.memberIds.size} Personen",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null,
                        tint     = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Nur du", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun NewListCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape   = RoundedCornerShape(22.dp),
        border  = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
        color   = Color.Transparent,
        modifier = Modifier.fillMaxWidth().height(210.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier         = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text("Neue Liste", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun JoinListDialog(onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Liste beitreten") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Gib den Einladungscode ein:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = code, onValueChange = { code = it.trim() },
                    label = { Text("Einladungscode") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onJoin(code); onDismiss() }, enabled = code.isNotBlank()) {
                Text("Beitreten")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}