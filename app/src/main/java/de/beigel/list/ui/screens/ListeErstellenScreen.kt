package de.beigel.list.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.beigel.list.data.TodoList
import de.beigel.list.data.displayNameFor
import de.beigel.list.utils.HapticFeedback

@Composable
fun ListeTeilenScreen(
    list            : TodoList,
    currentDeviceId : String,
    haptic          : HapticFeedback,
    onBack          : () -> Unit,
) {
    val clipboard   = LocalClipboardManager.current
    var copied      by remember { mutableStateOf(false) }
    var linkSharing by remember { mutableStateOf(true) }

    val listColor = listColor(list.color)
    val listIdx   = 0

    val avatarColors = listOf(
        Color(0xFF4F378B), Color(0xFF5B8DEF), Color(0xFF2FB6A0),
        Color(0xFFE8A04E), Color(0xFFE06FA0)
    )

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
        // App-Bar
        item {
            Row(
                modifier          = Modifier.fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                        tint = MaterialTheme.colorScheme.onSurface)
                }
                Text("Liste teilen", fontSize = 20.sp, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }
        // Listen-Summary
        item {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier         = Modifier.size(48.dp).clip(RoundedCornerShape(15.dp))
                        .background(listColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(listIconFor(listIdx), null, tint = listColor, modifier = Modifier.size(26.dp))
                }
                Column {
                    Text(list.name, fontSize = 18.sp, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("${list.memberIds.size} Mitglieder",
                        fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
        // Code kopieren
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape    = RoundedCornerShape(16.dp),
                color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier         = Modifier.size(40.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ContentCopy, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Einladungscode", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(list.id.take(12) + "…", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                    }
                    TextButton(onClick = {
                        clipboard.setText(AnnotatedString(list.id))
                        copied = true
                        haptic.tick()
                    }) {
                        Icon(
                            if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                            null, modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (copied) "Kopiert" else "Kopieren", fontSize = 13.sp)
                    }
                }
            }
        }
        // Link-Sharing-Row
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape    = RoundedCornerShape(16.dp),
                color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier         = Modifier.size(40.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Link, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Über Link teilen", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Jeder mit Link kann bearbeiten", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                    }
                    Switch(checked = linkSharing, onCheckedChange = { linkSharing = it; haptic.tick() })
                }
            }
        }
        // Mitglieder-Section
        item { SectionLabel("Mitglieder", modifier = Modifier.padding(top = 12.dp)) }
        items(list.memberIds.take(5).toList()) { memberId ->
            val idx     = list.memberIds.indexOf(memberId)
            val name    = list.displayNameFor(memberId)
            val initial = name.take(1).uppercase().ifEmpty { "?" }
            val role    = if (memberId == list.createdBy) "Besitzer" else "Bearbeiten"
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier         = Modifier.size(42.dp).clip(CircleShape)
                        .background(avatarColors[idx % avatarColors.size]),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initial, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (memberId == currentDeviceId) "Ich" else name,
                        fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(memberId.take(14), fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(role, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(Icons.Default.ExpandMore, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}