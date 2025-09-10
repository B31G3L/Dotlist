package de.beigel.list

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import de.beigel.list.service.MidnightResetWorker

class DailyListApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        scheduleMidnightReset()
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
}