package de.beigel.anschaffungsliste

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import de.beigel.anschaffungsliste.data.Anschaffung
import de.beigel.anschaffungsliste.repository.AnschaffungRepository
import de.beigel.anschaffungsliste.ui.AnschaffungViewModel
import de.beigel.anschaffungsliste.ui.AnschaffungViewModelFactory
import de.beigel.anschaffungsliste.ui.theme.AnschaffungslisteTheme
import org.burnoutcrew.reorderable.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = AnschaffungRepository(this)

        setContent {
            AnschaffungslisteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AnschaffungslisteScreen(repository)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnschaffungslisteScreen(
    repository: AnschaffungRepository,
    viewModel: AnschaffungViewModel = viewModel(
        factory = AnschaffungViewModelFactory(repository)
    )
) {
    val anschaffungen by viewModel.anschaffungen.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()
    val context = LocalContext.current

    // Reorderable State für Drag & Drop
    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to ->
            // Temporäre Neuordnung für UI
        },
        onDragEnd = { fromIndex, toIndex ->
            // Permanente Neuordnung
            val newList = anschaffungen.toMutableList()
            val item = newList.removeAt(fromIndex)
            newList.add(toIndex, item)
            viewModel.updatePrioritaeten(newList)
        }
    )

    Column {
        // Top App Bar
        TopAppBar(
            title = { Text("Anschaffungsliste") },
            actions = {
                IconButton(onClick = { viewModel.showAddDialog() }) {
                    Icon(Icons.Default.Add, contentDescription = "Hinzufügen")
                }
            }
        )

        // Loading Indicator
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Liste der Anschaffungen
        LazyColumn(
            state = reorderableState.listState,
            modifier = Modifier
                .fillMaxSize()
                .reorderable(reorderableState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(anschaffungen, key = { it.id }) { anschaffung ->
                ReorderableItem(
                    reorderableState = reorderableState,
                    key = anschaffung.id
                ) { isDragging ->
                    AnschaffungCard(
                        anschaffung = anschaffung,
                        isDragging = isDragging,
                        onEdit = { viewModel.showEditDialog(anschaffung) },
                        onDelete = { viewModel.deleteAnschaffung(anschaffung) },
                        onToggleErledigt = { viewModel.toggleErledigt(anschaffung) },
                        onOpenLink = { link ->
                            if (link.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                                context.startActivity(intent)
                            }
                        },
                        reorderableState = reorderableState
                    )
                }
            }
        }
    }

    // Dialog für Hinzufügen/Bearbeiten
    if (showDialog) {
        AnschaffungDialog(
            name = viewModel.dialogName,
            preis = viewModel.dialogPreis,
            link = viewModel.dialogLink,
            onNameChange = viewModel::updateDialogName,
            onPreisChange = viewModel::updateDialogPreis,
            onLinkChange = viewModel::updateDialogLink,
            onSave = viewModel::saveAnschaffung,
            onDismiss = viewModel::hideDialog
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnschaffungCard(
    anschaffung: Anschaffung,
    isDragging: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleErledigt: () -> Unit,
    onOpenLink: (String) -> Unit,
    reorderableState: ReorderableLazyListState
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .detectReorderAfterLongPress(reorderableState),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = anschaffung.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (anschaffung.erledigt) TextDecoration.LineThrough else null
                    )
                    Text(
                        text = anschaffung.formatierterPreis(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    if (anschaffung.link.isNotBlank()) {
                        TextButton(
                            onClick = { onOpenLink(anschaffung.link) },
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = anschaffung.kurzerLink(),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Drag Handle
                Icon(
                    Icons.Default.Menu,
                    contentDescription = "Verschieben",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleErledigt) {
                    Icon(
                        if (anschaffung.erledigt) Icons.Default.CheckCircle else Icons.Default.Check,
                        contentDescription = if (anschaffung.erledigt) "Als offen markieren" else "Als erledigt markieren",
                        tint = if (anschaffung.erledigt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Bearbeiten")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Löschen",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun AnschaffungDialog(
    name: String,
    preis: String,
    link: String,
    onNameChange: (String) -> Unit,
    onPreisChange: (String) -> Unit,
    onLinkChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (name.isBlank()) "Neue Anschaffung" else "Anschaffung bearbeiten",
                    style = MaterialTheme.typography.headlineSmall
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = preis,
                    onValueChange = onPreisChange,
                    label = { Text("Preis (€)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                OutlinedTextField(
                    value = link,
                    onValueChange = onLinkChange,
                    label = { Text("Link (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Abbrechen")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onSave,
                        enabled = name.isNotBlank()
                    ) {
                        Text("Speichern")
                    }
                }
            }
        }
    }
}