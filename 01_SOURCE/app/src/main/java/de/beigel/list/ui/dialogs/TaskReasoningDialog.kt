package de.beigel.list.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.beigel.list.utils.TaskReasoning
import de.beigel.list.utils.ScoreCategory
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun TaskReasoningDialog(
    reasoning: TaskReasoning,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Warum ist das wichtig?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Schließen")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Task Title
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = reasoning.task.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        if (reasoning.task.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = reasoning.task.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Smart Score
                ScoreDisplay(
                    score = reasoning.score,
                    category = reasoning.getScoreCategory()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Reasons
                Text(
                    text = "📋 Bewertungsfaktoren:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                reasoning.reasons.forEach { reason ->
                    ReasonItem(reason = reason)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Recommendation
                RecommendationCard(
                    recommendation = reasoning.recommendation,
                    category = reasoning.getScoreCategory()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Detailed Analysis
                DetailedAnalysisSection(reasoning = reasoning)

                Spacer(modifier = Modifier.height(24.dp))

                // Action Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Verstanden!")
                }
            }
        }
    }
}

@Composable
fun ScoreDisplay(
    score: Double,
    category: ScoreCategory
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(category.color).copy(alpha = 0.1f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = category.icon,
                style = MaterialTheme.typography.displaySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${score.toInt()}/100",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color(category.color)
            )

            Text(
                text = "Smart Score",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = (score / 100.0).toFloat().coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(category.color),
                trackColor = Color(category.color).copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Badge(
                containerColor = Color(category.color)
            ) {
                Text(
                    text = category.displayName,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ReasonItem(reason: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = reason,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun RecommendationCard(
    recommendation: String,
    category: ScoreCategory
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(category.color).copy(alpha = 0.15f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = null,
                tint = Color(category.color),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "💡 Empfehlung",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(category.color)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = recommendation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun DetailedAnalysisSection(reasoning: TaskReasoning) {
    val task = reasoning.task

    Column {
        Text(
            text = "🔍 Detailanalyse:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Priority Analysis
        AnalysisItem(
            title = "Priorität",
            icon = "⚡",
            value = task.priority.displayName,
            description = when (task.priority) {
                de.beigel.list.data.TaskPriority.HIGH -> "Hohe Priorität erhöht den Score erheblich"
                de.beigel.list.data.TaskPriority.MEDIUM -> "Normale Priorität mit standardmäßigem Score"
                de.beigel.list.data.TaskPriority.LOW -> "Niedrige Priorität, aber trotzdem wichtig"
            },
            color = Color(task.priority.color)
        )

        // Due Date Analysis
        task.dueDate?.let { dueDate ->
            val days = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(dueDate))
            AnalysisItem(
                title = "Fälligkeit",
                icon = "📅",
                value = when {
                    days < 0 -> "Überfällig"
                    days == 0L -> "Heute"
                    days == 1L -> "Morgen"
                    days <= 7 -> "Diese Woche"
                    else -> "Später"
                },
                description = when {
                    days < 0 -> "Überfällige Aufgaben haben höchste Priorität!"
                    days == 0L -> "Heute fällige Aufgaben sind sehr wichtig"
                    days == 1L -> "Morgen fällige Aufgaben sollten heute vorbereitet werden"
                    days <= 7 -> "Diese Woche fällige Aufgaben haben erhöhte Priorität"
                    else -> "Fernere Deadlines haben niedrigere Priorität"
                },
                color = when {
                    days < 0 -> MaterialTheme.colorScheme.error
                    days <= 1 -> Color(0xFFFF9800)
                    days <= 7 -> Color(0xFF2196F3)
                    else -> Color(0xFF4CAF50)
                }
            )
        }

        // Time Estimation Analysis
        task.estimatedMinutes?.let { minutes ->
            AnalysisItem(
                title = "Zeitaufwand",
                icon = "⏱️",
                value = when {
                    minutes <= 15 -> "Quick Win (${minutes} Min)"
                    minutes <= 60 -> "Kurze Aufgabe (${minutes} Min)"
                    minutes <= 120 -> "Normale Aufgabe (${minutes} Min)"
                    else -> "Große Aufgabe (${minutes} Min)"
                },
                description = when {
                    minutes <= 15 -> "Quick Wins sind perfekt für Zwischendurch"
                    minutes <= 60 -> "Ideale Länge für fokussiertes Arbeiten"
                    minutes <= 120 -> "Plane genug Zeit für diese Aufgabe ein"
                    else -> "Große Aufgabe - eventuell in kleinere Teile aufteilen"
                },
                color = when {
                    minutes <= 15 -> Color(0xFF4CAF50)
                    minutes <= 60 -> Color(0xFF2196F3)
                    minutes <= 120 -> Color(0xFFFF9800)
                    else -> Color(0xFFE53E3E)
                }
            )
        }

        // Energy Level Analysis
        AnalysisItem(
            title = "Energie",
            icon = task.energyLevel.icon,
            value = task.energyLevel.displayName,
            description = when (task.energyLevel) {
                de.beigel.list.data.EnergyLevel.LOW -> "Kann auch bei niedriger Energie erledigt werden"
                de.beigel.list.data.EnergyLevel.MEDIUM -> "Benötigt normale Konzentration"
                de.beigel.list.data.EnergyLevel.HIGH -> "Am besten bei hoher Energie und Konzentration"
            },
            color = when (task.energyLevel) {
                de.beigel.list.data.EnergyLevel.LOW -> Color(0xFF4CAF50)
                de.beigel.list.data.EnergyLevel.MEDIUM -> Color(0xFF2196F3)
                de.beigel.list.data.EnergyLevel.HIGH -> Color(0xFFE53E3E)
            }
        )

        // Context Analysis
        task.context?.let { context ->
            AnalysisItem(
                title = "Arbeitsbereich",
                icon = context.icon,
                value = context.displayName,
                description = "Kontext-spezifische Aufgaben werden zur passenden Zeit bevorzugt",
                color = Color(context.color)
            )
        }
    }
}

@Composable
fun AnalysisItem(
    title: String,
    icon: String,
    value: String,
    description: String,
    color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}