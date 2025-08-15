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
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class TaskWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = TaskDatabase.getDatabase(context)
        val tasks = database.taskDao().getTasksForDate(LocalDate.now().toString()).first()

        provideContent {
            TaskWidgetContent(
                tasks = tasks,
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
    onTaskClick: () -> androidx.glance.action.Action,
    onWidgetClick: () -> androidx.glance.action.Action
) {
    val completedCount = tasks.count { it.isCompleted }
    val totalCount = tasks.size

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(day = Color.White, night = Color(0xFF1E1E1E)))
            .padding(16.dp)
            .clickable(onWidgetClick())
    ) {
        // Header
        Text(
            text = "Daily List",
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(day = Color(0xFF3C3C3C), night = Color.White)
            ),
            modifier = GlanceModifier.fillMaxWidth()
        )

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Progress
        Text(
            text = if (totalCount > 0) "$completedCount von $totalCount" else "Keine Aufgaben",
            style = TextStyle(
                fontSize = 14.sp,
                color = ColorProvider(day = Color(0xFF009966), night = Color(0xFF00CC88))
            )
        )

        Spacer(modifier = GlanceModifier.height(12.dp))

        // Tasks (max 5)
        val displayTasks = tasks.take(5)
        displayTasks.forEach { task ->
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable(onTaskClick()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox als einfacher Indikator
                Text(
                    text = if (task.isCompleted) "✓" else "◯",
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = ColorProvider(
                            day = if (task.isCompleted) Color(0xFF009966) else Color(0xFF666666),
                            night = if (task.isCompleted) Color(0xFF00CC88) else Color(0xFF999999)
                        )
                    )
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                Text(
                    text = task.title,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = ColorProvider(
                            day = if (task.isCompleted) Color(0xFF3C3C3C).copy(alpha = 0.6f) else Color(0xFF3C3C3C),
                            night = if (task.isCompleted) Color.White.copy(alpha = 0.6f) else Color.White
                        )
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }

        if (tasks.size > 5) {
            Spacer(modifier = GlanceModifier.height(4.dp))
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
    }
}

class TaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TaskWidget()
}