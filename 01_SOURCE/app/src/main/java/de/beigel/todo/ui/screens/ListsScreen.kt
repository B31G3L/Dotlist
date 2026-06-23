package de.beigel.todo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.beigel.todo.data.TodoList
import de.beigel.todo.ui.theme.ListColors
import de.beigel.todo.utils.HapticFeedback
import de.beigel.todo.viewmodel.ListsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(
    viewModel  : ListsViewModel,
    deviceId   : String,
    haptic     : HapticFeedback,
    onOpenList : (TodoList) -> Unit
) {
    val uiState           by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }
    LaunchedEffect(uiState.joinSuccess) {
        uiState.joinSuccess?.let {
            snackbarHostState.showSnackbar("\"${it.name}\" beigetreten!")
            viewModel.clearJoinSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("Listen", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { haptic.tick(); viewModel.showJoinDialog() }) {
                        Icon(Icons.Default.Link, contentDescription = "Liste beitreten")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { haptic.click(); viewModel.showCreateDialog() },
                icon    = { Icon(Icons.Default.Add, contentDescription = null) },
                text    = { Text("Neue Liste") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.lists.isEmpty() -> {
                    EmptyListsState(
                        onCreateClick = { haptic.click(); viewModel.showCreateDialog() },
                        onJoinClick   = { haptic.tick(); viewModel.showJoinDialog() },
                        modifier      = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color    = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text     = "Tippe auf eine Liste um sie für Todos zu aktivieren",
                                style    = MaterialTheme.typography.bodySmall,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        LazyColumn(
                            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.lists, key = { it.id }) { list ->
                                val isSelected = list.id in uiState.selectedListIds
                                SelectableListCard(
                                    list       = list,
                                    isSelected = isSelected,
                                    isOwner    = list.createdBy == deviceId,
                                    onToggle   = {
                                        haptic.tick()
                                        viewModel.toggleListSelection(list.id)
                                    },
                                    onDelete   = {
                                        haptic.heavy()
                                        viewModel.deleteList(list.id)
                                    },
                                    onLeave    = {
                                        haptic.tick()
                                        viewModel.leaveList(list.id)
                                    }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showCreateDialog) {
        CreateListDialog(
            onDismiss = { haptic.tick(); viewModel.hideCreateDialog() },
            onCreate  = { name, color -> viewModel.createList(name, color) }
        )
    }
    if (uiState.showJoinDialog) {
        JoinListDialog(
            onDismiss = { haptic.tick(); viewModel.hideJoinDialog() },
            onJoin    = { id -> viewModel.joinList(id) }
        )
    }
}

@Composable
private fun SelectableListCard(
    list      : TodoList,
    isSelected: Boolean,
    isOwner   : Boolean,
    onToggle  : () -> Unit,
    onDelete  : () -> Unit,
    onLeave   : () -> Unit
) {
    var showMenu          by remember { mutableStateOf(false) }
    var showShareSheet    by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val listColor = remember(list.color) {
        try { Color(android.graphics.Color.parseColor(list.color)) }
        catch (e: Exception) { Color(0xFF5B3A8C) }
    }

    Card(
        modifier  = Modifier.fillMaxWidth().clickable { onToggle() },
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(
            1.5.dp, MaterialTheme.colorScheme.primary
        ) else null
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier         = Modifier.size(48.dp).clip(CircleShape).background(listColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = if (isSelected) Icons.Default.Check else Icons.Default.List,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = list.name,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color      = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    text  = "${list.memberIds.size} ${if (list.memberIds.size == 1) "Person" else "Personen"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text       = "Aktiv",
                        style      = MaterialTheme.typography.labelSmall,
                        color      = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            IconButton(onClick = { showShareSheet = true }) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Teilen",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Mehr")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (isOwner) {
                        DropdownMenuItem(
                            text        = { Text("Liste löschen", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            },
                            onClick = { showMenu = false; showDeleteConfirm = true }
                        )
                    } else {
                        DropdownMenuItem(
                            text        = { Text("Liste verlassen") },
                            leadingIcon = { Icon(Icons.Default.ExitToApp, null) },
                            onClick     = { showMenu = false; onLeave() }
                        )
                    }
                }
            }
        }
    }

    if (showShareSheet) {
        ShareListSheet(list = list, onDismiss = { showShareSheet = false })
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title   = { Text("Liste löschen?") },
            text    = { Text("\"${list.name}\" wird für alle Mitglieder gelöscht.") },
            confirmButton   = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("Löschen", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton   = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Abbrechen") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareListSheet(list: TodoList, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var copied    by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Liste teilen",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Schicke diesen Code an die Person, die beitreten soll:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                color    = MaterialTheme.colorScheme.primaryContainer,
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier              = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text       = list.id,
                        style      = MaterialTheme.typography.bodyMedium,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium,
                        modifier   = Modifier.weight(1f)
                    )
                    IconButton(onClick = { clipboard.setText(AnnotatedString(list.id)); copied = true }) {
                        Icon(
                            if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Kopieren",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            if (copied) {
                Text("Code kopiert!", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Fertig") }
        }
    }
}

@Composable
private fun CreateListDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name          by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(ListColors.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neue Liste", fontWeight = FontWeight.Bold) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Name der Liste") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                Text("Farbe", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ListColors.forEach { colorHex ->
                        val color = remember(colorHex) {
                            try { Color(android.graphics.Color.parseColor(colorHex)) }
                            catch (e: Exception) { Color.Gray }
                        }
                        Box(
                            modifier         = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == colorHex) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint     = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = { onCreate(name, selectedColor); onDismiss() },
                enabled  = name.isNotBlank()
            ) { Text("Erstellen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

@Composable
private fun JoinListDialog(onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Liste beitreten", fontWeight = FontWeight.Bold) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Gib den Code ein, den dir jemand geschickt hat:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value         = code,
                    onValueChange = { code = it.trim() },
                    label         = { Text("Einladungscode") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = { onJoin(code); onDismiss() },
                enabled  = code.isNotBlank()
            ) { Text("Beitreten") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}

@Composable
private fun EmptyListsState(
    onCreateClick: () -> Unit,
    onJoinClick  : () -> Unit,
    modifier     : Modifier = Modifier
) {
    Column(
        modifier            = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.List,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Text(
            "Noch keine Listen",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Erstelle eine neue Liste oder tritt einer bestehenden bei.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onCreateClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Neue Liste erstellen")
        }
        OutlinedButton(onClick = onJoinClick) {
            Icon(Icons.Default.Link, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Liste beitreten")
        }
    }
}