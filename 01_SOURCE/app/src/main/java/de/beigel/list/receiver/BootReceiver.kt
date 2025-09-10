package de.beigel.list.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.beigel.list.service.MidnightResetWorker
import de.beigel.list.service.NotificationWorker
import de.beigel.list.settings.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            // Reschedule midnight reset
            MidnightResetWorker.scheduleInitialWork(context)

            // Reschedule notifications if enabled
            CoroutineScope(Dispatchers.IO).launch {
                val settingsManager = SettingsManager(context)
                val notificationsEnabled = settingsManager.notificationsEnabled.first()

                if (notificationsEnabled) {
                    val hour = settingsManager.notificationHour.first()
                    val minute = settingsManager.notificationMinute.first()

                    NotificationWorker.scheduleDaily(context, hour, minute)
                }
            }
        }
    }
}