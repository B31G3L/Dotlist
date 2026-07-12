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
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import de.beigel.list.R
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
            Text(stringResource(R.string.title_notifications), fontSize = 20.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            if (hasUnread) {
                TextButton(onClick = { haptic.tick(); onMarkAllRead() }) {
                    Text(stringResource(R.string.action_mark_all_read), fontSize = 13.sp)
                }
            }
        }

        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.NotificationsNone, null,
                        modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.empty_no_notifications), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                if (heute.isNotEmpty()) {
                    item { SectionLabel(stringResource(R.string.section_today)) }
                    items(heute, key = { it.id }) { n ->
                        NotificationRow(n) { haptic.tick(); onOpenNotification(n) }
                    }
                }
                if (frueher.isNotEmpty()) {
                    item { SectionLabel(stringResource(R.string.section_earlier), modifier = Modifier.padding(top = 8.dp)) }
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
    val context = LocalContext.current
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
            Text(messageFor(notification, context), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(relativeTime(notification.createdAt, context), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
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

private fun messageFor(n: AppNotification, context: Context): String = when (n.type) {
    "ZUGEWIESEN" -> context.getString(R.string.notif_assigned, n.actorName, n.todoTitle)
    "ERLEDIGT"   -> context.getString(R.string.notif_done, n.actorName, n.todoTitle)
    "KOMMENTAR"  -> context.getString(R.string.notif_comment, n.actorName, n.todoTitle)
    "EINLADUNG"  -> context.getString(R.string.notif_invite, n.actorName, n.todoTitle)
    else          -> context.getString(R.string.notif_edited, n.actorName, n.todoTitle)
}

private fun isToday(ts: Timestamp): Boolean {
    val date  = Calendar.getInstance().apply { time = ts.toDate() }
    val today = Calendar.getInstance()
    return date.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
            date.get(Calendar.YEAR) == today.get(Calendar.YEAR)
}

private fun relativeTime(ts: Timestamp, context: Context): String {
    val diffMinutes = (System.currentTimeMillis() - ts.toDate().time) / 60000
    return when {
        diffMinutes < 1    -> context.getString(R.string.time_just_now)
        diffMinutes < 60   -> context.getString(R.string.time_minutes_ago, diffMinutes.toInt())
        diffMinutes < 1440 -> context.getString(R.string.time_hours_ago, (diffMinutes / 60).toInt())
        else                -> {
            val days = (diffMinutes / 1440).toInt()
            context.resources.getQuantityString(R.plurals.time_days_ago, days, days)
        }
    }
}