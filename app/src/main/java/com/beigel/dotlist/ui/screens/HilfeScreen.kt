package com.beigel.dotlist.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beigel.dotlist.R
import com.beigel.dotlist.utils.HapticFeedback

private data class FaqEntry(val question: String, val answer: String)

@Composable
private fun faqEntries(): List<FaqEntry> = listOf(
    FaqEntry(stringResource(R.string.faq_q1), stringResource(R.string.faq_a1)),
    FaqEntry(stringResource(R.string.faq_q2), stringResource(R.string.faq_a2)),
    FaqEntry(stringResource(R.string.faq_q3), stringResource(R.string.faq_a3)),
    FaqEntry(stringResource(R.string.faq_q4), stringResource(R.string.faq_a4)),
)

@Composable
fun HilfeScreen(
    haptic : HapticFeedback,
    onBack : () -> Unit,
) {
    val context = LocalContext.current
    var expandedIndex by remember { mutableStateOf<Int?>(null) }
    val faqEntries = faqEntries()

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = { haptic.tick(); onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(stringResource(R.string.title_help), fontSize = 20.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface)
        }

        Column(modifier = Modifier.padding(horizontal = 22.dp)) {
            Text(
                stringResource(R.string.section_faq), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))

            faqEntries.forEachIndexed { index, entry ->
                val isExpanded = expandedIndex == index
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape    = RoundedCornerShape(14.dp),
                    color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Column(
                        modifier = Modifier
                            .clickable { haptic.tick(); expandedIndex = if (isExpanded) null else index }
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                entry.question, fontSize = 14.5.sp, fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f)
                            )
                            Icon(
                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null, tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isExpanded) {
                            Text(
                                entry.answer, fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                stringResource(R.string.section_feedback), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))

            val feedbackEmail = stringResource(R.string.feedback_email)
            val feedbackSubject = stringResource(R.string.feedback_email_subject)
            OutlinedButton(
                onClick = {
                    haptic.tick()
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(feedbackEmail))
                        putExtra(Intent.EXTRA_SUBJECT, feedbackSubject)
                    }
                    runCatching { context.startActivity(intent) }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.MailOutline, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_send_feedback_email))
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}