package de.beigel.list.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.beigel.list.data.NotificationPreferences
import de.beigel.list.data.TodoItem
import de.beigel.list.data.TodoList
import de.beigel.list.notifications.LocalNotifier
import de.beigel.list.repository.TodoRepository
import de.beigel.list.utils.HapticFeedback
import de.beigel.list.viewmodel.ListsViewModel
import de.beigel.list.viewmodel.NotificationsViewModel

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
    val label         : String,
    val screen        : AppScreen,
    val selectedIcon  : ImageVector,
    val unselectedIcon: ImageVector,
) {
    AUFGABEN("Aufgaben", AppScreen.Aufgaben, Icons.Filled.CheckCircle,   Icons.Outlined.CheckCircle),
    LISTEN  ("Listen",   AppScreen.Listen,   Icons.Filled.ViewList,       Icons.Outlined.ViewList),
    KALENDER("Kalender", AppScreen.Kalender, Icons.Filled.CalendarMonth,  Icons.Outlined.CalendarMonth),
    PROFIL  ("Profil",   AppScreen.Profil,   Icons.Filled.Person,         Icons.Outlined.Person),
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

    // screen -> Pager: wenn sich der Tab von außen ändert (Bottom-Nav-Klick), Pager mitscrollen
    LaunchedEffect(screen) {
        val s = screen
        if (s.isTopLevel) {
            val idx = NavTab.entries.indexOfFirst { it.screen::class == s::class }
            if (idx >= 0 && pagerState.currentPage != idx) {
                pagerState.animateScrollToPage(idx)
            }
        }
    }

    // Pager -> screen: wenn gewischt wird, `screen` synchron mitziehen
    LaunchedEffect(pagerState.currentPage) {
        if (screen.isTopLevel) {
            val target = NavTab.entries[pagerState.currentPage].screen
            if (screen::class != target::class) {
                screen = target
            }
        }
    }

    val listsViewModel: ListsViewModel = viewModel(
        factory = ListsViewModel.Factory(repository, context)
    )
    val listsUiState by listsViewModel.uiState.collectAsStateWithLifecycle()

    val notificationsViewModel: NotificationsViewModel = viewModel(
        factory = NotificationsViewModel.Factory(repository)
    )
    val notificationsUiState by notificationsViewModel.uiState.collectAsStateWithLifecycle()
    val pushEnabled by NotificationPreferences.getPushEnabled(context).collectAsState(initial = true)

    // Berechtigung für Systembenachrichtigungen anfragen (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Bei neuer Benachrichtigung: zusätzlich Systembenachrichtigung zeigen, wenn im Profil aktiviert
    DisposableEffect(pushEnabled) {
        notificationsViewModel.onNewNotification = { notification ->
            if (pushEnabled) LocalNotifier.show(context, notification)
        }
        onDispose { notificationsViewModel.onNewNotification = null }
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
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) }
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
                    (context as? android.app.Activity)?.recreate()
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
                onCreate = { name, color ->
                    listsViewModel.createList(name, color)
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