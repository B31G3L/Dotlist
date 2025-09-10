package de.beigel.list

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import de.beigel.list.service.MidnightResetWorker
import de.beigel.list.service.DatabaseMaintenanceWorker
import de.beigel.list.service.SmartScoreUpdateWorker

/**
 * Hauptapplikationsklasse für Daily List
 * Initialisiert alle notwendigen Services und Background-Worker
 */
@HiltAndroidApp
class DailyListApplication : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()

        // Initialisiere Background-Worker
        initializeWorkers()
    }

    private fun initializeWorkers() {
        // Schedule Midnight Reset Worker
        MidnightResetWorker.scheduleInitialWork(this)

        // Schedule Database Maintenance (wöchentlich)
        DatabaseMaintenanceWorker.scheduleWeekly(this)

        // Schedule Smart Score Updates (stündlich)
        SmartScoreUpdateWorker.scheduleHourly(this)
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR)
            .build()
    }
}