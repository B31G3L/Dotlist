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
                onTaskClick = { taskId ->
                    actionRunCallback<ToggleTaskAction>(
                        parameters = mapOf("taskId" to taskId)
                    )
                },
                onWidgetClick = {
                    actionStartActivity<MainActivity>()
                }
            )
        }
    }
}

@Composable
fun TaskWidgetContent(
    tasks: List<TaskEntity>,
    onTaskClick: (String) -> Action,
    onWidgetClick: () -> Action
) {
    val completedCount = tasks.count { it.isCompleted }
    val totalCount = tasks.size

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(androidx.glance.color.ColorProvider(Color.White))
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
                    color = androidx.glance.color.ColorProvider(Color(0xFF3C3C3C))
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Progress
        Text(
            text = if (totalCount > 0) "$completedCount von $totalCount" else "Keine Aufgaben",
            style = TextStyle(
                fontSize = 14.sp,
                color = androidx.glance.color.ColorProvider(Color(0xFF009966))
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
                    .clickable(onTaskClick(task.id)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox
                Box(
                    modifier = GlanceModifier
                        .size(16.dp)
                        .background(
                            androidx.glance.color.ColorProvider(
                                if (task.isCompleted) Color(0xFF009966) else Color.Transparent
                            )
                        )
                        .border(
                            width = 2.dp,
                            color = androidx.glance.color.ColorProvider(Color(0xFF009966))
                        )
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                Text(
                    text = task.title,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = androidx.glance.color.ColorProvider(
                            if (task.isCompleted) Color(0xFF3C3C3C).copy(alpha = 0.6f)
                            else Color(0xFF3C3C3C)
                        )
                    ),
                    maxLines = 1
                )
            }
        }

        if (tasks.size > 5) {
            Text(
                text = "und ${tasks.size - 5} weitere...",
                style = TextStyle(
                    fontSize = 10.sp,
                    color = androidx.glance.color.ColorProvider(Color(0xFF3C3C3C).copy(alpha = 0.6f))
                )
            )
        }
    }
}

class TaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TaskWidget()
}