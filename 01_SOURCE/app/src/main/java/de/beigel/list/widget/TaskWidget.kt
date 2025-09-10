package de.beigel.list.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import androidx.glance.action.clickable
import de.beigel.list.data.TaskEntity
import de.beigel.list.data.TaskDatabase
import de.beigel.list.repository.TaskRepository
import de.beigel.list.settings.SettingsManager
import de.beigel.list.utils.TaskPriorityCalculator
import kotlinx.coroutines.flow.first

/**
 * Home Screen Widget für Daily List
 * Zeigt die wichtigsten Tasks und den aktuellen Fortschritt an
 */
class TaskWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Lade aktuelle Tasks
        val database = TaskDatabase.getDatabase(context)
        val settingsManager = SettingsManager(context)
        val priorityCalculator = TaskPriorityCalculator()
        val repository = TaskRepository(
            taskDao = database.taskDao(),
            settingsManager = settingsManager,
            priorityCalculator = priorityCalculator
        )

        val todayTasks = repository.getTodayTasksWithCompleted().first()
        val progress = repository.getTodayProgress()

        provideContent {
            TaskWidgetContent(
                tasks = todayTasks,
                completedCount = progress.completed,
                totalCount = progress.total
            )
        }
    }
}

@Composable
private fun TaskWidgetContent(
    tasks: List<TaskEntity>,
    completedCount: Int,
    totalCount: Int
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
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
                    color = ColorProvider(Color(0xFF3C3C3C))
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Progress
        ProgressSection(
            completedCount = completedCount,
            totalCount = totalCount
        )

        Spacer(modifier = GlanceModifier.height(12.dp))

        // Task List
        TaskList(tasks = tasks.take(5)) // Zeige max. 5 Tasks im Widget
    }
}

@Composable
private fun ProgressSection(
    completedCount: Int,
    totalCount: Int
) {
    val progressText = if (totalCount > 0) {
        "$completedCount von $totalCount Aufgaben"
    } else {
        "Keine Aufgaben heute"
    }

    Text(
        text = progressText,
        style = TextStyle(
            fontSize = 14.sp,
            color = ColorProvider(Color(0xFF009966))
        )
    )

    if (totalCount > 0) {
        val percentage = (completedCount * 100) / totalCount
        Text(
            text = "$percentage% erledigt",
            style = TextStyle(
                fontSize = 12.sp,
                color = ColorProvider(Color(0xFF666666))
            )
        )
    }
}

@Composable
private fun TaskList(tasks: List<TaskEntity>) {
    if (tasks.isEmpty()) {
        EmptyTaskList()
    } else {
        LazyColumn {
            items(tasks) { task ->
                TaskItem(task = task)
                Spacer(modifier = GlanceModifier.height(8.dp))
            }

            if (tasks.size == 5) {
                item {
                    Text(
                        text = "und weitere...",
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = ColorProvider(Color(0xFF666666))
                        ),
                        modifier = GlanceModifier.padding(start = 24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskItem(task: TaskEntity) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(TaskWidgetActions.openTask(task.id)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox
        TaskCheckbox(isCompleted = task.isCompleted)

        Spacer(modifier = GlanceModifier.width(8.dp))

        // Task Title
        Text(
            text = task.title,
            style = TextStyle(
                fontSize = 12.sp,
                color = ColorProvider(
                    if (task.isCompleted) Color(0xFF888888) else Color(0xFF3C3C3C)
                )
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight()
        )

        // Priority Indicator
        if (!task.isCompleted) {
            PriorityDot(priority = task.priority)
        }
    }
}

@Composable
private fun TaskCheckbox(isCompleted: Boolean) {
    Box(
        modifier = GlanceModifier
            .size(16.dp)
            .background(
                if (isCompleted) Color(0xFF009966) else Color.Transparent
            )
            .cornerRadius(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Text(
                text = "✓",
                style = TextStyle(
                    fontSize = 10.sp,
                    color = ColorProvider(Color.White)
                )
            )
        } else {
            // Border für unerledigte Tasks
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .cornerRadius(4.dp)
            )
        }
    }
}

@Composable
private fun PriorityDot(priority: de.beigel.list.data.TaskPriority) {
    val color = when (priority) {
        de.beigel.list.data.TaskPriority.HIGH -> Color(0xFFE53E3E)
        de.beigel.list.data.TaskPriority.MEDIUM -> Color(0xFF009966)
        de.beigel.list.data.TaskPriority.LOW -> Color(0xFF4A5568)
    }

    Box(
        modifier = GlanceModifier
            .size(8.dp)
            .background(color)
            .cornerRadius(4.dp)
    )
}

@Composable
private fun EmptyTaskList() {
    Column(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🎉",
            style = TextStyle(fontSize = 24.sp)
        )

        Spacer(modifier = GlanceModifier.height(8.dp))

        Text(
            text = "Alle Aufgaben erledigt!",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(Color(0xFF3C3C3C))
            )
        )

        Text(
            text = "Zeit für neue Ziele!",
            style = TextStyle(
                fontSize = 12.sp,
                color = ColorProvider(Color(0xFF666666))
            )
        )
    }
}

/**
 * Widget Receiver für AppWidget Updates
 */
class TaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TaskWidget()
}

/**
 * Actions für Widget-Interaktionen
 */
object TaskWidgetActions {
    fun openApp() = actionStartActivity<MainActivity>()

    fun openTask(taskId: Long) = actionStartActivity<MainActivity> {
        putExtra("open_task_id", taskId)
    }
}

/**
 * Utility-Funktionen für Widget-Updates
 */
object TaskWidgetManager {
    suspend fun updateAllWidgets(context: Context) {
        TaskWidget().updateAll(context)
    }

    suspend fun requestWidgetUpdate(context: Context) {
        // Force update aller Widget-Instanzen
        updateAllWidgets(context)
    }
}