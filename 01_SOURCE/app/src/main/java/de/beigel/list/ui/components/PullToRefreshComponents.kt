package de.beigel.list.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// Moderne Pull-to-Refresh mit Material 3 API
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernPullToRefreshLazyColumn(
    modifier: Modifier = Modifier,
    onRefresh: suspend () -> Unit,
    refreshing: Boolean = false,
    lazyListState: LazyListState = rememberLazyListState(),
    content: @Composable LazyListScope.() -> Unit
) {
    var internalRefreshing by remember { mutableStateOf(false) }

    val pullToRefreshState = rememberPullToRefreshState()

    // Handle refresh trigger
    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing && !internalRefreshing && !refreshing) {
            internalRefreshing = true
            try {
                onRefresh()
            } finally {
                delay(500) // Minimum refresh time for UX
                internalRefreshing = false
            }
        }
    }

    // Reset state when refresh completes
    LaunchedEffect(refreshing, internalRefreshing) {
        if (!refreshing && !internalRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }

    Box(
        modifier = modifier.nestedScroll(pullToRefreshState.nestedScrollConnection)
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            content = content
        )

        if (pullToRefreshState.isRefreshing || internalRefreshing || refreshing) {
            PullToRefreshContainer(
                modifier = Modifier.align(Alignment.TopCenter),
                state = pullToRefreshState,
            )
        }
    }
}

// Alternative: Fallback für ältere Compose Versionen
@Composable
fun FallbackPullToRefreshLazyColumn(
    modifier: Modifier = Modifier,
    onRefresh: suspend () -> Unit,
    refreshing: Boolean = false,
    lazyListState: LazyListState = rememberLazyListState(),
    content: @Composable LazyListScope.() -> Unit
) {
    var internalRefreshing by remember { mutableStateOf(false) }

    // Handle refresh
    LaunchedEffect(internalRefreshing) {
        if (internalRefreshing) {
            try {
                onRefresh()
            } finally {
                delay(500)
                internalRefreshing = false
            }
        }
    }

    Column(modifier = modifier) {
        // Simple refresh indicator
        if (internalRefreshing || refreshing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            content = content
        )
    }
}

// Smart wrapper die automatisch die beste Implementierung wählt
@Composable
fun SmartPullToRefreshLazyColumn(
    modifier: Modifier = Modifier,
    onRefresh: suspend () -> Unit,
    refreshing: Boolean = false,
    lazyListState: LazyListState = rememberLazyListState(),
    content: @Composable LazyListScope.() -> Unit
) {
    // Versuche moderne Implementation, fallback zu einfacher Version
    try {
        ModernPullToRefreshLazyColumn(
            modifier = modifier,
            onRefresh = onRefresh,
            refreshing = refreshing,
            lazyListState = lazyListState,
            content = content
        )
    } catch (e: Exception) {
        // Fallback zur einfachen Version wenn moderne APIs nicht verfügbar
        FallbackPullToRefreshLazyColumn(
            modifier = modifier,
            onRefresh = onRefresh,
            refreshing = refreshing,
            lazyListState = lazyListState,
            content = content
        )
    }
}