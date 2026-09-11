package com.beigel.list2share.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.beigel.list2share.R
import com.beigel.list2share.auth.AuthManager
import com.beigel.list2share.repository.TodoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Empfängt Firebase Cloud Messaging Push-Nachrichten.
 *
 * - [onNewToken]: wird bei App-Erststart und bei Token-Rotation aufgerufen,
 *   speichert den aktuellen Geräte-Token in Firestore
 *   (`deviceTokens/{uid}/tokens/{installationId}`, siehe [PushTokenStore]),
 *   damit die Cloud Function weiß, wohin sie pushen soll.
 * - [onMessageReceived]: wird nur aufgerufen, wenn die App im Vordergrund ist
 *   (im Hintergrund zeigt Android "notification"-Payloads automatisch an).
 *   Zeigt die Push-Nachricht in diesem Fall manuell als Systembenachrichtigung.
 */
class DotlistMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = AuthManager.currentUid ?: return
        scope.launch {
            try {
                TodoRepository(uid, this@DotlistMessagingService).saveDeviceToken(token)
            } catch (_: Exception) {
                // Wird beim nächsten App-Start / Token-Refresh erneut versucht
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: "List2Share"
        val body  = message.notification?.body ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        LocalNotifier.ensureChannel(this)

        // Die Cloud Function schickt listId/todoId im data-Teil mit; fehlt er,
        // bleibt die Benachrichtigung eben nicht antippbar.
        val listId = message.data["listId"]?.takeIf { it.isNotBlank() }
        val todoId = message.data["todoId"]?.takeIf { it.isNotBlank() }

        val builder = NotificationCompat.Builder(this, LocalNotifier.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setGroup(LocalNotifier.GROUP_KEY)
            .apply {
                listId?.let { setContentIntent(NotificationRoute.openIntent(this@DotlistMessagingService, it, todoId)) }
            }

        // Eine Benachrichtigung je Liste; ohne listId bleibt der bisherige
        // Fallback, bei dem jede Nachricht eine eigene erzeugt.
        val notificationId = listId?.hashCode() ?: message.hashCode()

        NotificationManagerCompat.from(this).notify(notificationId, builder.build())
    }
}