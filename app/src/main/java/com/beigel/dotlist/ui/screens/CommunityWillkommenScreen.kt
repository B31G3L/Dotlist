package com.beigel.dotlist.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beigel.dotlist.R
import com.beigel.dotlist.utils.HapticFeedback

/**
 * Zweiter Onboarding-Screen, direkt nach der Namenseingabe im WillkommenScreen.
 * Wird nur einmal gezeigt (siehe DeviceIdManager.isCommunityScreenShown) und
 * stellt kurz den Discord-Server vor. "Beitreten" öffnet Discord, "Vielleicht
 * später" überspringt – beides führt danach direkt in die App.
 */
@Composable
fun CommunityWillkommenScreen(
    haptic : HapticFeedback,
    onDone : () -> Unit,
) {
    val context = LocalContext.current
    val discordUrl = stringResource(R.string.discord_invite_url)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Groups,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text       = stringResource(R.string.community_welcome_title),
                fontSize   = 26.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign  = TextAlign.Center,
                color      = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text      = stringResource(R.string.community_welcome_subtitle),
                fontSize  = 15.sp,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier  = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    haptic.click()
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(discordUrl))
                    runCatching { context.startActivity(intent) }
                    onDone()
                },
                shape    = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Groups, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_join_discord_now), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(12.dp))

            TextButton(
                onClick = {
                    haptic.tick()
                    onDone()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_maybe_later))
            }

            Spacer(Modifier.weight(1.4f))
        }
    }
}