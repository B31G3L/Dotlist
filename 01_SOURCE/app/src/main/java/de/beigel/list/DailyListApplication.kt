package de.beigel.list

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import de.beigel.list.data.BacklogManager
import de.beigel.list.service.MidnightResetWorker
import de.beigel.list.data.TaskDatabase
import de.beigel.list.repository.TaskRepository
import de.beigel.list.settings.SettingsManager
import kotlinx.coroutines.*

class DailyListApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        scheduleMidnightReset()
        // Einmalige Migration für bestehende Installationen
        migrateExistingData()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "daily_reminders",
                "Tägliche Erinnerungen",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Benachrichtigungen für ausstehende Aufgaben"
                setShowBadge(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun scheduleMidnightReset() {
        MidnightResetWorker.scheduleInitialWork(this)
    }

    private fun migrateExistingData() {
        // Führt eine einmalige Datenmigration für Updates durch
        val prefs = getSharedPreferences("app_migration", Context.MODE_PRIVATE)
        val hasRunBacklogMigration = prefs.getBoolean("backlog_migration_v3", false)

        if (!hasRunBacklogMigration) {
            applicationScope.launch {
                try {
                    val database = TaskDatabase.getDatabase(this@DailyListApplication)
                    val settingsManager = SettingsManager(this@DailyListApplication)
                    val repository = TaskRepository(database.taskDao())
                    val backlogManager = BacklogManager(repository, settingsManager)

                    // Optimiere die Verteilung zwischen Daily und Backlog
                    backlogManager.optimizeDaily()

                    // Markiere Migration als abgeschlossen
                    prefs.edit().putBoolean("backlog_migration_v3", true).apply()
                } catch (e: Exception) {
                    // Log error if needed
                }
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel()
    }
}