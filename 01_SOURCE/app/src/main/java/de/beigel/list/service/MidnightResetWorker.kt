package de.beigel.list.service

import android.content.Context
import androidx.work.*
import de.beigel.list.repository.TaskRepository
import de.beigel.list.settings.SettingsManager
import de.beigel.list.notification.NotificationManager
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Worker für den täglichen Midnight Reset
 * Führt um Mitternacht folgende Aktionen aus:
 * - Verschiebt nicht erledigte Tasks zurück ins Backlog
 * - Aktualisiert Smart Scores
 * - Füllt die neue heutige Liste auf
 */
class MidnightResetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @Inject
    lateinit var repository: TaskRepository

    @Inject
    lateinit var settingsManager: SettingsManager

    override suspend fun doWork(): Result {
        return try {
            // Führe Midnight Reset durch
            repository.performMidnightReset()

            // Schedule nächsten Reset
            scheduleNextReset(applicationContext)

            Result.success()
        } catch (e: Exception) {
            // Bei Fehler versuche es erneut
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "midnight_reset"

        fun scheduleInitialWork(context: Context) {
            val workManager = WorkManager.getInstance(context)

            // Berechne Zeit bis nächste Mitternacht
            val now = LocalDateTime.now()
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
            val delay = Duration.between(now, nextMidnight)

            val request = OneTimeWorkRequestBuilder<MidnightResetWorker>()
                .setInitialDelay(delay.toMinutes(), TimeUnit.MINUTES)
                .addTag("midnight_reset")
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(false)
                        .setRequiresCharging(false)
                        .build()
                )
                .build()

            workManager.enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        private fun scheduleNextReset(context: Context) {
            // Schedule für nächste Nacht
            scheduleInitialWork(context)
        }
    }
}

/**
 * Worker für tägliche Benachrichtigungen
 * Sendet dem Nutzer Erinnerungen über offene Aufgaben
 */
class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @Inject
    lateinit var repository: TaskRepository

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var notificationManager: NotificationManager

    override suspend fun doWork(): Result {
        return try {
            val notificationsEnabled = settingsManager.notificationsEnabled.first()

            if (!notificationsEnabled) {
                return Result.success()
            }

            // Hole heutige Tasks
            val todayTasks = repository.getTodayTasks().first()
            val pendingTasks = todayTasks.filter { !it.isCompleted }
            val overdueCount = repository.getOverdueCount()

            // Erstelle und sende Benachrichtigung
            notificationManager.sendDailyReminder(
                pendingTasksCount = pendingTasks.size,
                overdueTasksCount = overdueCount,
                hasQuickWins = pendingTasks.any { it.estimatedMinutes != null && it.estimatedMinutes <= 15 }
            )

            // Schedule nächste Benachrichtigung
            scheduleNext(applicationContext)

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 2) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "daily_notification"

        fun scheduleDaily(context: Context, hour: Int, minute: Int) {
            val workManager = WorkManager.getInstance(context)

            // Berechne Verzögerung bis zur gewünschten Zeit
            val now = LocalDateTime.now()
            val targetTime = LocalTime.of(hour, minute)
            var targetDateTime = now.toLocalDate().atTime(targetTime)

            // Wenn die Zeit heute schon vorbei ist, plane für morgen
            if (targetDateTime.isBefore(now)) {
                targetDateTime = targetDateTime.plusDays(1)
            }

            val delay = Duration.between(now, targetDateTime)

            val request = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delay.toMinutes(), TimeUnit.MINUTES)
                .addTag("daily_notification")
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()

            workManager.enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancelDaily(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        private fun scheduleNext(context: Context) {
            // Hier würde man normalerweise die Settings abfragen
            // Für Einfachheit nehmen wir 9:00 Uhr als Standard
            scheduleDaily(context, 9, 0)
        }
    }
}

/**
 * Worker für Database Maintenance
 * Führt regelmäßige Wartungsarbeiten an der Datenbank durch
 */
class DatabaseMaintenanceWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @Inject
    lateinit var repository: TaskRepository

    override suspend fun doWork(): Result {
        return try {
            // Lösche sehr alte erledigte Tasks (älter als 90 Tage)
            cleanupOldTasks()

            // Aktualisiere Smart Scores für alle Tasks
            repository.refreshSmartScores()

            // Erstelle wiederkehrende Tasks für erledigte Tasks
            createRecurringTasks()

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private suspend fun cleanupOldTasks() {
        // Implementierung für das Löschen alter Tasks
        // TODO: Implementiere DAO-Methode für das Löschen alter Tasks
    }

    private suspend fun createRecurringTasks() {
        // Implementierung für wiederkehrende Tasks
        // TODO: Implementiere Logik für wiederkehrende Tasks
    }

    companion object {
        const val WORK_NAME = "database_maintenance"

        fun scheduleWeekly(context: Context) {
            val workManager = WorkManager.getInstance(context)

            val request = PeriodicWorkRequestBuilder<DatabaseMaintenanceWorker>(
                7, TimeUnit.DAYS
            )
                .addTag("maintenance")
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(true)
                        .setRequiresCharging(true)
                        .setRequiresDeviceIdle(true)
                        .build()
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

/**
 * Worker für Smart Score Updates
 * Aktualisiert regelmäßig die Smart Scores aller Tasks
 */
class SmartScoreUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @Inject
    lateinit var repository: TaskRepository

    override suspend fun doWork(): Result {
        return try {
            repository.refreshSmartScores()
            repository.refreshTodayListIfNeeded()

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "smart_score_update"

        fun scheduleHourly(context: Context) {
            val workManager = WorkManager.getInstance(context)

            val request = PeriodicWorkRequestBuilder<SmartScoreUpdateWorker>(
                1, TimeUnit.HOURS
            )
                .addTag("smart_score")
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancelHourly(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}