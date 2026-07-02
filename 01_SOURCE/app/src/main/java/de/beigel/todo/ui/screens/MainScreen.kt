package de.beigel.todo.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.beigel.todo.data.TodoList
import de.beigel.todo.repository.TodoRepository
import de.beigel.todo.utils.HapticFeedback
import de.beigel.todo.viewmodel.ListsViewModel

// ─── Navigation ──────────────────────────────────────────────────────────────

sealed class AppScreen {
    object Aufgaben    : AppScreen()
    object Listen      : AppScreen()
    object Kalender    : AppScreen()
    object Profil      : AppScreen()
    data class ListenDetail(val list: TodoList) : AppScreen()
    object ListeErstellen : AppScreen()
    data class ListeTeilen(val list: TodoList) : AppScreen()
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

    val listsViewModel: ListsViewModel = viewModel(
        factory = ListsViewModel.Factory(repository, context)
    )
    val listsUiState by listsViewModel.uiState.collectAsStateWithLifecycle()

    // Back-Handler für Sub-Screens
    if (!screen.isTopLevel) {
        BackHandler {
            screen = when (screen) {
                is AppScreen.ListenDetail  -> AppScreen.Listen
                is AppScreen.ListeErstellen -> AppScreen.Listen
                is AppScreen.ListeTeilen   -> if ((screen as? AppScreen.ListeTeilen) != null)
                    AppScreen.Listen else AppScreen.Listen
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
        fun goBack()                  {
            screen = when (val s = screen) {
                is AppScreen.ListenDetail   -> AppScreen.Listen
                is AppScreen.ListeErstellen -> AppScreen.Listen
                is AppScreen.ListeTeilen    -> AppScreen.ListenDetail(s.list)
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
        when (val s = screen) {
            is AppScreen.Aufgaben -> AufgabenScreen(
                lists      = listsUiState.lists,
                selectedIds= listsUiState.selectedListIds,
                isLoading  = listsUiState.isLoading,
                repository = repository,
                haptic     = haptic,
                padding    = padding,
                onGoListen = { haptic.click(); nav.goListen() }
            )
            is AppScreen.Listen -> ListenScreen(
                viewModel  = listsViewModel,
                deviceId   = deviceId,
                haptic     = haptic,
                padding    = padding,
                onOpenList = { list -> haptic.click(); nav.goDetail(list) },
                onShare    = { list -> nav.goTeilen(list) },
                onErstellen= { nav.goErstellen() }
            )
            is AppScreen.Kalender -> KalenderScreen(
                lists      = listsUiState.lists,
                selectedIds= listsUiState.selectedListIds,
                repository = repository,
                haptic     = haptic,
                padding    = padding
            )
            is AppScreen.Profil -> ProfilScreen(
                listsCount = listsUiState.lists.size,
                haptic     = haptic,
                padding    = padding
            )
            is AppScreen.ListenDetail -> ListenDetailScreen(
                list    = s.list,
                repository = repository,
                haptic  = haptic,
                onBack  = { nav.goListen() },
                onShare = { nav.goTeilen(s.list) }
            )
            is AppScreen.ListeErstellen -> ListeErstellenScreen(
                onBack   = { nav.goListen() },
                onCreate = { name, color ->
                    listsViewModel.createList(name, color)
                    nav.goListen()
                }
            )
            is AppScreen.ListeTeilen -> ListeTeilenScreen(
                list   = s.list,
                haptic = haptic,
                onBack = { nav.goBack() }
            )
        }
    }
}
