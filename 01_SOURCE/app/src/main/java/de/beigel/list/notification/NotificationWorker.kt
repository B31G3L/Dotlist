package de.beigel.list.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import de.beigel.list.MainActivity
import de.beigel.list.R
import de.beigel.list.data.TaskDatabase
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            createNotificationChannel()

            val database = TaskDatabase.getDatabase(applicationContext)
            val tasks = database.taskDao().getDailyTasksForDate(LocalDate.now().toString()).first()

            val pendingTasks = tasks.filter { !it.isCompleted }

            if (pendingTasks.isNotEmpty()) {
                showNotification(pendingTasks.size, tasks.size)
            }

            // Schedule next notification
            scheduleNextNotification()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Tägliche Erinnerungen für Aufgaben"
                setShowBadge(true)
                enableVibration(true)
                setSound(null, null) // No sound for better UX
            }

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(pendingTasksCount: Int, totalTasks: Int) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationText = when {
            pendingTasksCount == 1 -> "Du hast noch 1 offene Aufgabe für heute"
            pendingTasksCount > 1 -> "Du hast noch $pendingTasksCount offene Aufgaben für heute"
            else -> "Alle Aufgaben für heute erledigt! 🎉"
        }

        val progress = if (totalTasks > 0) {
            ((totalTasks - pendingTasksCount).toFloat() / totalTasks.toFloat() * 100).toInt()
        } else 0

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Daily List")
            .setContentText(notificationText)
            .setSubText("$progress% erledigt")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFF009966.toInt())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .apply {
                // Add progress bar if there are tasks
                if (totalTasks > 0) {
                    setProgress(100, progress, false)
                }

                // Add action buttons
                if (pendingTasksCount > 0) {
                    addAction(
                        R.drawable.ic_notification,
                        "App öffnen",
                        pendingIntent
                    )
                }
            }
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun scheduleNextNotification() {
        // Schedule for the same time tomorrow
        val settingsManager = de.beigel.list.settings.SettingsManager(applicationContext)

        if (settingsManager.notificationsEnabled) {
            scheduleDaily(
                applicationContext,
                settingsManager.notificationHour,
                settingsManager.notificationMinute
            )
        }
    }

    companion object {
        const val CHANNEL_ID = "daily_reminders"
        const val NOTIFICATION_ID = 1
        const val WORK_TAG = "daily_notification"

        fun scheduleDaily(context: Context, hour: Int, minute: Int) {
            val initialDelay = calculateInitialDelay(hour, minute)

            val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(false)
                        .setRequiresCharging(false)
                        .setRequiresDeviceIdle(false)
                        .build()
                )
                .addTag(WORK_TAG)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "daily_reminder",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    workRequest
                )
        }

        fun scheduleOneTime(context: Context, hour: Int, minute: Int) {
            val initialDelay = calculateInitialDelay(hour, minute)

            val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(false)
                        .setRequiresCharging(false)
                        .setRequiresDeviceIdle(false)
                        .build()
                )
                .addTag(WORK_TAG)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "one_time_reminder",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
        }

        fun cancelNotifications(context: Context) {
            WorkManager.getInstance(context)
                .cancelUniqueWork("daily_reminder")

            WorkManager.getInstance(context)
                .cancelUniqueWork("one_time_reminder")

            // Also cancel all work with our tag
            WorkManager.getInstance(context)
                .cancelAllWorkByTag(WORK_TAG)

            // Clear any existing notifications
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
        }

        private fun calculateInitialDelay(hour: Int, minute: Int): Long {
            val now = LocalDateTime.now()
            val target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
            val targetAdjusted = if (target.isBefore(now) || target.isEqual(now)) {
                target.plusDays(1)
            } else {
                target
            }

            return ChronoUnit.MILLIS.between(now, targetAdjusted)
        }

        fun getNextNotificationTime(hour: Int, minute: Int): LocalDateTime {
            val now = LocalDateTime.now()
            val target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
            return if (target.isBefore(now) || target.isEqual(now)) {
                target.plusDays(1)
            } else {
                target
            }
        }
    }
}