package de.beigel.list.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import de.beigel.list.data.AppNotification

/**
 * Zeigt In-App-Benachrichtigungen zusätzlich als echte Android-Systembenachrichtigung an.
 *
 * Wichtig: Das funktioniert zuverlässig, solange der App-Prozess noch läuft
 * (App im Hintergrund, aber vom System noch nicht beendet). Für garantierte
 * Zustellung bei komplett geschlossener App bräuchte es Firebase Cloud
 * Messaging + einen Server (Cloud Function), das ist hier nicht umgesetzt.
 */
object LocalNotifier {
    private const val CHANNEL_ID = "dotlist_notifications"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID, "Aufgaben-Benachrichtigungen", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Änderungen an deinen Aufgaben durch andere Mitglieder" }
            manager.createNotificationChannel(channel)
        }
    }

    fun show(context: Context, notification: AppNotification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        ensureChannel(context)

        val text = messageFor(notification)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notification.actorName)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        androidx.core.app.NotificationManagerCompat.from(context)
            .notify(notification.id.hashCode(), builder.build())
    }

    private fun messageFor(n: AppNotification): String = when (n.type) {
        "ZUGEWIESEN" -> "hat dir „${n.todoTitle}“ zugewiesen"
        "ERLEDIGT"   -> "hat „${n.todoTitle}“ erledigt"
        "KOMMENTAR"  -> "hat zu „${n.todoTitle}“ kommentiert"
        "EINLADUNG"  -> "lädt dich zur Liste „${n.todoTitle}“ ein"
        else          -> "hat „${n.todoTitle}“ bearbeitet"
    }
}