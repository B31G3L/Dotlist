@file:OptIn(ExperimentalMaterial3Api::class)

package de.beigel.list.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.beigel.list.ui.theme.ALL_LIST_ICONS
import de.beigel.list.ui.theme.DEFAULT_LIST_ICON_NAME
import de.beigel.list.ui.theme.ICONS_BY_CATEGORY
import de.beigel.list.ui.theme.ListColors
import de.beigel.list.ui.theme.ListIconCategory
import de.beigel.list.ui.theme.listIconByName

@Composable
fun ListeErstellenScreen(
    onBack  : () -> Unit,
    onCreate: (name: String, color: String, icon: String) -> Unit,
) {
    var name          by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(ListColors.first()) }
    var selectedIcon  by remember { mutableStateOf(DEFAULT_LIST_ICON_NAME) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showIconSheet    by remember { mutableStateOf(false) }

    val previewColor = listColor(selectedColor)
    val previewIcon  = listIconByName(selectedIcon)

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

        // Name-Feld + Icon-Vorschau (öffnet die vollständige Symbolauswahl)
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier         = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                    .background(previewColor.copy(alpha = 0.16f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .clickable { showIconSheet = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(previewIcon, null, tint = previewColor, modifier = Modifier.size(26.dp))
            }
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
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            ListColors.forEach { hex ->
                val c      = listColor(hex)
                val active = hex == selectedColor
                Box(
                    modifier         = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(
                            if (active) 2.5.dp else 1.dp,
                            if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape
                        )
                        .clickable { selectedColor = hex },
                    contentAlignment = Alignment.Center
                ) {
                    if (active) {
                        Icon(Icons.Default.Check, null,
                            tint = Color.White, modifier = Modifier.size(15.dp))
                    }
                }
            }
            // "+"-Kachel: zeigt die aktuell gewählte Farbe und öffnet den eigenen Farbwähler
            Box(
                modifier         = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(previewColor)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .clickable { showColorPicker = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
        }

        // Symbol
        SectionLabel("Symbol", modifier = Modifier.padding(top = 20.dp))
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(previewColor.copy(alpha = 0.16f))
                    .border(1.5.dp, previewColor, RoundedCornerShape(15.dp))
                    .clickable { showIconSheet = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(previewIcon, null, tint = previewColor, modifier = Modifier.size(23.dp))
            }
            Text(
                text     = "Alle Symbole durchsuchen",
                fontSize = 14.sp,
                color    = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { showIconSheet = true }
            )
        }

        // Erstellen-Button
        Spacer(Modifier.height(30.dp))
        Button(
            onClick  = { if (name.isNotBlank()) onCreate(name, selectedColor, selectedIcon) },
            enabled  = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(54.dp),
            shape    = RoundedCornerShape(16.dp)
        ) {
            Text("Liste erstellen", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(32.dp))
    }

    // ── Icon-BottomSheet: Kategorien + vollständiges Raster ────────────────────
    if (showIconSheet) {
        val sheetState        = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var selectedCategory  by remember { mutableStateOf<ListIconCategory?>(null) }
        val visibleIcons      = remember(selectedCategory) {
            if (selectedCategory == null) ALL_LIST_ICONS
            else ICONS_BY_CATEGORY[selectedCategory].orEmpty()
        }
        ModalBottomSheet(
            onDismissRequest = { showIconSheet = false },
            sheetState       = sheetState,
            containerColor   = MaterialTheme.colorScheme.surface,
            shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Symbol wählen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick  = { selectedCategory = null },
                            label    = { Text("Alle") }
                        )
                    }
                    items(ListIconCategory.values().toList()) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick  = { selectedCategory = category },
                            label    = { Text(category.label) }
                        )
                    }
                }
                LazyVerticalGrid(
                    columns               = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier.fillMaxWidth().heightIn(max = 340.dp)
                ) {
                    items(visibleIcons) { option ->
                        val isSelected = selectedIcon == option.name
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .then(if (isSelected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)) else Modifier)
                                .clickable { selectedIcon = option.name; showIconSheet = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                option.vector, option.name,
                                tint     = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Eigene Farbe: RGB-Regler ────────────────────────────────────────────────
    if (showColorPicker) {
        val init = try { android.graphics.Color.parseColor(selectedColor) } catch (e: Exception) { android.graphics.Color.parseColor("#6750A4") }
        var r by remember { mutableStateOf(android.graphics.Color.red(init)) }
        var g by remember { mutableStateOf(android.graphics.Color.green(init)) }
        var b by remember { mutableStateOf(android.graphics.Color.blue(init)) }
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title   = { Text("Eigene Farbe") },
            text    = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(Color(android.graphics.Color.rgb(r, g, b)))
                    )
                    Text("Rot", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(value = r.toFloat(), onValueChange = { r = it.toInt() }, valueRange = 0f..255f)
                    Text("Grün", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(value = g.toFloat(), onValueChange = { g = it.toInt() }, valueRange = 0f..255f)
                    Text("Blau", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(value = b.toFloat(), onValueChange = { b = it.toInt() }, valueRange = 0f..255f)
                }
            },
            confirmButton = {
                Button(onClick = {
                    selectedColor = String.format("#%02X%02X%02X", r, g, b)
                    showColorPicker = false
                }) { Text("Übernehmen") }
            },
            dismissButton = { TextButton(onClick = { showColorPicker = false }) { Text("Abbrechen") } }
        )
    }
}