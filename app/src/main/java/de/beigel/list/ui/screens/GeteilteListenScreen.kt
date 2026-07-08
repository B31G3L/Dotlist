package de.beigel.list.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GroupOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.beigel.list.data.TodoList
import de.beigel.list.data.displayNameFor
import de.beigel.list.utils.HapticFeedback

@Composable
fun GeteilteListenScreen(
    lists   : List<TodoList>,
    haptic  : HapticFeedback,
    onBack  : () -> Unit,
    onOpenShare: (TodoList) -> Unit,
) {
    val sharedLists = remember(lists) { lists.filter { it.memberIds.size > 1 } }
    val avatarColors = listOf(
        Color(0xFF4F378B), Color(0xFF5B8DEF), Color(0xFF2FB6A0),
        Color(0xFFE8A04E), Color(0xFFE06FA0)
    )

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = { haptic.tick(); onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("Geteilte Listen", fontSize = 20.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface)
        }

        if (sharedLists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.GroupOff, null,
                        modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text("Noch keine geteilten Listen", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Teile eine Liste über den Einladungscode", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                itemsIndexed(sharedLists, key = { _, it -> it.id }) { index, list ->
                    val listColor = listColor(list.color)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { haptic.tick(); onOpenShare(list) }
                            .padding(horizontal = 22.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier         = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                                .background(listColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(de.beigel.list.ui.theme.iconFor(list, index), null, tint = listColor, modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(list.name, fontSize = 15.5.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "${list.memberIds.size} Mitglieder · " +
                                        list.memberIds.take(3).joinToString(", ") { list.displayNameFor(it) },
                                fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}