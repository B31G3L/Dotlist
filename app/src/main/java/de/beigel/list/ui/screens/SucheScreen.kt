package de.beigel.list.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.beigel.list.R
import de.beigel.list.data.RecentSearchesPreferences
import de.beigel.list.data.TodoItem
import de.beigel.list.data.TodoList
import de.beigel.list.repository.TodoRepository
import de.beigel.list.utils.HapticFeedback
import de.beigel.list.viewmodel.TodosViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SucheScreen(
    lists      : List<TodoList>,
    repository : TodoRepository,
    haptic     : HapticFeedback,
    onBack     : () -> Unit,
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var query by remember { mutableStateOf("") }
    val recent by RecentSearchesPreferences.getRecent(context).collectAsState(initial = emptyList())

    val vms: List<Pair<TodoList, TodosViewModel>> = lists.map { list ->
        list to viewModel(key = "suche_${list.id}", factory = TodosViewModel.Factory(repository, list.id, context))
    }
    val allTodos: List<Pair<TodoList, TodoItem>> = vms.flatMap { (list, vm) ->
        vm.uiState.collectAsStateWithLifecycle().value.todos.map { list to it }
    }

    val results = remember(query, allTodos) {
        if (query.isBlank()) emptyList()
        else allTodos.filter { (list, todo) ->
            todo.title.contains(query, ignoreCase = true) || list.name.contains(query, ignoreCase = true)
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        // Suchfeld-Zeile
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = { haptic.tick(); onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Surface(
                shape    = RoundedCornerShape(28.dp),
                color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier          = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value           = query,
                        onValueChange   = { query = it },
                        placeholder     = { Text(stringResource(R.string.placeholder_search), fontSize = 15.sp) },
                        singleLine      = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (query.isNotBlank()) {
                                scope.launch { RecentSearchesPreferences.addRecent(context, query) }
                                keyboardController?.hide()
                            }
                        }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor   = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor   = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.weight(1f).focusRequester(focusRequester)
                    )
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        if (query.isBlank()) {
            // Letzte Suchen
            if (recent.isNotEmpty()) {
                SectionLabel(stringResource(R.string.section_recent_searches))
                FlowRowChips(
                    items   = recent,
                    onClick = { query = it },
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.empty_search_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        } else if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, null,
                        modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.empty_search_results, query), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            SectionLabel(stringResource(R.string.section_results_for, query))
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                items(results, key = { "${it.first.id}_${it.second.id}" }) { (list, todo) ->
                    val vm = vms.find { it.first.id == list.id }?.second
                    AufgabenTaskRow(
                        todo      = todo,
                        listName  = list.name,
                        onToggle  = { vm?.toggleTodo(todo, de.beigel.list.data.DeviceIdManager.getDeviceName(context)); haptic.tick() }
                    )
                }
            }
        }
    }
}

@Composable
private fun FlowRowChips(items: List<String>, onClick: (String) -> Unit) {
    // Einfaches Wrap-Layout ohne extra Dependency
    val rows = remember(items) {
        val result = mutableListOf<MutableList<String>>()
        var currentLen = 0
        var currentRow = mutableListOf<String>()
        items.forEach { item ->
            val len = item.length + 4
            if (currentLen + len > 34 && currentRow.isNotEmpty()) {
                result.add(currentRow)
                currentRow = mutableListOf()
                currentLen = 0
            }
            currentRow.add(item)
            currentLen += len
        }
        if (currentRow.isNotEmpty()) result.add(currentRow)
        result
    }
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { entry ->
                    Surface(
                        shape    = RoundedCornerShape(50),
                        color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.clickable { onClick(entry) }
                    ) {
                        Row(
                            modifier          = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.History, null,
                                modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(entry, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}