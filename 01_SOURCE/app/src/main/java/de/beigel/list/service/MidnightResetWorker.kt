package de.beigel.list.service

import android.content.Context
import androidx.work.*
import de.beigel.list.data.TaskDatabase
import de.beigel.list.repository.TaskRepository
import de.beigel.list.settings.SettingsManager
import de.beigel.list.data.BacklogManager
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class MidnightResetWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            resetDailyTasks()
            scheduleNextReset()
            Result.success()
        } catch (e: Exception) {
            // Log error if needed
            Result.retry()
        }
    }

    private suspend fun resetDailyTasks() {
        val database = TaskDatabase.getDatabase(applicationContext)
        val taskDao = database.taskDao()
        val settingsManager = SettingsManager(applicationContext)
        val repository = TaskRepository(taskDao)
        val backlogManager = BacklogManager(repository, settingsManager)

        // 1. Unerledigte Aufgaben von gestern optional ins Backlog verschieben
        val yesterday = LocalDate.now().minusDays(1).toString()
        val yesterdayTasks = taskDao.getDailyTasksForDate(yesterday).first()

        // Nur unerledigte Tasks ins Backlog verschieben
        val uncompletedYesterdayTasks = yesterdayTasks.filter { !it.isCompleted }

        uncompletedYesterdayTasks.forEach { task ->
            val maxBacklogPos = taskDao.getMaxBacklogPosition() ?: -1
            taskDao.moveTaskToBacklog(task.id, maxBacklogPos + 1)
        }

        // 2. Wenn Auto-Backlog aktiviert ist: Daily List für heute aus Backlog füllen
        if (settingsManager.autoBacklogEnabled) {
            repository.fillDailyListFromBacklog(settingsManager.maxDailyTasks)
        }

        // 3. Alte Aufgaben nach 30 Tagen löschen (Aufräumen)
        val thirtyDaysAgo = LocalDate.now().minusDays(30)
        val oldTasks = taskDao.getTasksForDateRange(
            startDate = LocalDate.now().minusDays(60).toString(),
            endDate = thirtyDaysAgo.toString()
        ).first()

        // Nur wirklich alte erledigte Aufgaben löschen
        oldTasks.filter { task ->
            val taskDate = LocalDate.parse(task.date)
            taskDate.isBefore(thirtyDaysAgo) && task.isCompleted
        }.forEach { task ->
            taskDao.deleteTask(task)
        }

        // 4. Backlog optimieren falls nötig
        backlogManager.optimizeDaily()
    }

    private fun scheduleNextReset() {
        val workRequest = OneTimeWorkRequestBuilder<MidnightResetWorker>()
            .setInitialDelay(calculateTimeUntilMidnight(), TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(false)
                    .setRequiresCharging(false)
                    .setRequiresDeviceIdle(false)
                    .setRequiresStorageNotLow(false)
                    .build()
            )
            .addTag("midnight_reset")
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                "midnight_reset",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
    }

    private fun calculateTimeUntilMidnight(): Long {
        val now = LocalDateTime.now()
        val midnight = LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.MIDNIGHT)
        return ChronoUnit.MILLIS.between(now, midnight)
    }

    companion object {
        fun scheduleInitialWork(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<MidnightResetWorker>()
                .setInitialDelay(calculateTimeUntilMidnight(), TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(false)
                        .setRequiresCharging(false)
                        .setRequiresDeviceIdle(false)
                        .setRequiresStorageNotLow(false)
                        .build()
                )
                .addTag("midnight_reset")
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "midnight_reset",
                    ExistingWorkPolicy.KEEP,
                    workRequest
                )
        }

        private fun calculateTimeUntilMidnight(): Long {
            val now = LocalDateTime.now()
            val midnight = LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.MIDNIGHT)
            return ChronoUnit.MILLIS.between(now, midnight)
        }

        fun cancelWork(context: Context) {
            WorkManager.getInstance(context)
                .cancelUniqueWork("midnight_reset")
        }
    }
}