package de.beigel.list.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

    LaunchedEffect(Unit) {
        isLoading = true
        val data = mutableListOf<HistoryDayData>()

        for (i in 0..6) {
            val date = LocalDate.now().minusDays(i.toLong())
            val tasks = repository.getTasksForDate(date).first()
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
                    Icons.Default.ArrowBack,
                    contentDescription = "Zurück",
                    tint = Color(0xFF009966)
                )
            }

            Text(
                text = "Historie (7 Tage)",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF3C3C3C),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF009966))
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historyData) { dayData ->
                    HistoryDayCard(dayData = dayData)
                }
            }
        }
    }
}

@Composable
fun HistoryDayCard(dayData: HistoryDayData) {
    val isToday = dayData.date == LocalDate.now()
    val completedTasks = dayData.tasks.filter { it.isCompleted }
    val totalTasks = dayData.tasks.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isToday) 4.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) Color(0xFF009966).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
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
                        text = dayData.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF3C3C3C),
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (isToday) "Heute" else dayData.date.dayOfWeek.getDisplayName(
                            TextStyle.FULL, Locale.GERMAN
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isToday) Color(0xFF009966) else Color(0xFF3C3C3C).copy(alpha = 0.7f)
                    )
                }

                // Completion Rate
                if (totalTasks > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${(dayData.completionRate * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF009966),
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        dayData.completionRate >= 0.8f -> Color(0xFF009966)
                                        dayData.completionRate >= 0.5f -> Color(0xFFFF8C00)
                                        else -> Color(0xFFE53E3E)
                                    }
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            if (totalTasks > 0) {
                LinearProgressIndicator(
                    progress = dayData.completionRate,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF009966),
                    trackColor = Color(0xFF009966).copy(alpha = 0.3f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$completedTasks von $totalTasks Aufgaben erledigt",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF3C3C3C).copy(alpha = 0.7f)
                )
            } else {
                Text(
                    text = "Keine Aufgaben",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF3C3C3C).copy(alpha = 0.5f)
                )
            }

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
                            color = if (task.isCompleted) Color(0xFF009966) else Color(0xFF3C3C3C),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (task.isCompleted) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Erledigt",
                                tint = Color(0xFF009966),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                if (dayData.tasks.size > 3) {
                    Text(
                        text = "und ${dayData.tasks.size - 3} weitere...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF3C3C3C).copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        }
    }
}

data class HistoryDayData(
    val date: LocalDate,
    val tasks: List<TaskEntity>,
    val completionRate: Float
)