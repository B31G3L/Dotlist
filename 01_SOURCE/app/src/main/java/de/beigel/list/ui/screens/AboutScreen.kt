package de.beigel.list.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.beigel.list.onboarding.OnboardingManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val onboardingManager = remember { OnboardingManager(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "DailyList",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Deine tägliche Aufgabenverwaltung",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        // App-Information Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "App-Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                InfoRow(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = "1.0.0"
                )

                InfoRow(
                    icon = Icons.Default.Build,
                    title = "Entwickelt mit",
                    subtitle = "Jetpack Compose & Material 3"
                )

                InfoRow(
                    icon = Icons.Default.Storage,
                    title = "Datenspeicherung",
                    subtitle = "Lokal auf dem Gerät (Room Database)"
                )

                InfoRow(
                    icon = Icons.Default.Security,
                    title = "Datenschutz",
                    subtitle = "Alle Daten bleiben auf deinem Gerät"
                )
            }
        }

        // Hilfe & Einführung Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Hilfe & Einführung",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                SettingsClickable(
                    title = "Onboarding erneut anzeigen",
                    subtitle = "Die Einführung zur App noch einmal durchgehen",
                    onClick = {
                        onboardingManager.resetOnboarding()
                        // Restart activity to show onboarding
                        (context as? android.app.Activity)?.recreate()
                    }
                )

                InfoRow(
                    icon = Icons.Default.School,
                    title = "Erste Schritte",
                    subtitle = "Lerne alle Features der App kennen"
                )
            }
        }

        // Footer
        Text(
            text = "Made with ❤️ for productivity",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
