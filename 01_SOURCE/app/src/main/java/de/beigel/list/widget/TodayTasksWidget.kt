package de.beigel.list.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import de.beigel.list.MainActivity
import de.beigel.list.data.TaskEntity

/**
 * Heute-Aufgaben Widget - Zeigt die heutigen Aufgaben mit Fortschritt
 */
class TodayTasksWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: android.content.Context, id: GlanceId) {
        val widgetRepository = WidgetRepository(context)
        val data = widgetRepository.getTodayWidgetData()

        provideContent {
            GlanceTheme {
                TodayTasksContent(data = data)
            }
        }
    }
}

@Composable
private fun TodayTasksContent(data: TodayWidgetData) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color.White))
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        // Header mit Titel und Fortschritt
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Horizontal.Start,
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "Heute",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color(0xFF009966))
                    )
                )

                Text(
                    text = "${data.completedCount}/${data.totalCount} erledigt",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = ColorProvider(Color(0xFF666666))
                    )
                )
            }

            // Fortschritts-Anzeige (vereinfacht)
            Text(
                text = "${(data.progress * 100).toInt()}%",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(
                        when {
                            data.progress >= 0.8f -> Color(0xFF10B981)
                            data.progress >= 0.5f -> Color(0xFF009966)
                            else -> Color(0xFFFF8C00)
                        }
                    )
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(12.dp))

        // Fortschrittsbalken (vereinfacht als Text)
        val progressBar = buildString {
            val segments = 10
            val filled = (data.progress * segments).toInt()
            repeat(filled) { append("█") }
            repeat(segments - filled) { append("░") }
        }

        Text(
            text = progressBar,
            style = TextStyle(
                fontSize = 12.sp,
                color = ColorProvider(Color(0xFF009966))
            )
        )

        Spacer(modifier = GlanceModifier.height(12.dp))

        // Task Liste oder Empty State
        if (data.tasks.isEmpty()) {
            EmptyTasksView()
        } else {
            TasksList(tasks = data.displayTasks)

            // "Weitere Tasks" Anzeige
            if (data.tasks.size > data.displayTasks.size) {
                Text(
                    text = "und ${data.tasks.size - data.displayTasks.size} weitere...",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = ColorProvider(Color(0xFF666666))
                    ),
                    modifier = GlanceModifier.padding(start = 8.dp, top = 4.dp)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // Limit Warnung
        if (data.isOverLimit) {
            Text(
                text = "⚠️ ${data.totalCount - data.maxTasks} über Limit",
                style = TextStyle(
                    fontSize = 10.sp,
                    color = ColorProvider(Color(0xFFE53E3E))
                )
            )
        }
    }
}

@Composable
private fun TasksList(tasks: List<TaskEntity>) {
    Column {
        tasks.forEach { task ->
            TaskItem(task = task)
            Spacer(modifier = GlanceModifier.height(6.dp))
        }
    }
}

@Composable
private fun TaskItem(task: TaskEntity) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionStartActivity<MainActivity>())
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        // Priority Indicator (vereinfacht als farbiger Text)
        Text(
            text = "●",
            style = TextStyle(
                fontSize = 12.sp,
                color = ColorProvider(Color(task.priority.color))
            )
        )

        Spacer(modifier = GlanceModifier.width(8.dp))

        // Checkbox (als Symbol)
        Text(
            text = if (task.isCompleted) "☑" else "☐",
            style = TextStyle(
                fontSize = 14.sp,
                color = ColorProvider(
                    if (task.isCompleted) Color(0xFF009966) else Color(0xFF666666)
                )
            )
        )

        Spacer(modifier = GlanceModifier.width(8.dp))

        // Task Title
        Text(
            text = task.title,
            style = TextStyle(
                fontSize = 13.sp,
                color = ColorProvider(
                    if (task.isCompleted) Color(0xFF999999) else Color(0xFF333333)
                )
            ),
            modifier = GlanceModifier.defaultWeight()
        )
    }
}

@Composable
private fun EmptyTasksView() {
    Column(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        Text(
            text = "🎯",
            style = TextStyle(fontSize = 32.sp)
        )

        Spacer(modifier = GlanceModifier.height(8.dp))

        Text(
            text = "Keine Aufgaben für heute",
            style = TextStyle(
                fontSize = 14.sp,
                color = ColorProvider(Color(0xFF666666))
            )
        )

        Spacer(modifier = GlanceModifier.height(4.dp))

        Text(
            text = "Tippe hier um eine hinzuzufügen",
            style = TextStyle(
                fontSize = 12.sp,
                color = ColorProvider(Color(0xFF009966))
            )
        )
    }
}