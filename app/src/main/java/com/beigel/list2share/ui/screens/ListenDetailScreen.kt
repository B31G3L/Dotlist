package com.beigel.list2share.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.util.Log
import android.widget.Toast
import com.beigel.list2share.R
import com.beigel.list2share.data.ListMode
import com.beigel.list2share.data.MigrationPreferences
import com.beigel.list2share.data.activityOf
import com.beigel.list2share.data.departmentFor
import com.beigel.list2share.data.formatWhen
import com.beigel.list2share.data.formatPrice
import com.beigel.list2share.data.isSimple
import com.beigel.list2share.data.listMode
import com.beigel.list2share.auth.AuthManager
import com.beigel.list2share.data.TodoItem
import com.beigel.list2share.data.TodoList
import com.beigel.list2share.data.canManageMembers
import com.beigel.list2share.repository.TodoRepository
import com.beigel.list2share.ui.theme.priorityColor
import com.beigel.list2share.utils.HapticFeedback
import com.beigel.list2share.viewmodel.TodosViewModel
import com.beigel.list2share.data.DeviceIdManager
import com.beigel.list2share.data.Priority
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenDetailScreen(
    list      : TodoList,
    repository: TodoRepository,
    haptic    : HapticFeedback,
    deviceId  : String,
    onBack    : () -> Unit,
    onShare   : () -> Unit,
    onOpenTask: (TodoItem) -> Unit = {},
) {
    val context = LocalContext.current
    val vm by remember { mutableStateOf(null as TodosViewModel?) }
    val todoVm: TodosViewModel = viewModel(
        key     = "detail_${list.id}",
        factory = TodosViewModel.Factory(repository, list.id, context)
    )
    val uiState by todoVm.uiState.collectAsStateWithLifecycle()

    val mode      = list.listMode
    val shopping  = mode == ListMode.EINKAUFEN
    val checklist = mode == ListMode.CHECKLISTE
    val purchase  = mode == ListMode.ANSCHAFFUNG
    // Im Laden will man alles aus einem Gang beieinander haben. Leere
    // Abteilungen fallen weg, "Sonstiges" erscheint also nur, wenn wirklich
    // etwas drin ist. Bewusst hier und nicht im LazyColumn-Inhalt: dessen
    // Lambda ist nicht @Composable, remember() geht dort nicht.
    val openTodos = remember(uiState.todos, purchase) {
        val open = uiState.todos.filter { !it.isDone }
        // Anschaffungen nach Priorität statt nach Eingabereihenfolge: was
        // dringend gebraucht wird, gehört nach oben. Bei Gleichstand bleibt
        // die manuelle Reihenfolge erhalten.
        if (purchase) open.sortedWith(
            compareBy({ Priority.fromString(it.priority).ordinal }, { it.position })
        ) else open
    }
    val doneTodos = remember(uiState.todos) { uiState.todos.filter { it.isDone } }
    val groupedTodos = remember(openTodos) {
        openTodos.groupBy { departmentFor(it.title) }
            .toSortedMap(compareBy { it.ordinal })
    }
    val total     = uiState.todos.size
    val doneCount = doneTodos.size
    val progress  = if (total > 0) doneCount.toFloat() / total else 0f

    val listColor = listColor(list.color)
    val hasMultipleMembers = list.memberIds.size > 1
    // Lokaler Stand des Cloud-Teilen-Reglers – wird bei Umschalten sofort aktualisiert,
    // damit die UI reagiert, auch wenn der `list`-Parameter selbst (Navigations-Snapshot)
    // erst beim erneuten Betreten des Screens den echten Stand widerspiegelt.
    // Auch an isShared gekoppelt: überführt die Cloud-Migration die Liste im
    // Hintergrund, soll die Ansicht das mitbekommen.
    var listIsShared by remember(list.id, list.isShared) { mutableStateOf(list.isShared) }
    var isTogglingShare by remember { mutableStateOf(false) }
    var showUnshareConfirm by remember { mutableStateOf(false) }
    var showAdd   by remember { mutableStateOf(false) }
    var newText   by remember { mutableStateOf("") }
    val focusReq  = remember { FocusRequester() }
    val scope     = rememberCoroutineScope()
    val copySuffix = stringResource(R.string.list_copy_suffix)
    val shareTemplate = stringResource(R.string.share_list_message)
    val shareFailed = stringResource(R.string.share_list_failed)
    var isSharing by remember { mutableStateOf(false) }

    var showActivity by remember { mutableStateOf(false) }
    val activity = remember(uiState.todos, list.memberNames) { activityOf(list, uiState.todos) }

    var showOptionsSheet  by remember { mutableStateOf(false) }
    var showRenameDialog  by remember { mutableStateOf(false) }
    var renameText        by remember { mutableStateOf(list.name) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showLeaveConfirm  by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); todoVm.clearError() }
    }
    LaunchedEffect(uiState.recentlyDeleted) {
        val deleted = uiState.recentlyDeleted ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message    = context.getString(R.string.toast_task_deleted, deleted.title),
            actionLabel = context.getString(R.string.action_undo),
            duration   = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            todoVm.undoDelete()
        } else {
            todoVm.dismissRecentlyDeleted()
        }
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
                    IconButton(onClick = { haptic.tick(); showOptionsSheet = true }) {
                        Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            // Kopfbereich: Mitglieder + Fortschritt
            item {
                Column(modifier = Modifier.padding(horizontal = 22.dp)) {
                    if (hasMultipleMembers) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MemberAvatarStack(memberIds = list.memberIds, listColor = listColor)
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.members_count, list.memberIds.size),
                                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    Text(stringResource(R.string.progress_done_of_total, doneCount, total),
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
                            placeholder   = { Text(stringResource(R.string.placeholder_add_task),
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
            // Summe der offenen Anschaffungen. Einträge ohne Preis zählen nicht
            // mit, deshalb steht daneben, wie viele davon noch keinen haben.
            if (purchase && openTodos.isNotEmpty()) {
                item {
                    val total = openTodos.sumOf { it.price ?: 0.0 }
                    val missing = openTodos.count { it.price == null }
                    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp)) {
                        Text(
                            text = stringResource(R.string.purchase_total, formatPrice(total)),
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (missing > 0) {
                            Text(
                                text = pluralStringResource(R.plurals.purchase_without_price, missing, missing),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Section Aufgaben
            item { SectionLabel(stringResource(R.string.title_tasks)) }
            if (openTodos.isEmpty() && doneTodos.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.empty_no_tasks_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (shopping) {
                groupedTodos.forEach { (department, todos) ->
                    item(key = "dep_${department.name}") {
                        SectionLabel(stringResource(department.labelRes), modifier = Modifier.padding(top = 8.dp))
                    }
                    items(todos, key = { it.id }) { todo ->
                        DetailTaskRow(
                            todo      = todo,
                            listColor = listColor,
                            simple    = true,
                            shopping  = true,
                            onToggle  = { todoVm.toggleTodo(todo); haptic.tick() },
                            onDelete  = { todoVm.deleteTodo(todo.id); haptic.heavy() },
                            onClick   = { haptic.tick(); onOpenTask(todo) }
                        )
                    }
                }
            } else {
                items(openTodos, key = { it.id }) { todo ->
                    DetailTaskRow(
                        todo      = todo,
                        listColor = listColor,
                        simple    = checklist,
                        shopping  = false,
                        purchase  = purchase,
                        onToggle  = { todoVm.toggleTodo(todo); haptic.tick() },
                        onDelete  = { todoVm.deleteTodo(todo.id); haptic.heavy() },
                        onClick   = { haptic.tick(); onOpenTask(todo) }
                    )
                }
            }
            if (activity.isNotEmpty()) {
                item {
                    TextButton(
                        onClick = { haptic.tick(); showActivity = !showActivity },
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                    ) {
                        Text(
                            stringResource(
                                if (showActivity) R.string.action_hide_activity
                                else R.string.action_show_activity
                            ),
                            fontSize = 13.sp
                        )
                    }
                }
                if (showActivity) {
                    items(activity, key = { "act_${it.todoId}" }) { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (entry.who.isNotBlank()) {
                                    stringResource(R.string.activity_done_by, entry.who, entry.title)
                                } else {
                                    stringResource(R.string.activity_done, entry.title)
                                },
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = formatWhen(entry.at),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (doneTodos.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SectionLabel(stringResource(R.string.section_done_count, doneTodos.size))
                        TextButton(onClick = {
                            haptic.click()
                            scope.launch {
                                try {
                                    // Checkliste soll wiederverwendbar bleiben, deshalb
                                    // zurücksetzen statt löschen.
                                    if (checklist) repository.resetAllTodos(list.id)
                                    else repository.deleteDoneTodos(list.id)
                                } catch (e: Exception) {
                                    Log.w("ListenDetailScreen", "Erledigte nicht bearbeitet", e)
                                }
                            }
                        }) {
                            Text(
                                stringResource(
                                    if (checklist) R.string.action_reset_all else R.string.action_clear_done
                                ),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                items(doneTodos, key = { "done_${it.id}" }) { todo ->
                    DetailTaskRow(
                        todo      = todo,
                        listColor = listColor,
                        simple    = mode.isSimple,
                        shopping  = shopping,
                        purchase  = purchase,
                        onToggle  = { todoVm.toggleTodo(todo); haptic.tick() },
                        onDelete  = { todoVm.deleteTodo(todo.id); haptic.heavy() },
                        onClick   = { haptic.tick(); onOpenTask(todo) }
                    )
                }
            }
        }

        // Extended FAB
        ExtendedFloatingActionButton(
            onClick        = { haptic.click(); showAdd = true },
            modifier       = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 40.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor   = MaterialTheme.colorScheme.onPrimary,
            shape          = RoundedCornerShape(18.dp),
            icon           = { Icon(Icons.Default.Add, null) },
            text           = { Text(stringResource(R.string.fab_task), fontWeight = FontWeight.Medium, fontSize = 15.sp) }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showAdd) {
        NeueAufgabeScreen(
            lists           = listOf(list),
            initialListId   = list.id,
            currentDeviceId = deviceId,
            onDismiss       = { showAdd = false },
            onSave          = { _, title, description, priority, dueDate, assignedTo, reminderMinutes ->
                todoVm.addTodo(title, description, priority, dueDate, assignedTo, reminderMinutes)
                showAdd = false
            }
        )
    }

    // ── Options-Sheet (Bearbeiten / Duplizieren / Teilen / Löschen / Verlassen) ──
    val isOwner   = list.createdBy == deviceId
    val canManage = list.canManageMembers(deviceId)

    if (showOptionsSheet) {
        ModalBottomSheet(onDismissRequest = { showOptionsSheet = false }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                if (isOwner) {
                    OptionRow(
                        icon  = Icons.Default.Edit,
                        label = stringResource(R.string.action_edit),
                        onClick = {
                            showOptionsSheet = false
                            renameText = list.name
                            showRenameDialog = true
                        }
                    )
                }
                // ── Cloud-Teilen-Regler ────────────────────────────────────
                /*
                 * Der Regler gilt jetzt auch mit Google-Konto, solange die Liste
                 * noch lokal ist: wer die Übernahme beim Anmelden abgelehnt hat,
                 * kann sie hier nachholen. Für bereits geteilte Listen bleibt er
                 * aus, denn zurückholen geht mit Konto nicht (siehe unshareList).
                 */
                if (canManage && (!AuthManager.isSignedInWithGoogle || !listIsShared)) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Icon(
                            if (listIsShared) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            null, tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.label_share_toggle), fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                if (listIsShared) stringResource(R.string.hint_share_toggle_on)
                                else stringResource(R.string.hint_share_toggle_off),
                                fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Switch(
                            checked  = listIsShared,
                            enabled  = !isTogglingShare,
                            onCheckedChange = { turnOn ->
                                haptic.tick()
                                if (turnOn) {
                                    isTogglingShare = true
                                    scope.launch {
                                        try {
                                            repository.shareList(list, DeviceIdManager.getDeviceName(context))
                                            // Die Liste ist jetzt in der Cloud – eine früher
                                            // abgelehnte Übernahme ist damit erledigt.
                                            MigrationPreferences.removeDeclined(context, list.id)
                                            listIsShared = true
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar(context.getString(R.string.error_unknown))
                                        } finally {
                                            isTogglingShare = false
                                        }
                                    }
                                } else {
                                    showUnshareConfirm = true
                                }
                            }
                        )
                    }
                }
                if (listIsShared) {
                    OptionRow(
                        icon  = Icons.Default.Group,
                        label = stringResource(R.string.action_manage),
                        onClick = { showOptionsSheet = false; onShare() }
                    )
                }
                OptionRow(
                    icon  = Icons.Default.ContentCopy,
                    label = stringResource(R.string.action_duplicate),
                    onClick = {
                        showOptionsSheet = false
                        haptic.click()
                        scope.launch {
                            repository.duplicateList(list, DeviceIdManager.getDeviceName(context), copySuffix)
                        }
                    }
                )
                if (listIsShared) {
                    OptionRow(
                        icon  = Icons.Default.Share,
                        label = stringResource(R.string.action_share),
                        onClick = {
                            showOptionsSheet = false
                            haptic.click()
                            if (isSharing) return@OptionRow
                            isSharing = true
                            scope.launch {
                                // Der Einladungscode muss aus Firestore kommen: er steht
                                // nicht in der Liste selbst, und ohne gültigen Code kann
                                // niemand beitreten. Gibt es keinen, wird einer erzeugt.
                                val code = try {
                                    (repository.activeInvite(list.id)
                                        ?: repository.createInvite(list)).code
                                } catch (e: Exception) {
                                    Log.w("ListenDetailScreen", "Einladung für ${list.id} nicht verfügbar", e)
                                    null
                                } finally {
                                    isSharing = false
                                }

                                if (code == null) {
                                    Toast.makeText(context, shareFailed, Toast.LENGTH_SHORT).show()
                                    return@launch
                                }

                                val link = context.getString(R.string.invite_link, code)
                                val message = shareTemplate.format(list.name, code, link)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, message)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, null))
                            }
                        }
                    )
                }
                // Reihum durch die Modi: bei drei Möglichkeiten braucht es dafür
                // kein eigenes Untermenü.
                run {
                    val nextMode = ListMode.entries[(mode.ordinal + 1) % ListMode.entries.size]
                    OptionRow(
                        icon  = when (nextMode) {
                            ListMode.EINKAUFEN   -> Icons.Default.ShoppingCart
                            ListMode.CHECKLISTE  -> Icons.Default.CheckCircle
                            ListMode.ANSCHAFFUNG -> Icons.Default.Star
                            ListMode.AUFGABEN    -> Icons.AutoMirrored.Filled.List
                        },
                        label = stringResource(R.string.action_mode_switch, stringResource(nextMode.labelRes)),
                        onClick = {
                            showOptionsSheet = false
                            haptic.click()
                            scope.launch {
                                // Nichts geht verloren: Prioritäten und Termine bleiben
                                // im Dokument stehen, der Modus blendet sie nur aus.
                                try {
                                    repository.setListMode(list.id, nextMode)
                                } catch (e: Exception) {
                                    Log.w("ListenDetailScreen", "Modus für ${list.id} nicht geändert", e)
                                }
                            }
                        }
                    )
                }
                run {
                    val isMuted = list.mutedBy.contains(deviceId)
                    OptionRow(
                        icon  = if (isMuted) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                        label = if (isMuted) stringResource(R.string.action_unmute) else stringResource(R.string.action_mute),
                        onClick = {
                            showOptionsSheet = false
                            haptic.click()
                            scope.launch {
                                try { repository.setListMuted(list.id, !isMuted) } catch (_: Exception) {}
                            }
                        }
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                if (isOwner) {
                    OptionRow(
                        icon     = Icons.Default.Delete,
                        label    = stringResource(R.string.action_delete),
                        tint     = MaterialTheme.colorScheme.error,
                        onClick  = { showOptionsSheet = false; showDeleteConfirm = true }
                    )
                } else {
                    OptionRow(
                        icon     = Icons.AutoMirrored.Filled.Logout,
                        label    = stringResource(R.string.action_leave),
                        tint     = MaterialTheme.colorScheme.error,
                        onClick  = { showOptionsSheet = false; showLeaveConfirm = true }
                    )
                }
            }
        }
    }

    // ── Liste verlassen bestätigen (nur für Nicht-Ersteller) ─────────────
    var isLeaving by remember { mutableStateOf(false) }
    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isLeaving) showLeaveConfirm = false },
            title   = { Text(stringResource(R.string.dialog_leave_list_title)) },
            text    = { Text(stringResource(R.string.dialog_leave_list_message, list.name)) },
            confirmButton = {
                TextButton(
                    enabled = !isLeaving,
                    onClick = {
                        haptic.heavy()
                        isLeaving = true
                        scope.launch {
                            try {
                                repository.leaveList(list.id)
                                showLeaveConfirm = false
                                onBack()
                            } catch (e: Exception) {
                                isLeaving = false
                                val unknown = context.getString(R.string.error_unknown)
                                snackbarHostState.showSnackbar(context.getString(R.string.error_leave_failed, e.localizedMessage ?: unknown))
                            }
                        }
                    }
                ) { Text(stringResource(R.string.action_leave), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(enabled = !isLeaving, onClick = { showLeaveConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    // ── Teilen deaktivieren bestätigen (entfernt Firebase-Eintrag + Zugriff für alle) ──
    if (showUnshareConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isTogglingShare) showUnshareConfirm = false },
            title   = { Text(stringResource(R.string.dialog_unshare_title)) },
            text    = { Text(stringResource(R.string.dialog_unshare_message)) },
            confirmButton = {
                TextButton(
                    enabled = !isTogglingShare,
                    onClick = {
                        haptic.heavy()
                        isTogglingShare = true
                        scope.launch {
                            try {
                                repository.unshareList(list)
                                listIsShared = false
                                showUnshareConfirm = false
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(context.getString(R.string.error_unknown))
                            } finally {
                                isTogglingShare = false
                            }
                        }
                    }
                ) { Text(stringResource(R.string.action_disable_sharing), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(enabled = !isTogglingShare, onClick = { showUnshareConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    // ── Liste umbenennen ──────────────────────────────────────────────────
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title   = { Text(stringResource(R.string.dialog_rename_list_title)) },
            text    = {
                TextField(
                    value         = renameText,
                    onValueChange = { renameText = it },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = renameText.isNotBlank(),
                    onClick = {
                        haptic.click()
                        scope.launch { repository.renameList(list.id, renameText) }
                        showRenameDialog = false
                    }
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }

    // ── Liste löschen bestätigen ──────────────────────────────────────────
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title   = { Text(stringResource(R.string.dialog_delete_list_title)) },
            text    = { Text(stringResource(R.string.dialog_delete_list_message, list.name)) },
            confirmButton = {
                TextButton(onClick = {
                    haptic.heavy()
                    scope.launch { repository.deleteList(list.id) }
                    showDeleteConfirm = false
                    onBack()
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

@Composable
private fun OptionRow(
    icon    : ImageVector,
    label   : String,
    tint    : Color? = null,
    onClick : () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Icon(icon, null, tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, fontSize = 16.sp, color = tint ?: MaterialTheme.colorScheme.onSurface)
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
                Text(
                    text       = initial,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                    lineHeight = 11.sp,
                    style      = LocalTextStyle.current.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}

@Composable
private fun DetailTaskRow(
    todo     : TodoItem,
    listColor: Color,
    /** Ohne Prioritätspunkt – gilt für Einkaufen und Checkliste. */
    simple   : Boolean = false,
    /** Zusätzlich mit Mengenangabe – nur beim Einkaufen. */
    shopping : Boolean = false,
    /** Zusätzlich mit Preis – nur bei Anschaffungen. */
    purchase : Boolean = false,
    onToggle : () -> Unit,
    onDelete : () -> Unit,
    onClick  : () -> Unit = {},
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp)
            .padding(horizontal = 10.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Checkbox
        Box(
            modifier         = Modifier.size(24.dp).clip(CircleShape)
                .background(if (todo.isDone) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            if (todo.isDone) {
                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
            } else {
                Surface(
                    modifier = Modifier.size(24.dp), shape = CircleShape, color = Color.Transparent,
                    border   = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
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
        if (purchase && todo.price != null) {
            Text(
                text     = formatPrice(todo.price),
                fontSize = 13.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (shopping && todo.quantity.isNotBlank()) {
            Text(
                text     = todo.quantity,
                fontSize = 13.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Anschaffungen zeigen den Preis, der Prioritätspunkt bleibt daneben
        // sinnvoll: die Priorität bestimmt dort die Reihenfolge.
        if (!simple) {
            // Prioritätspunkt
            Box(Modifier.size(8.dp).clip(CircleShape).background(
                if (todo.isDone) Color.Transparent else priorityColor(Priority.fromString(todo.priority))
            ))
        }
        // Mehr-Menü
        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.MoreVert, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { showMenu = false; onDelete() }
                )
            }
        }
    }
}