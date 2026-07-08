package de.beigel.list.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.beigel.list.utils.HapticFeedback

private data class FaqEntry(val question: String, val answer: String)

private val faqEntries = listOf(
    FaqEntry(
        "Wie teile ich eine Liste?",
        "Öffne die Liste, tippe auf das Teilen-Icon und kopiere den Einladungscode. " +
                "Die andere Person fügt ihn beim Beitreten in \"Meine Listen\" ein."
    ),
    FaqEntry(
        "Was ist der Unterschied zwischen Besitzer, Admin und Bearbeiter?",
        "Der Besitzer hat eine Liste erstellt und kann sie löschen oder den Besitz übertragen. " +
                "Admins dürfen zusätzlich den Einladungscode weitergeben und Mitglieder verwalten. " +
                "Bearbeiter können Aufgaben ansehen und bearbeiten, aber keine Mitglieder verwalten."
    ),
    FaqEntry(
        "Warum verliere ich Zugriff, wenn ich die App neu installiere?",
        "Ohne ein verknüpftes Google-Konto ist dein Zugang nur auf diesem Gerät gespeichert. " +
                "Verknüpfe dein Google-Konto unter \"Konto verwalten\", um das zu vermeiden."
    ),
    FaqEntry(
        "Bekomme ich Benachrichtigungen, wenn die App geschlossen ist?",
        "Ja, solange die App im Hintergrund noch läuft. Bei vollständig beendeter App " +
                "können Benachrichtigungen verzögert oder gar nicht ankommen."
    ),
)

@Composable
fun HilfeScreen(
    haptic : HapticFeedback,
    onBack : () -> Unit,
) {
    val context = LocalContext.current
    var expandedIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = { haptic.tick(); onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("Hilfe & Feedback", fontSize = 20.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface)
        }

        Column(modifier = Modifier.padding(horizontal = 22.dp)) {
            Text(
                "HÄUFIGE FRAGEN", fontSize = 12.sp, fontWeight = FontWeight.Bold,
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
                "FEEDBACK", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    haptic.tick()
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("feedback@example.com"))
                        putExtra(Intent.EXTRA_SUBJECT, "Dotlist Feedback")
                    }
                    runCatching { context.startActivity(intent) }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.MailOutline, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Feedback per E-Mail senden")
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}