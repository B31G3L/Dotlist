package de.beigel.list.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import de.beigel.list.MainActivity
import de.beigel.list.R
import de.beigel.list.data.TaskDatabase
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import java.util.jar.Manifest

class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        createNotificationChannel()

        val database = TaskDatabase.getDatabase(applicationContext)
        val tasks = database.taskDao().getTasksForDate(LocalDate.now().toString()).first()

        val pendingTasks = tasks.filter { !it.isCompleted }

        if (pendingTasks.isNotEmpty()) {
            showNotification(pendingTasks.size)
        }

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Tägliche Erinnerungen für Aufgaben"
            }

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    private fun showNotification(pendingTasksCount: Int) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Daily List")
            .setContentText("Du hast noch $pendingTasksCount offene Aufgaben für heute")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFF009966.toInt())
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "daily_reminders"
        const val NOTIFICATION_ID = 1

        fun scheduleDaily(context: Context, hour: Int, minute: Int) {
            val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(calculateInitialDelay(hour, minute), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "daily_reminder",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    workRequest
                )
        }

        private fun calculateInitialDelay(hour: Int, minute: Int): Long {
            val now = java.time.LocalDateTime.now()
            val target = now.withHour(hour).withMinute(minute).withSecond(0)
            val targetAdjusted = if (target.isBefore(now)) target.plusDays(1) else target

            return java.time.Duration.between(now, targetAdjusted).toMillis()
        }
    }
}