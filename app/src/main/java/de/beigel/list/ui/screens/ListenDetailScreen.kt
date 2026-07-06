package de.beigel.list.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenDetailScreen(
    list      : TodoList,
    repository: TodoRepository,
    haptic    : HapticFeedback,
    onBack    : () -> Unit,
    onShare   : () -> Unit,
) {
    val vm by remember { mutableStateOf(null as TodosViewModel?) }
    val todoVm: TodosViewModel = viewModel(
        key     = "detail_${list.id}",
        factory = TodosViewModel.Factory(repository, list.id)
    )
    val uiState by todoVm.uiState.collectAsStateWithLifecycle()

    val openTodos = remember(uiState.todos) { uiState.todos.filter { !it.isDone } }
    val doneTodos = remember(uiState.todos) { uiState.todos.filter { it.isDone } }
    val total     = uiState.todos.size
    val doneCount = doneTodos.size
    val progress  = if (total > 0) doneCount.toFloat() / total else 0f

    val listColor = listColor(list.color)
    val isShared  = list.memberIds.size > 1
    var showAdd   by remember { mutableStateOf(false) }
    var newText   by remember { mutableStateOf("") }
    val focusReq  = remember { FocusRequester() }

    val listIdx = 0  // We don't have global index here, use 0 as default icon

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); todoVm.clearError() }
    }

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // App-Bar
            item {
                Row(
                    modifier          = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                            tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(list.name, fontSize = 20.sp, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    if (isShared) {
                        IconButton(onClick = onShare) {
                            Icon(Icons.Default.Group, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = { haptic.tick() }) {
                        Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            // Kopfbereich: Mitglieder + Fortschritt
            item {
                Column(modifier = Modifier.padding(horizontal = 22.dp)) {
                    if (isShared) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MemberAvatarStack(memberIds = list.memberIds, listColor = listColor)
                                Spacer(Modifier.width(10.dp))
                                Text("${list.memberIds.size} Mitglieder",
                                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text     = "Verwalten",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color    = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onShare() }
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    Text("$doneCount von $total erledigt",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress      = { progress },
                        modifier      = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color         = listColor,
                        trackColor    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }
            // Eingabe
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                    shape    = RoundedCornerShape(16.dp),
                    color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier          = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value         = newText,
                            onValueChange = { newText = it },
                            placeholder   = { Text("Neue Aufgabe hinzufügen…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors        = TextFieldDefaults.colors(
                                focusedContainerColor   = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor   = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier      = Modifier.weight(1f).focusRequester(focusReq),
                            singleLine    = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (newText.isNotBlank()) {
                                    todoVm.addTodo(newText); newText = ""; haptic.click()
                                }
                            })
                        )
                        if (newText.isNotBlank()) {
                            IconButton(onClick = {
                                todoVm.addTodo(newText); newText = ""; haptic.click()
                            }) {
                                Icon(Icons.Default.Add, null,
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            // Section Aufgaben
            item { SectionLabel("Aufgaben") }
            if (openTodos.isEmpty() && doneTodos.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Noch keine Aufgaben", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(openTodos, key = { it.id }) { todo ->
                DetailTaskRow(
                    todo      = todo,
                    listColor = listColor,
                    onToggle  = { todoVm.toggleTodo(todo); haptic.tick() },
                    onDelete  = { todoVm.deleteTodo(todo.id); haptic.heavy() }
                )
            }
            if (doneTodos.isNotEmpty()) {
                item { SectionLabel("Erledigt (${doneTodos.size})", modifier = Modifier.padding(top = 8.dp)) }
                items(doneTodos, key = { "done_${it.id}" }) { todo ->
                    DetailTaskRow(
                        todo      = todo,
                        listColor = listColor,
                        onToggle  = { todoVm.toggleTodo(todo); haptic.tick() },
                        onDelete  = { todoVm.deleteTodo(todo.id); haptic.heavy() }
                    )
                }
            }
        }

        // Extended FAB
        ExtendedFloatingActionButton(
            onClick        = { newText = ""; focusReq.requestFocus(); haptic.click() },
            modifier       = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 24.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor   = MaterialTheme.colorScheme.onPrimary,
            shape          = RoundedCornerShape(18.dp),
            icon           = { Icon(Icons.Default.Add, null) },
            text           = { Text("Aufgabe", fontWeight = FontWeight.Medium, fontSize = 15.sp) }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun MemberAvatarStack(memberIds: List<String>, listColor: Color) {
    val avatarColors = listOf(
        Color(0xFF5B8DEF), Color(0xFF2FB6A0), Color(0xFFE8A04E),
        Color(0xFFE06FA0), Color(0xFF4F378B)
    )
    Row {
        memberIds.take(3).forEachIndexed { i, id ->
            val initial = id.take(1).uppercase().ifEmpty { "?" }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .offset(x = (-6 * i).dp)
                    .clip(CircleShape)
                    .background(avatarColors[i % avatarColors.size]),
                contentAlignment = Alignment.Center
            ) {
                Text(initial, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun DetailTaskRow(
    todo     : TodoItem,
    listColor: Color,
    onToggle : () -> Unit,
    onDelete : () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 12.dp)
            .padding(horizontal = 10.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Checkbox
        Box(
            modifier         = Modifier.size(24.dp).clip(CircleShape)
                .background(if (todo.isDone) MaterialTheme.colorScheme.primary else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (todo.isDone) {
                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
            } else {
                Surface(
                    modifier = Modifier.size(24.dp), shape = CircleShape, color = Color.Transparent,
                    border   = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                ) {}
            }
        }
        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text           = todo.title,
                fontSize       = 15.sp,
                color          = if (todo.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (todo.isDone) TextDecoration.LineThrough else TextDecoration.None,
                lineHeight     = 20.sp
            )
        }
        // Prioritätspunkt
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (todo.isDone) Color.Transparent else listColor))
        // Mehr-Menü
        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.MoreVert, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Löschen", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { showMenu = false; onDelete() }
                )
            }
        }
    }
}