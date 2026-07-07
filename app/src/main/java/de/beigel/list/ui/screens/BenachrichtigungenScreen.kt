package de.beigel.list.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import de.beigel.list.data.AppNotification
import de.beigel.list.data.TodoList
import de.beigel.list.utils.HapticFeedback
import java.util.Calendar

@Composable
fun BenachrichtigungenScreen(
    notifications     : List<AppNotification>,
    lists              : List<TodoList>,
    deviceId           : String,
    haptic             : HapticFeedback,
    onBack             : () -> Unit,
    onMarkAllRead      : () -> Unit,
    onOpenNotification : (AppNotification) -> Unit,
) {
    val heute  = remember(notifications) { notifications.filter { isToday(it.createdAt) } }
    val frueher = remember(notifications) { notifications.filter { !isToday(it.createdAt) } }
    val hasUnread = notifications.any { !it.isRead }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        // App-Bar
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { haptic.tick(); onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("Benachrichtigungen", fontSize = 20.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            if (hasUnread) {
                TextButton(onClick = { haptic.tick(); onMarkAllRead() }) {
                    Text("Alle lesen", fontSize = 13.sp)
                }
            }
        }

        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.NotificationsNone, null,
                        modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text("Keine Benachrichtigungen", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                if (heute.isNotEmpty()) {
                    item { SectionLabel("Heute") }
                    items(heute, key = { it.id }) { n ->
                        NotificationRow(n) { haptic.tick(); onOpenNotification(n) }
                    }
                }
                if (frueher.isNotEmpty()) {
                    item { SectionLabel("Früher", modifier = Modifier.padding(top = 8.dp)) }
                    items(frueher, key = { it.id }) { n ->
                        NotificationRow(n) { haptic.tick(); onOpenNotification(n) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: AppNotification, onClick: () -> Unit) {
    val (icon, tint) = iconFor(notification.type)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (!notification.isRead) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier         = Modifier.size(40.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                notification.actorName.take(1).uppercase().ifEmpty { "?" },
                fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(messageFor(notification), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(relativeTime(notification.createdAt), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp))
        }
        Box(
            modifier         = Modifier.size(28.dp).clip(CircleShape)
                .background(tint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
        }
        if (!notification.isRead) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
    }
}

private fun iconFor(type: String): Pair<ImageVector, Color> = when (type) {
    "ZUGEWIESEN" -> Icons.Default.PersonAdd to Color(0xFF5B8DEF)
    "ERLEDIGT"   -> Icons.Default.Check     to Color(0xFF2FB6A0)
    "KOMMENTAR"  -> Icons.Default.Comment   to Color(0xFFE8A04E)
    "EINLADUNG"  -> Icons.Default.Mail      to Color(0xFF9B6FE0)
    else          -> Icons.Default.PersonAdd to Color(0xFF5B8DEF)
}

private fun messageFor(n: AppNotification): String = when (n.type) {
    "ZUGEWIESEN" -> "${n.actorName} hat dir „${n.todoTitle}“ zugewiesen"
    "ERLEDIGT"   -> "${n.actorName} hat „${n.todoTitle}“ erledigt"
    "KOMMENTAR"  -> "${n.actorName} hat zu „${n.todoTitle}“ kommentiert"
    "EINLADUNG"  -> "${n.actorName} lädt dich zur Liste „${n.todoTitle}“ ein"
    else          -> "${n.actorName} hat „${n.todoTitle}“ bearbeitet"
}

private fun isToday(ts: Timestamp): Boolean {
    val date  = Calendar.getInstance().apply { time = ts.toDate() }
    val today = Calendar.getInstance()
    return date.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
            date.get(Calendar.YEAR) == today.get(Calendar.YEAR)
}

private fun relativeTime(ts: Timestamp): String {
    val diffMinutes = (System.currentTimeMillis() - ts.toDate().time) / 60000
    return when {
        diffMinutes < 1    -> "gerade eben"
        diffMinutes < 60   -> "vor $diffMinutes Min"
        diffMinutes < 1440 -> "vor ${diffMinutes / 60} Std"
        else                -> "vor ${diffMinutes / 1440} Tag${if (diffMinutes / 1440 > 1) "en" else ""}"
    }
}