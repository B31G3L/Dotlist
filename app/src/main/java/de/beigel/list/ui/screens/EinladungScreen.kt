package de.beigel.list.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.beigel.list.R
import de.beigel.list.data.TodoList
import de.beigel.list.utils.HapticFeedback

@Composable
fun EinladungScreen(
    list      : TodoList,
    haptic    : HapticFeedback,
    onAccept  : () -> Unit,
    onDecline : () -> Unit,
) {
    val color = listColor(list.color)

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        // App-Bar
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { haptic.tick(); onDecline() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(stringResource(R.string.title_invitation), fontSize = 20.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface)
        }

        Column(
            modifier            = Modifier.weight(1f).fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon-Duo: Listenfarbe + Megaphone
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(bottom = 28.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy((-14).dp)) {
                    Box(
                        modifier         = Modifier.size(64.dp).clip(CircleShape).background(color.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(list.name.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 24.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Box(
                        modifier         = Modifier.size(48.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Campaign, null, tint = color, modifier = Modifier.size(22.dp))
                    }
                }
            }

            Text(
                text       = stringResource(R.string.invite_message),
                fontSize   = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign  = TextAlign.Center,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text       = "„${list.name}“",
                fontSize   = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign  = TextAlign.Center,
                color      = color
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text      = stringResource(R.string.invite_permissions),
                fontSize  = 14.sp,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(28.dp))

            // Listen-Summary-Karte
            Surface(
                shape    = RoundedCornerShape(18.dp),
                color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier         = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                            .background(color.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Group, null, tint = color, modifier = Modifier.size(22.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(list.name, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.members_count, list.memberIds.size),
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Aktionen
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick  = { haptic.click(); onAccept() },
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(stringResource(R.string.action_accept), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            TextButton(
                onClick  = { haptic.tick(); onDecline() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_decline), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}