package de.beigel.list.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.beigel.list.notification.NotificationWorker
import de.beigel.list.service.MidnightResetWorker
import de.beigel.list.settings.SettingsManager

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                rescheduleWorkers(context)
            }
        }
    }

    private fun rescheduleWorkers(context: Context) {
        val settingsManager = SettingsManager(context)

        // Always reschedule midnight reset
        MidnightResetWorker.scheduleInitialWork(context)

        // Reschedule notifications if enabled
        if (settingsManager.notificationsEnabled) {
            NotificationWorker.scheduleDaily(
                context,
                settingsManager.notificationHour,
                settingsManager.notificationMinute
            )
        }
    }
}