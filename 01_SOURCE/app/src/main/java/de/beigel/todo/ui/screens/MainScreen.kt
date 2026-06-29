package de.beigel.todo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.beigel.todo.data.TodoList
import de.beigel.todo.repository.TodoRepository
import de.beigel.todo.utils.HapticFeedback
import de.beigel.todo.viewmodel.ListsViewModel
import kotlinx.coroutines.launch

private enum class Tab(
    val label        : String,
    val selectedIcon : ImageVector,
    val unselectedIcon: ImageVector,
) {
    AUFGABEN("Aufgaben", Icons.Filled.CheckCircle,  Icons.Outlined.CheckCircle),
    LISTEN  ("Listen",   Icons.Filled.List,          Icons.Outlined.List),
    KALENDER("Kalender", Icons.Filled.DateRange,     Icons.Outlined.DateRange),
    PROFIL  ("Profil",   Icons.Filled.Person,        Icons.Outlined.Person),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(repository: TodoRepository, deviceId: String) {
    val context = LocalContext.current
    val haptic  = remember { HapticFeedback(context) }
    val scope   = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(Tab.AUFGABEN) }

    val listsViewModel: ListsViewModel = viewModel(
        factory = ListsViewModel.Factory(repository, context)
    )
    val listsUiState by listsViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick  = {
                            haptic.tick()
                            currentTab = tab
                        },
                        icon     = {
                            Icon(
                                imageVector        = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label    = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (currentTab) {
                Tab.AUFGABEN -> {
                    val activeLists = listsUiState.lists.filter { it.id in listsUiState.selectedListIds }
                    if (listsUiState.isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (activeLists.isEmpty()) {
                        EmptyTodosState(onGoToLists = {
                            haptic.click()
                            currentTab = Tab.LISTEN
                        })
                    } else {
                        MultiTodosScreen(
                            activeLists = activeLists,
                            repository  = repository,
                            haptic      = haptic
                        )
                    }
                }
                Tab.LISTEN -> {
                    ListsScreen(
                        viewModel  = listsViewModel,
                        deviceId   = deviceId,
                        haptic     = haptic,
                        onOpenList = { list ->
                            listsViewModel.setLastList(list.id)
                            currentTab = Tab.AUFGABEN
                        }
                    )
                }
                Tab.KALENDER -> KalenderPlaceholder()
                Tab.PROFIL   -> SettingsScreen(onBack = { currentTab = Tab.AUFGABEN })
            }
        }
    }
}

@Composable
private fun KalenderPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier            = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector        = Icons.Filled.DateRange,
                contentDescription = null,
                modifier           = Modifier.size(64.dp),
                tint               = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Text(
                text       = "Kalender",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text  = "Kalenderansicht – demnächst verfügbar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyTodosState(onGoToLists: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary

    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.08f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_scale"
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier            = Modifier.padding(32.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale),
                shape = RoundedCornerShape(20.dp),
                color = primary.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector        = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint               = primary,
                        modifier           = Modifier.size(44.dp)
                    )
                }
            }

            Text(
                text       = "Keine Aufgaben",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text  = "Erstelle zuerst eine Liste und füge dann Aufgaben hinzu.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onGoToLists,
                shape   = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.List, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Zu den Listen")
            }
        }
    }
}
