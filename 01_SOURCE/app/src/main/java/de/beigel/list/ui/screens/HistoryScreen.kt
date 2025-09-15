package de.beigel.list.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.beigel.list.data.TaskEntity
import de.beigel.list.repository.TaskRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    repository: TaskRepository,
    onNavigateBack: () -> Unit
) {
    var historyData by remember { mutableStateOf<List<HistoryDayData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedPeriod by remember { mutableStateOf(HistoryPeriod.WEEK) }

    LaunchedEffect(selectedPeriod) {
        isLoading = true
        val data = mutableListOf<HistoryDayData>()

        val daysToShow = when (selectedPeriod) {
            HistoryPeriod.WEEK -> 7
            HistoryPeriod.MONTH -> 30
        }

        for (i in 0 until daysToShow) {
            val date = LocalDate.now().minusDays(i.toLong())
            val tasks: List<TaskEntity> = repository.getAllTasksForDate(date).first()
            val completionRate = repository.getCompletionRate(date)

            data.add(
                HistoryDayData(
                    date = date,
                    tasks = tasks,
                    completionRate = completionRate
                )
            )
        }

        historyData = data
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Zurück",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Historie",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = when (selectedPeriod) {
                        HistoryPeriod.WEEK -> "Letzte 7 Tage"
                        HistoryPeriod.MONTH -> "Letzter Monat"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Period Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryPeriod.values().forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = {
                            Text(
                                when (period) {
                                    HistoryPeriod.WEEK -> "7 Tage"
                                    HistoryPeriod.MONTH -> "30 Tage"
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Lade Historie...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // Statistics Overview
            if (historyData.isNotEmpty()) {
                HistoryStatsCard(historyData = historyData)
                Spacer(modifier = Modifier.height(16.dp))
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historyData) { dayData ->
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        HistoryDayCard(dayData = dayData)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryStatsCard(historyData: List<HistoryDayData>) {
    val totalTasks = historyData.sumOf { it.tasks.size }
    val completedTasks = historyData.sumOf { it.tasks.count { task -> task.isCompleted } }
    val averageCompletion = if (historyData.isNotEmpty()) {
        historyData.map { it.completionRate }.average()
    } else 0.0
    val productiveDays = historyData.count { it.completionRate >= 0.8f }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Übersicht",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Aufgaben",
                    value = totalTasks.toString(),
                    subtitle = "$completedTasks erledigt"
                )

                StatItem(
                    label = "Ø Abschlussrate",
                    value = "${(averageCompletion * 100).toInt()}%",
                    subtitle = "pro Tag"
                )

                StatItem(
                    label = "Produktive Tage",
                    value = productiveDays.toString(),
                    subtitle = "≥80% erledigt"
                )
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    subtitle: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun HistoryDayCard(dayData: HistoryDayData) {
    val isToday = dayData.date == LocalDate.now()
    val isYesterday = dayData.date == LocalDate.now().minusDays(1)
    val completedTasks = dayData.tasks.filter { it.isCompleted }
    val totalTasks = dayData.tasks.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isToday) 6.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                dayData.completionRate >= 0.8f -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Date Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when {
                            isToday -> "Heute"
                            isYesterday -> "Gestern"
                            else -> dayData.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.GERMAN)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = dayData.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                // Completion Rate Circle
                if (totalTasks > 0) {
                    CompletionCircle(
                        completionRate = dayData.completionRate,
                        completedTasks = completedTasks.size,
                        totalTasks = totalTasks
                    )
                }
            }

            if (totalTasks > 0) {
                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { dayData.completionRate },
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        dayData.completionRate >= 0.8f -> MaterialTheme.colorScheme.tertiary
                        dayData.completionRate >= 0.5f -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.error
                    },
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${completedTasks.size} von $totalTasks Aufgaben erledigt",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                // Task Preview
                if (dayData.tasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    val displayTasks = dayData.tasks.take(3)
                    displayTasks.forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(task.priority.color))
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (task.isCompleted)
                                    MaterialTheme.colorScheme.tertiary
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            if (task.isCompleted) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Erledigt",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (dayData.tasks.size > 3) {
                        Text(
                            text = "und ${dayData.tasks.size - 3} weitere...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Keine Aufgaben",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun CompletionCircle(
    completionRate: Float,
    completedTasks: Int,
    totalTasks: Int
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(48.dp)
    ) {
        CircularProgressIndicator(
            progress = { completionRate },
            modifier = Modifier.fillMaxSize(),
            color = when {
                completionRate >= 0.8f -> MaterialTheme.colorScheme.tertiary
                completionRate >= 0.5f -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.error
            },
            strokeWidth = 4.dp,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )

        Text(
            text = "${(completionRate * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

data class HistoryDayData(
    val date: LocalDate,
    val tasks: List<TaskEntity>,
    val completionRate: Float
)

enum class HistoryPeriod {
    WEEK, MONTH
}