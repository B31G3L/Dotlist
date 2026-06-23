package de.beigel.todo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.beigel.todo.data.TodoList
import de.beigel.todo.repository.TodoRepository
import de.beigel.todo.utils.HapticFeedback
import de.beigel.todo.viewmodel.ListsViewModel
import de.beigel.todo.viewmodel.TodosViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(repository: TodoRepository, deviceId: String) {
    val context      = LocalContext.current
    val haptic       = remember { HapticFeedback(context) }
    val scope        = rememberCoroutineScope()
    var showSettings by remember { mutableStateOf(false) }

    val listsViewModel: ListsViewModel = viewModel(
        factory = ListsViewModel.Factory(repository, context)
    )
    val listsUiState by listsViewModel.uiState.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(pageCount = { 2 })

    // Pager und TabRow synchron halten
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 1) haptic.tick()
    }

    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
        return
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title   = { Text("Daily List", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { haptic.tick(); showSettings = true }) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Einstellungen")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
                TabRow(selectedTabIndex = pagerState.currentPage) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick  = { haptic.tick(); scope.launch { pagerState.animateScrollToPage(0) } },
                        icon     = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                        text     = { Text("Todos") }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick  = { haptic.tick(); scope.launch { pagerState.animateScrollToPage(1) } },
                        icon     = { Icon(Icons.Default.List, contentDescription = null) },
                        text     = { Text("Listen") }
                    )
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) { page ->
            when (page) {
                0 -> {
                    val activeLists = listsUiState.lists.filter { it.id in listsUiState.selectedListIds }
                    if (listsUiState.isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (activeLists.isEmpty()) {
                        EmptyTodosState(onGoToLists = {
                            haptic.click()
                            scope.launch { pagerState.animateScrollToPage(1) }
                        })
                    } else {
                        MultiTodosScreen(
                            activeLists = activeLists,
                            repository  = repository,
                            haptic      = haptic
                        )
                    }
                }
                1 -> {
                    ListsScreen(
                        viewModel  = listsViewModel,
                        deviceId   = deviceId,
                        haptic     = haptic,
                        onOpenList = { list ->
                            listsViewModel.setLastList(list.id)
                            scope.launch { pagerState.animateScrollToPage(0) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyTodosState(onGoToLists: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary

    // Puls-Animation wie NexTime Welcome-Screen
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
            // Animiertes Icon
            Box(
                modifier         = Modifier
                    .size(80.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(20.dp))
                    .then(
                        Modifier.drawBehind { /* gradient background */ }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(scale),
                    shape    = RoundedCornerShape(20.dp),
                    color    = primary.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector        = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint               = primary,
                            modifier           = Modifier.size(44.dp)
                        )
                    }
                }
            }

            Text(
                text       = "Keine Todos",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text  = "Erstelle zuerst eine Liste und füge dann Todos hinzu.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onGoToLists,
                shape   = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.List, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Zu den Listen")
            }
        }
    }
}