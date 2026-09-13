package com.beigel.list2share.ui.screens

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beigel.list2share.R
import com.beigel.list2share.data.NotificationPreferences
import com.beigel.list2share.data.TodoItem
import com.beigel.list2share.data.TodoList
import com.beigel.list2share.notifications.LocalNotifier
import com.beigel.list2share.SharedText
import com.beigel.list2share.repository.CloudMigration
import com.beigel.list2share.notifications.NotificationRoute
import com.beigel.list2share.repository.TodoRepository
import com.beigel.list2share.utils.HapticFeedback
import com.beigel.list2share.viewmodel.ListsViewModel
import com.beigel.list2share.viewmodel.NotificationsViewModel
import kotlin.collections.find
import android.util.Log
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

// ─── Navigation ──────────────────────────────────────────────────────────────

sealed class AppScreen {
    object Aufgaben    : AppScreen()
    object Listen      : AppScreen()
    object Kalender    : AppScreen()
    object Profil      : AppScreen()
    data class ListenDetail(val list: TodoList) : AppScreen()
    object ListeErstellen : AppScreen()
    data class ListeTeilen(val list: TodoList) : AppScreen()
    object Suche             : AppScreen()
    object Benachrichtigungen : AppScreen()
    object Konto              : AppScreen()
    object GeteilteListen     : AppScreen()
    object Hilfe              : AppScreen()
    data class AufgabeDetail(val list: TodoList, val todo: TodoItem, val from: AppScreen) : AppScreen()
}

private val AppScreen.isTopLevel
    get() = this is AppScreen.Aufgaben || this is AppScreen.Listen ||
            this is AppScreen.Kalender || this is AppScreen.Profil

private enum class NavTab(
    val screen        : AppScreen,
    val selectedIcon  : ImageVector,
    val unselectedIcon: ImageVector,
) {
    AUFGABEN(AppScreen.Aufgaben, Icons.Filled.CheckCircle,   Icons.Outlined.CheckCircle),
    LISTEN  (AppScreen.Listen,   Icons.AutoMirrored.Filled.ViewList, Icons.AutoMirrored.Outlined.ViewList),
    KALENDER(AppScreen.Kalender, Icons.Filled.CalendarMonth,  Icons.Outlined.CalendarMonth),
    PROFIL  (AppScreen.Profil,   Icons.Filled.Person,         Icons.Outlined.Person),
}

@Composable
private fun NavTab.label(): String = when (this) {
    NavTab.AUFGABEN -> stringResource(R.string.nav_tasks)
    NavTab.LISTEN   -> stringResource(R.string.nav_lists)
    NavTab.KALENDER -> stringResource(R.string.nav_calendar)
    NavTab.PROFIL   -> stringResource(R.string.nav_profile)
}

// ─── Root ─────────────────────────────────────────────────────────────────────

