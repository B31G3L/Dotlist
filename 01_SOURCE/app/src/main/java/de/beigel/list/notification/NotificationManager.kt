package de.beigel.list.notification

import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.beigel.list.MainActivity
import de.beigel.list.R
import de.beigel.list.notification.TaskActionReceiver
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManager @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val CHANNEL_ID_REMINDERS = "daily_reminders"
        private const val CHANNEL_ID_TASKS = "task_updates"
        private const val CHANNEL_ID_FOCUS = "smart_focus"

        private const val NOTIFICATION_ID_DAILY_REMINDER = 1001
        private const val NOTIFICATION_ID_TASK_DUE = 1002
        private const val NOTIFICATION_ID_FOCUS_UPDATE = 1003
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager

            // Daily Reminders Channel
            val remindersChannel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Tägliche Erinnerungen",
                AndroidNotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Tägliche Erinnerungen an offene Aufgaben"
                enableVibration(true)
                setShowBadge(true)
            }

            // Task Updates Channel
            val tasksChannel = NotificationChannel(
                CHANNEL_ID_TASKS,
                "Aufgaben-Updates",
                AndroidNotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Benachrichtigungen über fällige Aufgaben"
                enableVibration(true)
                setShowBadge(true)
            }

            // Smart Focus Channel
            val focusChannel = NotificationChannel(
                CHANNEL_ID_FOCUS,
                "Smart Focus",
                AndroidNotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Updates zu deinen wichtigsten Aufgaben"
                enableVibration(false)
                setShowBadge(false)
            }

            notificationManager.createNotificationChannels(listOf(
                remindersChannel,
                tasksChannel,
                focusChannel
            ))
        }
    }

    fun sendDailyReminder(
        pendingTasksCount: Int,
        overdueTasksCount: Int,
        hasQuickWins: Boolean
    ) {
        if (!areNotificationsEnabled()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (title, text) = createDailyReminderContent(
            pendingTasksCount,
            overdueTasksCount,
            hasQuickWins
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID_DAILY_REMINDER, notification)
        }
    }

    private fun createDailyReminderContent(
        pendingCount: Int,
        overdueCount: Int,
        hasQuickWins: Boolean
    ): Pair<String, String> {
        val title = when {
            pendingCount == 0 && overdueCount == 0 -> "Alle Aufgaben erledigt! 🎉"
            overdueCount > 0 -> "⚠️ Du hast überfällige Aufgaben"
            pendingCount > 0 -> "📋 Du hast $pendingCount offene Aufgaben"
            else -> "Daily List - Zeit für neue Aufgaben!"
        }

        val text = when {
            pendingCount == 0 && overdueCount == 0 ->
                "Großartig! Du hast alle deine Aufgaben erledigt. Zeit für eine wohlverdiente Pause oder neue Ziele!"

            overdueCount > 0 && pendingCount > 0 ->
                "$overdueCount überfällige und $pendingCount weitere offene Aufgaben warten auf dich."

            overdueCount > 0 ->
                "$overdueCount Aufgaben sind überfällig. Am besten gleich damit anfangen!"

            hasQuickWins ->
                "$pendingCount Aufgaben warten auf dich. Du hast auch einige Quick Wins für zwischendurch!"

            pendingCount > 0 ->
                "$pendingCount Aufgaben stehen heute auf deiner Liste. Du schaffst das!"

            else ->
                "Starte produktiv in den Tag mit neuen Aufgaben!"
        }

        return Pair(title, text)
    }

    fun sendTaskDueNotification(
        taskTitle: String,
        taskId: Long,
        isOverdue: Boolean = false
    ) {
        if (!areNotificationsEnabled()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_task_id", taskId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isOverdue) "⚠️ Aufgabe überfällig" else "📅 Aufgabe fällig"
        val text = if (isOverdue) {
            "Die Aufgabe '$taskTitle' ist überfällig."
        } else {
            "Die Aufgabe '$taskTitle' ist heute fällig."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TASKS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(
                R.drawable.ic_notification,
                "Als erledigt markieren",
                createCompleteTaskAction(taskId)
            )
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID_TASK_DUE + taskId.toInt(), notification)
        }
    }

    fun sendSmartFocusUpdate(
        focusTasksCount: Int,
        topTaskTitle: String
    ) {
        if (!areNotificationsEnabled()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🎯 Smart Focus Update"
        val text = "Deine wichtigste Aufgabe jetzt: '$topTaskTitle'"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_FOCUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setOnlyAlertOnce(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID_FOCUS_UPDATE, notification)
        }
    }

    fun sendTaskCompletedCelebration(
        taskTitle: String,
        streakDays: Int? = null
    ) {
        if (!areNotificationsEnabled()) return

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🎉 Aufgabe erledigt!"
        val text = if (streakDays != null && streakDays > 1) {
            "'$taskTitle' erledigt! Du bist seit $streakDays Tagen am Ball! 🔥"
        } else {
            "'$taskTitle' erfolgreich erledigt! Weiter so! 💪"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TASKS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setTimeoutAfter(5000) // Auto-dismiss nach 5 Sekunden
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(System.currentTimeMillis().toInt(), notification)
        }
    }

    private fun createCompleteTaskAction(taskId: Long): PendingIntent {
        val intent = Intent(context, TaskActionReceiver::class.java).apply {
            action = "COMPLETE_TASK"
            putExtra("task_id", taskId)
        }

        return PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun areNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun cancelDailyReminder() {
        with(NotificationManagerCompat.from(context)) {
            cancel(NOTIFICATION_ID_DAILY_REMINDER)
        }
    }

    fun cancelTaskNotification(taskId: Long) {
        with(NotificationManagerCompat.from(context)) {
            cancel(NOTIFICATION_ID_TASK_DUE + taskId.toInt())
        }
    }

    fun cancelAllNotifications() {
        with(NotificationManagerCompat.from(context)) {
            cancelAll()
        }
    }
}