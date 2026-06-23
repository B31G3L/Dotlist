package de.beigel.todo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.beigel.todo.data.TodoItem
import de.beigel.todo.data.TodoList
import de.beigel.todo.repository.TodoRepository
import de.beigel.todo.utils.HapticFeedback
import de.beigel.todo.viewmodel.TodosViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiTodosScreen(
    activeLists: List<TodoList>,
    repository : TodoRepository,
    haptic     : HapticFeedback
) {
    if (activeLists.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Keine Liste aktiv", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val viewModels: List<Pair<TodoList, TodosViewModel>> = activeLists.map { list ->
        list to viewModel(
            key     = "multi_${list.id}",
            factory = TodosViewModel.Factory(repository, list.id)
        )
    }

    val allStates = viewModels.map { (list, vm) ->
        list to vm.uiState.collectAsStateWithLifecycle().value
    }

    val isLoading = allStates.any { it.second.isLoading }

    val openTodos: List<Pair<TodoList, TodoItem>> = allStates.flatMap { (list, state) ->
        state.todos.filter { !it.isDone }.map { list to it }
    }
    val doneTodos: List<Pair<TodoList, TodoItem>> = allStates.flatMap { (list, state) ->
        state.todos.filter { it.isDone }.map { list to it }
    }

    var inputText     by remember { mutableStateOf("") }
    var targetListId  by remember(activeLists) { mutableStateOf(activeLists.first().id) }
    var editingTodo   by remember { mutableStateOf<TodoItem?>(null) }
    var editingListId by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val scope          = rememberCoroutineScope()

    var sheetTodo  by remember { mutableStateOf<Pair<TodoList, TodoItem>?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val targetList  = activeLists.find { it.id == targetListId } ?: activeLists.first()
    val targetColor = remember(targetList.color) {
        try { Color(android.graphics.Color.parseColor(targetList.color)) }
        catch (e: Exception) { Color(0xFF5B3A8C) }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val errors = allStates.mapNotNull { it.second.error }
    LaunchedEffect(errors) {
        errors.firstOrNull()?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(editingTodo) {
        editingTodo?.let {
            inputText = it.title
            delay(100)
            focusRequester.requestFocus()
        }
    }

    fun submitInput() {
        val text = inputText.trim()
        if (text.isBlank()) return
        val editing = editingTodo
        if (editing != null && editingListId != null) {
            val vm = viewModels.find { it.first.id == editingListId }?.second
            vm?.editTodo(editing.id, text)
            editingTodo   = null
            editingListId = null
        } else {
            val vm = viewModels.find { it.first.id == targetListId }?.second
            vm?.addTodo(text)
        }
        haptic.success()
        inputText = ""
    }

    Scaffold(
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(modifier = Modifier.navigationBarsPadding()) {

                    // Ziel-Liste Chips
                    if (activeLists.size > 1 && editingTodo == null) {
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Hinzufügen zu:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            activeLists.forEach { list ->
                                val isTarget = list.id == targetListId
                                val c = remember(list.color) {
                                    try { Color(android.graphics.Color.parseColor(list.color)) }
                                    catch (e: Exception) { Color.Gray }
                                }
                                FilterChip(
                                    selected = isTarget,
                                    onClick  = { haptic.tick(); targetListId = list.id },
                                    label    = {
                                        Text(
                                            list.name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style    = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = c.copy(alpha = 0.2f),
                                        selectedLabelColor     = c
                                    )
                                )
                            }
                        }
                    }

                    // Edit-Banner
                    if (editingTodo != null) {
                        Surface(
                            color    = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier              = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint     = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        "Bearbeiten",
                                        style      = MaterialTheme.typography.labelSmall,
                                        color      = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                TextButton(
                                    onClick        = {
                                        haptic.tick()
                                        editingTodo   = null
                                        editingListId = null
                                        inputText     = ""
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("Abbrechen", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    // Eingabezeile
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value         = inputText,
                            onValueChange = { inputText = it },
                            placeholder   = {
                                Text(if (editingTodo != null) "Todo bearbeiten…" else "Neues Todo…")
                            },
                            modifier      = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            shape         = RoundedCornerShape(24.dp),
                            singleLine    = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { submitInput() }),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (editingTodo != null)
                                    MaterialTheme.colorScheme.primary
                                else targetColor
                            )
                        )
                        FilledIconButton(
                            onClick  = { submitInput() },
                            enabled  = inputText.isNotBlank(),
                            colors   = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (editingTodo != null)
                                    MaterialTheme.colorScheme.primary
                                else targetColor
                            )
                        ) {
                            Icon(
                                if (editingTodo != null) Icons.Default.Check else Icons.Default.Send,
                                contentDescription = if (editingTodo != null) "Speichern" else "Hinzufügen"
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (openTodos.isEmpty() && doneTodos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Text(
                        "Keine Todos",
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Füge dein erstes Todo unten ein.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(openTodos, key = { "${it.first.id}_${it.second.id}" }) { (list, todo) ->
                val c = remember(list.color) {
                    try { Color(android.graphics.Color.parseColor(list.color)) }
                    catch (e: Exception) { Color.Gray }
                }
                MultiTodoRow(
                    todo        = todo,
                    accentColor = c,
                    listName    = if (activeLists.size > 1) list.name else null,
                    isEditing   = editingTodo?.id == todo.id,
                    onToggle    = {
                        haptic.success()
                        val vm = viewModels.find { it.first.id == list.id }?.second
                        vm?.toggleTodo(todo)
                    },
                    onTap       = {
                        haptic.tick()
                        sheetTodo = list to todo
                    }
                )
            }

            if (doneTodos.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Erledigt (${doneTodos.size})",
                        style    = MaterialTheme.typography.labelMedium,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
                items(doneTodos, key = { "done_${it.first.id}_${it.second.id}" }) { (list, todo) ->
                    val c = remember(list.color) {
                        try { Color(android.graphics.Color.parseColor(list.color)) }
                        catch (e: Exception) { Color.Gray }
                    }
                    MultiTodoRow(
                        todo        = todo,
                        accentColor = c,
                        listName    = if (activeLists.size > 1) list.name else null,
                        isEditing   = false,
                        onToggle    = {
                            haptic.success()
                            val vm = viewModels.find { it.first.id == list.id }?.second
                            vm?.toggleTodo(todo)
                        },
                        onTap       = {
                            haptic.tick()
                            sheetTodo = list to todo
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // ── Bottom Sheet ──────────────────────────────────────────────────────────
    sheetTodo?.let { (list, todo) ->
        val sheetAccent = remember(list.color) {
            try { Color(android.graphics.Color.parseColor(list.color)) }
            catch (e: Exception) { Color(0xFF5B3A8C) }
        }

        ModalBottomSheet(
            onDismissRequest = { haptic.tick(); sheetTodo = null },
            sheetState       = sheetState,
            containerColor   = MaterialTheme.colorScheme.surface,
            shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle       = {
                Box(
                    modifier         = Modifier.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.width(40.dp).height(4.dp),
                        shape    = RoundedCornerShape(2.dp),
                        color    = MaterialTheme.colorScheme.outlineVariant
                    ) {}
                }
            }
        ) {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Todo-Vorschau
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    color    = sheetAccent.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier              = Modifier.padding(14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint     = sheetAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text       = todo.title,
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.onSurface,
                                maxLines   = 2,
                                overflow   = TextOverflow.Ellipsis
                            )
                            if (activeLists.size > 1) {
                                Text(
                                    text  = list.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = sheetAccent.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // Aktions-Buttons
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Bearbeiten
                    Surface(
                        modifier = Modifier.weight(1f).height(64.dp),
                        shape    = RoundedCornerShape(16.dp),
                        color    = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        onClick  = {
                            haptic.click()
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                sheetTodo     = null
                                editingTodo   = todo
                                editingListId = list.id
                                targetListId  = list.id
                            }
                        }
                    ) {
                        Column(
                            modifier            = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                null,
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Bearbeiten",
                                style      = MaterialTheme.typography.labelMedium,
                                color      = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 11.sp
                            )
                        }
                    }

                    // Löschen
                    Surface(
                        modifier = Modifier.weight(1f).height(64.dp),
                        shape    = RoundedCornerShape(16.dp),
                        color    = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        onClick  = {
                            haptic.heavy()
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                sheetTodo = null
                                val vm = viewModels.find { it.first.id == list.id }?.second
                                vm?.deleteTodo(todo.id)
                            }
                        }
                    ) {
                        Column(
                            modifier            = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                null,
                                tint     = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Löschen",
                                style      = MaterialTheme.typography.labelMedium,
                                color      = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiTodoRow(
    todo        : TodoItem,
    accentColor : Color,
    listName    : String?,
    isEditing   : Boolean,
    onToggle    : () -> Unit,
    onTap       : () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = when {
                isEditing   -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                todo.isDone -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else        -> MaterialTheme.colorScheme.surface
            }
        ),
        border    = if (isEditing) androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.primary
        ) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (todo.isDone) 0.dp else 1.dp)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked         = todo.isDone,
                onCheckedChange = { onToggle() },
                colors          = CheckboxDefaults.colors(
                    checkedColor   = accentColor,
                    checkmarkColor = Color.White
                )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onTap)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text           = todo.title,
                    style          = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (todo.isDone) TextDecoration.LineThrough else null,
                    color          = if (todo.isDone)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.onSurface,
                    maxLines       = 3,
                    overflow       = TextOverflow.Ellipsis
                )
                if (listName != null) {
                    Text(
                        text  = listName,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}