@Composable
fun MainScreen(repository: TodoRepository, deviceId: String) {
    val context = LocalContext.current
    val haptic  = remember { HapticFeedback(context) }

    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Aufgaben) }
    var previousTopLevel by remember { mutableStateOf<AppScreen>(AppScreen.Aufgaben) }

    // Pager für die vier Haupt-Tabs (Wisch-Navigation), synchron mit `screen`
    val pagerState = rememberPagerState(pageCount = { NavTab.entries.size })

    // Verhindert, dass der programmatische Sprung (Bottom-Nav-Klick) über mehrere
    // Zwischenseiten hinweg mit der Wisch-Synchronisation kollidiert (führte dazu,
    // dass die Navigation z. B. zwischen Kalender und Profil hängen blieb).
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    // screen -> Pager: wenn sich der Tab von außen ändert (Bottom-Nav-Klick), Pager mitscrollen
    LaunchedEffect(screen) {
        val s = screen
        if (s.isTopLevel) {
            val idx = NavTab.entries.indexOfFirst { it.screen::class == s::class }
            if (idx >= 0 && pagerState.currentPage != idx) {
                isProgrammaticScroll = true
                pagerState.animateScrollToPage(idx)
                isProgrammaticScroll = false
            }
        }
    }

    // Pager -> screen: wenn gewischt wird, `screen` synchron mitziehen
    // (nicht während eines programmatischen Sprungs, sonst überschreiben sich beide Effekte gegenseitig)
    LaunchedEffect(pagerState.currentPage) {
        if (!isProgrammaticScroll && screen.isTopLevel) {
            val target = NavTab.entries[pagerState.currentPage].screen
            if (screen::class != target::class) {
                screen = target
            }
        }
    }

    /*
     * Schlüssel auf die UID: ViewModels überleben ein recreate() der Activity.
     * Meldet sich jemand mit einem bestehenden Google-Konto an, wechselt die
     * UID – ohne den Schlüssel behielte das ViewModel das Repository der alten
     * UID und beobachtete weiterhin fremde Listen.
     */
    val listsViewModel: ListsViewModel = viewModel(
        key = "lists_$deviceId",
        factory = ListsViewModel.Factory(repository, context)
    )
    val listsUiState by listsViewModel.uiState.collectAsStateWithLifecycle()

    val notificationsViewModel: NotificationsViewModel = viewModel(
        key = "notifications_$deviceId",
        factory = NotificationsViewModel.Factory(repository)
    )
    val notificationsUiState by notificationsViewModel.uiState.collectAsStateWithLifecycle()
    val pushEnabled by NotificationPreferences.getPushEnabled(context).collectAsState(initial = true)

    // Berechtigung für Systembenachrichtigungen anfragen (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Bei neuer Benachrichtigung: zusätzlich Systembenachrichtigung zeigen, wenn im Profil aktiviert
    DisposableEffect(pushEnabled) {
        notificationsViewModel.onNewNotification = { notification ->
            if (pushEnabled) LocalNotifier.show(context, notification)
        }
        onDispose { notificationsViewModel.onNewNotification = null }
    }

    // Angetippte Benachrichtigung: zur betroffenen Liste springen, sobald sie
    // geladen ist. Ist sie nicht (mehr) dabei – gelöscht, verlassen –, wird das
    // Ziel nach dem ersten geladenen Listenstand einfach verworfen.
    val pendingRoute by NotificationRoute.pending.collectAsStateWithLifecycle()
    LaunchedEffect(pendingRoute, listsUiState.lists, listsUiState.isLoading) {
        val target = pendingRoute ?: return@LaunchedEffect
        if (listsUiState.isLoading) return@LaunchedEffect
        listsUiState.lists.find { it.id == target.listId }?.let { list ->
            screen = AppScreen.ListenDetail(list)
            listsViewModel.setLastList(list.id)
        }
        NotificationRoute.consume()
    }

    // Nach dem Anmelden: fragen, welche lokalen Listen in die Cloud sollen.
    val pendingMigration by CloudMigration.pending.collectAsStateWithLifecycle()
    if (pendingMigration.isNotEmpty()) {
        MigrationSheet(
            lists     = pendingMigration,
            onConfirm = { selected ->
                CloudMigration.migrate(context, deviceId, selected)
                CloudMigration.decline(context, pendingMigration.map { it.id }.toSet() - selected)
            },
            onDismiss = {
                // Wegwischen heißt nicht „nein für immer": beim nächsten Start
                // wird erneut gefragt.
                CloudMigration.dismiss()
            }
        )
    }

    // Aus einer anderen App geteilter Text: erst fragen, in welche Liste.
    val shareScope = rememberCoroutineScope()
    val pendingShare by SharedText.pending.collectAsStateWithLifecycle()
    if (pendingShare != null && !listsUiState.isLoading) {
        SharedTextSheet(
            share    = pendingShare!!,
            lists    = listsUiState.lists,
            onDismiss = { SharedText.consume() },
            onPick   = { list ->
                val share = pendingShare!!
                SharedText.consume()
                shareScope.launch {
                    try {
                        repository.addTodo(list.id, share.title, share.description)
                    } catch (e: Exception) {
                        Log.w("MainScreen", "Geteilter Text nicht übernommen", e)
                    }
                }
                screen = AppScreen.ListenDetail(list)
                listsViewModel.setLastList(list.id)
            }
        )
    }

    // Back-Handler für Sub-Screens
    if (!screen.isTopLevel) {
        BackHandler {
            screen = when (val s = screen) {
                is AppScreen.ListenDetail  -> AppScreen.Listen
                is AppScreen.ListeErstellen -> AppScreen.Listen
                is AppScreen.ListeTeilen   -> if ((screen as? AppScreen.ListeTeilen) != null)
                    AppScreen.Listen else AppScreen.Listen
                is AppScreen.Suche, is AppScreen.Benachrichtigungen,
                is AppScreen.Konto, is AppScreen.GeteilteListen, is AppScreen.Hilfe -> previousTopLevel
                is AppScreen.AufgabeDetail -> s.from
                else                       -> AppScreen.Listen
            }
        }
    }

    val nav = object {
        fun goAufgaben()              { screen = AppScreen.Aufgaben }
        fun goListen()                { screen = AppScreen.Listen }
        fun goKalender()              { screen = AppScreen.Kalender }
        fun goProfil()                { screen = AppScreen.Profil }
        fun goDetail(list: TodoList)  { screen = AppScreen.ListenDetail(list) }
        fun goErstellen()             { screen = AppScreen.ListeErstellen }
        fun goTeilen(list: TodoList)  { screen = AppScreen.ListeTeilen(list) }
        fun goSuche() {
            if (screen.isTopLevel) previousTopLevel = screen
            screen = AppScreen.Suche
        }
        fun goBenachrichtigungen() {
            if (screen.isTopLevel) previousTopLevel = screen
            screen = AppScreen.Benachrichtigungen
        }
        fun goKonto() {
            if (screen.isTopLevel) previousTopLevel = screen
            screen = AppScreen.Konto
        }
        fun goGeteilteListen() {
            if (screen.isTopLevel) previousTopLevel = screen
            screen = AppScreen.GeteilteListen
        }
        fun goHilfe() {
            if (screen.isTopLevel) previousTopLevel = screen
            screen = AppScreen.Hilfe
        }
        fun goAufgabeDetail(list: TodoList, todo: TodoItem) {
            screen = AppScreen.AufgabeDetail(list, todo, from = screen)
        }
        fun goBack()                  {
            screen = when (val s = screen) {
                is AppScreen.ListenDetail   -> AppScreen.Listen
                is AppScreen.ListeErstellen -> AppScreen.Listen
                is AppScreen.ListeTeilen    -> AppScreen.ListenDetail(s.list)
                is AppScreen.Suche, is AppScreen.Benachrichtigungen,
                is AppScreen.Konto, is AppScreen.GeteilteListen, is AppScreen.Hilfe -> previousTopLevel
                is AppScreen.AufgabeDetail  -> s.from
                else                        -> AppScreen.Listen
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (screen.isTopLevel) {
                NavigationBar {
                    NavTab.entries.forEach { tab ->
                        val selected = screen::class == tab.screen::class
                        NavigationBarItem(
                            selected = selected,
                            onClick  = {
                                haptic.tick()
                                screen = tab.screen
                            },
                            icon  = {
                                Icon(
                                    imageVector        = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label()
                                )
                            },
                            label = { Text(tab.label()) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (screen.isTopLevel) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (NavTab.entries[page]) {
                    NavTab.AUFGABEN -> AufgabenScreen(
                        lists      = listsUiState.lists,
                        selectedIds= listsUiState.selectedListIds,
                        isLoading  = listsUiState.isLoading,
                        repository = repository,
                        haptic     = haptic,
                        padding    = padding,
                        deviceId   = deviceId,
                        unreadNotifications = notificationsUiState.unreadCount,
                        onGoListen = { haptic.click(); nav.goListen() },
                        onSearch   = { nav.goSuche() },
                        onNotifications = { nav.goBenachrichtigungen() },
                        onOpenTask = { list, todo -> nav.goAufgabeDetail(list, todo) }
                    )
                    NavTab.LISTEN -> ListenScreen(
                        viewModel  = listsViewModel,
                        repository = repository,
                        deviceId   = deviceId,
                        haptic     = haptic,
                        padding    = padding,
                        onOpenList = { list -> haptic.click(); nav.goDetail(list) },
                        onShare    = { list -> nav.goTeilen(list) },
                        onErstellen= { nav.goErstellen() },
                        onSearch   = { nav.goSuche() },
                        onOpenTask = { list, todo -> nav.goAufgabeDetail(list, todo) }
                    )
                    NavTab.KALENDER -> KalenderScreen(
                        lists      = listsUiState.lists,
                        selectedIds= listsUiState.selectedListIds,
                        repository = repository,
                        haptic     = haptic,
                        padding    = padding
                    )
                    NavTab.PROFIL -> ProfilScreen(
                        lists      = listsUiState.lists,
                        repository = repository,
                        haptic     = haptic,
                        padding    = padding,
                        onOpenGeteilteListen = { nav.goGeteilteListen() },
                        onOpenKonto          = { nav.goKonto() },
                        onOpenHilfe          = { nav.goHilfe() }
                    )
                }
            }
            return@Scaffold
        }
        when (val s = screen) {
            is AppScreen.Suche -> SucheScreen(
                lists      = listsUiState.lists,
                repository = repository,
                haptic     = haptic,
                onBack     = { nav.goBack() }
            )
            is AppScreen.Benachrichtigungen -> BenachrichtigungenScreen(
                notifications = notificationsUiState.notifications,
                lists         = listsUiState.lists,
                deviceId      = deviceId,
                haptic        = haptic,
                onBack        = { nav.goBack() },
                onMarkAllRead = { notificationsViewModel.markAllRead() },
                onOpenNotification = { notification ->
                    notificationsViewModel.markRead(notification.id)
                    listsUiState.lists.find { it.id == notification.listId }?.let { nav.goDetail(it) }
                }
            )
            is AppScreen.Konto -> KontoScreen(
                haptic  = haptic,
                onBack  = { nav.goBack() },
                onSignedOut = {
                    (context as? Activity)?.recreate()
                }
            )
            is AppScreen.GeteilteListen -> GeteilteListenScreen(
                lists   = listsUiState.lists,
                haptic  = haptic,
                onBack  = { nav.goBack() },
                onOpenShare = { list -> nav.goTeilen(list) }
            )
            is AppScreen.Hilfe -> HilfeScreen(
                haptic = haptic,
                onBack = { nav.goBack() }
            )
            is AppScreen.ListenDetail -> ListenDetailScreen(
                list    = s.list,
                repository = repository,
                haptic  = haptic,
                deviceId = deviceId,
                onBack  = { nav.goListen() },
                onShare = { nav.goTeilen(s.list) },
                onOpenTask = { todo -> nav.goAufgabeDetail(s.list, todo) }
            )
            is AppScreen.ListeErstellen -> ListeErstellenScreen(
                onBack   = { nav.goListen() },
                onCreate = { name, color, icon, mode ->
                    listsViewModel.createList(name, color, icon, mode)
                    nav.goListen()
                }
            )
            is AppScreen.ListeTeilen -> ListeTeilenScreen(
                list            = s.list,
                currentDeviceId = deviceId,
                repository      = repository,
                haptic          = haptic,
                onBack          = { nav.goBack() }
            )
            is AppScreen.AufgabeDetail -> AufgabeDetailScreen(
                list            = s.list,
                todo            = s.todo,
                repository      = repository,
                currentDeviceId = deviceId,
                haptic          = haptic,
                onBack          = { nav.goBack() },
                allLists        = listsUiState.lists
            )
            // Aufgaben/Listen/Kalender/Profil werden weiter oben im HorizontalPager
            // gerendert (mit return@Scaffold abgefangen) – hier nie erreicht.
            else -> {}
        }
    }
}
/**
 * Auswahl, in welche Liste ein von außen geteilter Text soll.
 *
 * Bewusst ein Sheet über der laufenden App statt eines eigenen Screens: der
 * Vorgang ist kurz, und nach der Auswahl landet man ohnehin in der Liste.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SharedTextSheet(
    share    : SharedText.Pending,
    lists    : List<TodoList>,
    onDismiss: () -> Unit,
    onPick   : (TodoList) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text(
                text     = stringResource(R.string.share_target_title),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color    = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp)
            )
            Text(
                text     = share.title,
                fontSize = 14.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 22.dp).padding(bottom = 12.dp)
            )

            if (lists.isEmpty()) {
                // Ohne Liste gibt es kein Ziel – hier nur erklären, nicht
                // nebenbei eine Liste anlegen.
                Text(
                    text     = stringResource(R.string.share_target_no_lists),
                    fontSize = 14.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp)
                )
            } else {
                lists.forEach { list ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(list) }
                            .padding(horizontal = 22.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            Modifier.size(10.dp).clip(CircleShape)
                                .background(listColor(list.color))
                        )
                        Text(
                            text     = list.name,
                            fontSize = 15.sp,
                            color    = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}


/**
 * Auswahl, welche lokalen Listen beim Anmelden in die Cloud sollen.
 *
 * Alles vorausgewählt, weil das der übliche Wunsch ist – aber abwählbar:
 * wer die App bewusst ohne Konto benutzt hat, will vielleicht nicht jede
 * Liste auf einem Server haben.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MigrationSheet(
    lists    : List<TodoList>,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember(lists) { mutableStateOf(lists.map { it.id }.toSet()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text(
                text       = stringResource(R.string.migration_title),
                fontSize   = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
                modifier   = Modifier.padding(horizontal = 22.dp, vertical = 4.dp)
            )
            Text(
                text     = stringResource(R.string.migration_message),
                fontSize = 14.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 22.dp).padding(bottom = 12.dp)
            )

            lists.forEach { list ->
                val checked = list.id in selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selected = if (checked) selected - list.id else selected + list.id
                        }
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Checkbox(checked = checked, onCheckedChange = null)
                    Text(
                        text     = list.name,
                        fontSize = 15.sp,
                        color    = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { onConfirm(emptySet()) }) {
                    Text(stringResource(R.string.migration_keep_all_local))
                }
                TextButton(onClick = { onConfirm(selected) }) {
                    Text(stringResource(R.string.migration_confirm))
                }
            }
        }
    }
}
