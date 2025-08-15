package de.beigel.list.service

import android.content.Context
import androidx.work.*
import de.beigel.list.data.TaskDatabase
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
            Result.retry()
        }
    }

    private suspend fun resetDailyTasks() {
        val database = TaskDatabase.getDatabase(applicationContext)
        val taskDao = database.taskDao()

        // Alle Aufgaben von gestern und davor als "historisch" markieren
        val yesterday = LocalDate.now().minusDays(1)
        val tasks = taskDao.getTasksForDateRange(
            startDate = LocalDate.now().minusDays(30).toString(),
            endDate = yesterday.toString()
        ).first()

        // Optional: Alte Aufgaben nach 30 Tagen löschen
        val thirtyDaysAgo = LocalDate.now().minusDays(30)
        tasks.filter {
            LocalDate.parse(it.date).isBefore(thirtyDaysAgo)
        }.forEach { task ->
            taskDao.deleteTask(task)
        }

        // Neue Aufgaben für heute werden automatisch erstellt, wenn der User sie hinzufügt
    }

    private fun scheduleNextReset() {
        val workRequest = OneTimeWorkRequestBuilder<MidnightResetWorker>()
            .setInitialDelay(calculateTimeUntilMidnight(), TimeUnit.MILLISECONDS)
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
    }
}