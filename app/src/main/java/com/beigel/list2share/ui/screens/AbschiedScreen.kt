package com.beigel.list2share.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.beigel.list2share.R
import java.text.DateFormat
import java.util.Calendar

/**
 * Abschieds-Hinweis: die App wird aus dem Play Store genommen und das
 * Firebase-Backend ca. einen Monat später gelöscht.
 *
 * Wird in MainActivity bei jedem Start VOR der Firebase-Anmeldung gezeigt,
 * damit der Screen auch dann noch erscheint, wenn das Backend schon weg ist.
 */
object Abschied {
    /** Ab diesem Tag wird Firebase abgeschaltet. Bei Bedarf hier anpassen. */
    val firebaseLoeschDatum: Calendar = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.DECEMBER, 1)
    }

    /** true, sobald das Löschdatum erreicht ist – dann gibt es kein "Weiter" mehr. */
    fun istBackendWeg(): Boolean =
        !Calendar.getInstance().before(firebaseLoeschDatum)
}

@Composable
fun AbschiedScreen(
    onWeiter: (() -> Unit)?,
) {
    val context = LocalContext.current
    val datum = DateFormat.getDateInstance(DateFormat.LONG).format(Abschied.firebaseLoeschDatum.time)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(44.dp),
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.abschied_titel, stringResource(R.string.app_name)),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.abschied_intro, stringResource(R.string.app_name)),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(28.dp))

            AbschiedPunkt(
                icon = Icons.Filled.Storefront,
                titel = stringResource(R.string.abschied_store_titel),
                text = stringResource(R.string.abschied_store_text),
            )
            Spacer(Modifier.height(12.dp))
            AbschiedPunkt(
                icon = Icons.Filled.CloudOff,
                titel = stringResource(R.string.abschied_cloud_titel, datum),
                text = stringResource(R.string.abschied_cloud_text),
                hervorheben = true,
            )

            Spacer(Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.abschied_danke),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(32.dp))

            if (onWeiter != null) {
                Button(
                    onClick = onWeiter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(stringResource(R.string.abschied_weiter))
                }
                Spacer(Modifier.height(8.dp))
            } else {
                Text(
                    text = stringResource(R.string.abschied_beendet),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(16.dp))
            }

            TextButton(
                onClick = {
                    val mail = context.getString(R.string.feedback_email)
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$mail"))
                    runCatching { context.startActivity(intent) }
                },
            ) {
                Text(stringResource(R.string.abschied_kontakt))
            }
        }
    }
}

@Composable
private fun AbschiedPunkt(
    icon: ImageVector,
    titel: String,
    text: String,
    hervorheben: Boolean = false,
) {
    val container = if (hervorheben) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.surfaceVariant
    val inhalt    = if (hervorheben) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = inhalt,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = titel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = inhalt,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = inhalt,
            )
        }
    }
}
