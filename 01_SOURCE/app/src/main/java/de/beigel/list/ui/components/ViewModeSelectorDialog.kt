package de.beigel.list.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.beigel.list.data.TaskEntity
import de.beigel.list.viewmodel.ViewMode
import de.beigel.list.ui.screens.SmartFocusTaskCard

// View Mode Selector Dialog
@Composable
fun ViewModeSelectorDialog(
    currentMode: ViewMode,
    onModeSelected: (ViewMode) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Ansicht wählen",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                ViewMode.values().forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = currentMode == mode,
                                onClick = { onModeSelected(mode) }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = { onModeSelected(mode) }
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "${mode.icon} ${mode.displayName}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Abbrechen")
                    }
                }
            }
        }
    }
}

// Search Content
@Composable
fun SearchContent(
    searchQuery: String,
    searchResults: List<TaskEntity>,
    availableTags: List<String>,
    selectedTag: String?,
    onTaskClick: (TaskEntity) -> Unit,
    onTaskComplete: (TaskEntity) -> Unit,
    onTagSelected: (String) -> Unit,
    onClearTagFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Tags Filter
        if (availableTags.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "🏷️ Nach Tags filtern:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (selectedTag != null) {
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = onClearTagFilter,
                                    label = { Text("Alle anzeigen") }
                                )
                            }
                        }

                        items(availableTags) { tag ->
                            FilterChip(
                                selected = selectedTag == tag,
                                onClick = { onTagSelected(tag) },
                                label = { Text(tag) }
                            )
                        }
                    }
                }
            }
        }

        // Search Results
        if (searchQuery.isNotBlank()) {
            item {
                Text(
                    text = "🔍 Suchergebnisse für '$searchQuery'",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (searchResults.isEmpty()) {
                item {
                    EmptySearchState(query = searchQuery)
                }
            } else {
                items(searchResults) { task ->
                    SmartFocusTaskCard(
                        task = task,
                        rank = 0, // No ranking in search
                        onTaskClick = { onTaskClick(task) },
                        onCompleteTask = { onTaskComplete(task) },
                        onShowReasoning = { /* No reasoning in search */ }
                    )
                }
            }
        } else {
            item {
                EmptySearchState(query = null)
            }
        }
    }
}

// Overdue Content
@Composable
fun OverdueContent(
    overdueTasks: List<TaskEntity>,
    onTaskClick: (TaskEntity) -> Unit,
    onTaskComplete: (TaskEntity) -> Unit,
    onShowReasoning: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🚨",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Überfällige Aufgaben",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "${overdueTasks.size} Aufgabe(n) sind überfällig",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        if (overdueTasks.isEmpty()) {
            item {
                EmptyOverdueState()
            }
        } else {
            items(overdueTasks) { task ->
                SmartFocusTaskCard(
                    task = task,
                    rank = 0,
                    onTaskClick = { onTaskClick(task) },
                    onCompleteTask = { onTaskComplete(task) },
                    onShowReasoning = { onShowReasoning(task) }
                )
            }
        }
    }
}

// Today Content
@Composable
fun TodayContent(
    todaysTasks: List<TaskEntity>,
    onTaskClick: (TaskEntity) -> Unit,
    onTaskComplete: (TaskEntity) -> Unit,
    onShowReasoning: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📅",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Heute fällige Aufgaben",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                        Text(
                            text = "${todaysTasks.size} Aufgabe(n) sind heute fällig",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        if (todaysTasks.isEmpty()) {
            item {
                EmptyTodayState()
            }
        } else {
            items(todaysTasks) { task ->
                SmartFocusTaskCard(
                    task = task,
                    rank = 0,
                    onTaskClick = { onTaskClick(task) },
                    onCompleteTask = { onTaskComplete(task) },
                    onShowReasoning = { onShowReasoning(task) }
                )
            }
        }
    }
}

// Quick Wins Content
@Composable
fun QuickWinsContent(
    quickWins: List<TaskEntity>,
    onTaskClick: (TaskEntity) -> Unit,
    onTaskComplete: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Quick Wins",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Aufgaben die in 15 Minuten oder weniger erledigt werden können",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        if (quickWins.isEmpty()) {
            item {
                EmptyQuickWinsState()
            }
        } else {
            items(quickWins) { task ->
                SmartFocusTaskCard(
                    task = task,
                    rank = 0,
                    onTaskClick = { onTaskClick(task) },
                    onCompleteTask = { onTaskComplete(task) },
                    onShowReasoning = { }
                )
            }
        }
    }
}

// Empty States
@Composable
fun EmptySearchState(query: String?) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🔍",
                style = MaterialTheme.typography.displayMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (query.isNullOrBlank()) {
                    "Suche nach Aufgaben"
                } else {
                    "Keine Ergebnisse gefunden"
                },
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (query.isNullOrBlank()) {
                    "Gib einen Suchbegriff ein um deine Aufgaben zu durchsuchen"
                } else {
                    "Versuche es mit anderen Begriffen oder filtere nach Tags"
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun EmptyOverdueState() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "✅",
                style = MaterialTheme.typography.displayMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Keine überfälligen Aufgaben!",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Großartig! Du bist auf dem neuesten Stand mit deinen Aufgaben.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun EmptyTodayState() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎯",
                style = MaterialTheme.typography.displayMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Keine Aufgaben für heute!",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Du hast heute keine fälligen Aufgaben. Zeit für Spontanes oder Aufgaben für morgen vorbereiten?",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun EmptyQuickWinsState() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚡",
                style = MaterialTheme.typography.displayMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Keine Quick Wins verfügbar",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Erstelle Aufgaben mit einer Zeitschätzung von 15 Minuten oder weniger um Quick Wins zu haben.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

// Motivational Components
@Composable
fun MotivationalBanner(
    message: String,
    type: MotivationType = MotivationType.ENCOURAGEMENT,
    onDismiss: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (type) {
                MotivationType.CELEBRATION -> MaterialTheme.colorScheme.primaryContainer
                MotivationType.ENCOURAGEMENT -> MaterialTheme.colorScheme.secondaryContainer
                MotivationType.GENTLE_NUDGE -> MaterialTheme.colorScheme.tertiaryContainer
                MotivationType.ACHIEVEMENT -> Color(0xFFFFD700).copy(alpha = 0.2f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (type) {
                    MotivationType.CELEBRATION -> "🎉"
                    MotivationType.ENCOURAGEMENT -> "💪"
                    MotivationType.GENTLE_NUDGE -> "🌟"
                    MotivationType.ACHIEVEMENT -> "🏆"
                },
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                color = when (type) {
                    MotivationType.CELEBRATION -> MaterialTheme.colorScheme.onPrimaryContainer
                    MotivationType.ENCOURAGEMENT -> MaterialTheme.colorScheme.onSecondaryContainer
                    MotivationType.GENTLE_NUDGE -> MaterialTheme.colorScheme.onTertiaryContainer
                    MotivationType.ACHIEVEMENT -> Color(0xFFB8860B)
                }
            )

            onDismiss?.let { dismiss ->
                IconButton(onClick = dismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Schließen",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

enum class MotivationType {
    CELEBRATION,
    ENCOURAGEMENT,
    GENTLE_NUDGE,
    ACHIEVEMENT
}

// Task Statistics Components
@Composable
fun TaskStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: String,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// Progress Indicators
@Composable
fun CircularProgressCard(
    title: String,
    progress: Float,
    progressText: String,
    icon: String,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = progress,
                    strokeWidth = 8.dp,
                    color = color,
                    trackColor = color.copy(alpha = 0.3f),
                    modifier = Modifier.size(80.dp)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = icon,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
        }
    }
}