@file:OptIn(ExperimentalLayoutApi::class)

package de.beigel.todo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.beigel.todo.ui.theme.ListColors

private val iconOptions: List<ImageVector> = listOf(
    Icons.Default.Work, Icons.Default.Home, Icons.Default.ShoppingCart, Icons.Default.Favorite,
    Icons.Default.School, Icons.Default.FitnessCenter, Icons.Default.Flight, Icons.Default.Star,
)

@Composable
fun ListeErstellenScreen(
    onBack  : () -> Unit,
    onCreate: (name: String, color: String) -> Unit,
) {
    var name          by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(ListColors.first()) }
    var selectedIcon  by remember { mutableIntStateOf(0) }
    var shareEnabled  by remember { mutableStateOf(false) }

    val previewColor = listColor(selectedColor)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        // App-Bar
        Row(
            modifier          = Modifier.fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("Neue Liste", fontSize = 20.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface)
        }

        // Name-Feld
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon-Preview
            Box(
                modifier         = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                    .background(previewColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconOptions[selectedIcon], null,
                    tint = previewColor, modifier = Modifier.size(26.dp))
            }
            // Text-Feld
            Column(modifier = Modifier.weight(1f)) {
                TextField(
                    value         = name,
                    onValueChange = { name = it },
                    placeholder   = { Text("Listenname",
                        color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors        = TextFieldDefaults.colors(
                        focusedContainerColor   = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor   = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        }

        // Farbe
        SectionLabel("Farbe")
        Row(
            modifier              = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ListColors.forEach { hex ->
                val c      = listColor(hex)
                val active = hex == selectedColor
                Box(
                    modifier         = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(c)
                        .then(if (active) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), CircleShape) else Modifier)
                        .clickable { selectedColor = hex },
                    contentAlignment = Alignment.Center
                ) {
                    if (active) {
                        Icon(Icons.Default.Check, null,
                            tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Symbol
        SectionLabel("Symbol", modifier = Modifier.padding(top = 12.dp))
        FlowRow(
            modifier              = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement   = Arrangement.spacedBy(12.dp)
        ) {
            iconOptions.forEachIndexed { i, icon ->
                val active = i == selectedIcon
                Box(
                    modifier         = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(if (active) previewColor.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .then(if (active) Modifier.border(2.dp, previewColor, RoundedCornerShape(15.dp)) else Modifier)
                        .clickable { selectedIcon = i },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null,
                        tint     = if (active) previewColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(23.dp))
                }
            }
        }

        // Teilen-Row
        Spacer(Modifier.height(28.dp))
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
                    Icon(Icons.Default.PersonAdd, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mit anderen teilen", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Per E-Mail oder Link einladen", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                }
                Switch(checked = shareEnabled, onCheckedChange = { shareEnabled = it })
            }
        }

        // Erstellen-Button
        Spacer(Modifier.height(30.dp))
        Button(
            onClick  = { if (name.isNotBlank()) onCreate(name, selectedColor) },
            enabled  = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(54.dp),
            shape    = RoundedCornerShape(16.dp)
        ) {
            Text("Liste erstellen", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(32.dp))
    }
}
