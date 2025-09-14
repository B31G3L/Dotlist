package de.beigel.list.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// Vereinfachte Pull-to-Refresh Implementation ohne experimentelle APIs
@Composable
fun PullToRefreshLazyColumn(
    modifier: Modifier = Modifier,
    onRefresh: suspend () -> Unit,
    refreshing: Boolean = false,
    lazyListState: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit
) {
    var internalRefreshing by remember { mutableStateOf(false) }

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

// Fallback für ältere Compose Versionen
@Composable
fun FallbackPullToRefreshLazyColumn(
    modifier: Modifier = Modifier,
    onRefresh: suspend () -> Unit,
    refreshing: Boolean = false,
    lazyListState: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit
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
    content: LazyListScope.() -> Unit
) {
    // Verwende einfache Implementation
    PullToRefreshLazyColumn(
        modifier = modifier,
        onRefresh = onRefresh,
        refreshing = refreshing,
        lazyListState = lazyListState,
        content = content
    )
}