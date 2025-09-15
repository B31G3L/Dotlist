package de.beigel.list.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import de.beigel.list.MainActivity
import de.beigel.list.data.TaskDatabase
import de.beigel.list.data.TaskEntity
import de.beigel.list.settings.SettingsManager
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class TaskWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = TaskDatabase.getDatabase(context)
        val settingsManager = SettingsManager(context)
        val tasks: List<TaskEntity> = database.taskDao().getDailyTasksForDate(LocalDate.now().toString()).first()

        // Filter tasks based on widget settings
        val displayTasks = if (settingsManager.widgetShowCompleted) {
            tasks
        } else {
            tasks.filter { !it.isCompleted }
        }.take(settingsManager.widgetMaxItems)

        provideContent {
            TaskWidgetContent(
                tasks = displayTasks,
                totalTasks = tasks,
                onTaskClick = {
                    actionRunCallback<ToggleTaskAction>()
                },
                onWidgetClick = {
                    actionStartActivity(
                        intent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                    )
                }
            )
        }
    }
}

@Composable
fun TaskWidgetContent(
    tasks: List<TaskEntity>,
    totalTasks: List<TaskEntity>,
    onTaskClick: () -> androidx.glance.action.Action,
    onWidgetClick: () -> androidx.glance.action.Action
) {
    val completedCount = totalTasks.count { it.isCompleted }
    val totalCount = totalTasks.size
    val progress = if (totalCount > 0) (completedCount.toFloat() / totalCount.toFloat()) else 0f

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(day = Color.White, night = Color(0xFF1E1E1E)))
            .padding(16.dp)
            .clickable(onWidgetClick())
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Daily List",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(day = Color(0xFF3C3C3C), night = Color.White)
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Progress Info
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (totalCount > 0) {
                    "$completedCount von $totalCount (${(progress * 100).toInt()}%)"
                } else {
                    "Keine Aufgaben"
                },
                style = TextStyle(
                    fontSize = 12.sp,
                    color = ColorProvider(day = Color(0xFF009966), night = Color(0xFF00CC88))
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(12.dp))

        // Progress Bar Visualization (simple)
        if (totalCount > 0) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().height(4.dp)
            ) {
                // Completed portion
                Box(
                    modifier = GlanceModifier
                        .fillMaxHeight()
                        .width((120 * progress).dp)
                        .background(ColorProvider(day = Color(0xFF009966), night = Color(0xFF00CC88)))
                )
                // Remaining portion
                Box(
                    modifier = GlanceModifier
                        .fillMaxHeight()
                        .width((120 * (1 - progress)).dp)
                        .background(ColorProvider(day = Color(0xFFE0E0E0), night = Color(0xFF444444)))
                )
            }

            Spacer(modifier = GlanceModifier.height(12.dp))
        }

        // Tasks List
        if (tasks.isNotEmpty()) {
            tasks.take(5).forEach { task ->
                TaskWidgetItem(
                    task = task,
                    onTaskClick = onTaskClick
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
            }

            if (tasks.size > 5) {
                Text(
                    text = "und ${tasks.size - 5} weitere...",
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = ColorProvider(
                            day = Color(0xFF3C3C3C).copy(alpha = 0.6f),
                            night = Color.White.copy(alpha = 0.6f)
                        )
                    )
                )
            }
        } else {
            // Empty state
            Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉",
                    style = TextStyle(fontSize = 24.sp)
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "Alle erledigt!",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = ColorProvider(day = Color(0xFF009966), night = Color(0xFF00CC88))
                    )
                )
            }
        }
    }
}

@Composable
fun TaskWidgetItem(
    task: TaskEntity,
    onTaskClick: () -> androidx.glance.action.Action
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(onTaskClick()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox indicator
        Text(
            text = if (task.isCompleted) "✓" else "◯",
            style = TextStyle(
                fontSize = 14.sp,
                color = ColorProvider(
                    day = if (task.isCompleted) Color(0xFF009966) else Color(0xFF666666),
                    night = if (task.isCompleted) Color(0xFF00CC88) else Color(0xFF999999)
                )
            )
        )

        Spacer(modifier = GlanceModifier.width(8.dp))

        // Priority indicator
        Box(
            modifier = GlanceModifier
                .size(6.dp)
                .background(ColorProvider(Color(task.priority.color)))
        )

        Spacer(modifier = GlanceModifier.width(8.dp))

        // Task title
        Text(
            text = task.title,
            style = TextStyle(
                fontSize = 11.sp,
                color = ColorProvider(
                    day = if (task.isCompleted)
                        Color(0xFF3C3C3C).copy(alpha = 0.6f)
                    else
                        Color(0xFF3C3C3C),
                    night = if (task.isCompleted)
                        Color.White.copy(alpha = 0.6f)
                    else
                        Color.White
                )
            ),
            maxLines = 1
        )
    }
}

class TaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TaskWidget()
}