package de.beigel.list.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.beigel.list.data.TaskContext
import de.beigel.list.data.EnergyLevel
import de.beigel.list.settings.SettingsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val pagerState = rememberPagerState(pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()

    // Onboarding state
    var userName by remember { mutableStateOf("") }
    var selectedWorkStyle by remember { mutableStateOf<WorkStyle?>(null) }
    var selectedContexts by remember { mutableStateOf<Set<TaskContext>>(emptySet()) }
    var workingHours by remember { mutableStateOf(9 to 17) }
    var energyPeaks by remember { mutableStateOf<Set<Int>>(setOf(9, 10, 15, 16)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Progress Indicator
        OnboardingProgressIndicator(
            currentPage = pagerState.currentPage,
            totalPages = pagerState.pageCount
        )

        // Content Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> WorkStylePage(
                    selectedWorkStyle = selectedWorkStyle,
                    onWorkStyleSelected = { selectedWorkStyle = it }
                )
                2 -> ContextPreferencePage(
                    selectedContexts = selectedContexts,
                    onContextsChanged = { selectedContexts = it }
                )
                3 -> SchedulePage(
                    workingHours = workingHours,
                    energyPeaks = energyPeaks,
                    onWorkingHoursChanged = { workingHours = it },
                    onEnergyPeaksChanged = { energyPeaks = it }
                )
                4 -> CompletePage(
                    userName = userName,
                    onUserNameChanged = { userName = it }
                )
            }
        }

        // Navigation Buttons
        OnboardingNavigation(
            currentPage = pagerState.currentPage,
            totalPages = pagerState.pageCount,
            canProceed = when (pagerState.currentPage) {
                1 -> selectedWorkStyle != null
                2 -> selectedContexts.isNotEmpty()
                else -> true
            },
            onNext = {
                if (pagerState.currentPage == pagerState.pageCount - 1) {
                    // Save onboarding data
                    saveOnboardingData(
                        settingsManager,
                        userName,
                        selectedWorkStyle,
                        selectedContexts,
                        workingHours,
                        energyPeaks
                    )
                    onComplete()
                } else {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            },
            onPrevious = {
                if (pagerState.currentPage > 0) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                }
            },
            onSkip = {
                saveOnboardingData(
                    settingsManager,
                    userName,
                    selectedWorkStyle,
                    selectedContexts,
                    workingHours,
                    energyPeaks
                )
                onComplete()
            }
        )
    }
}

@Composable
fun OnboardingProgressIndicator(
    currentPage: Int,
    totalPages: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(totalPages) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    .animateContentSize()
            )

            if (index < totalPages - 1) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
fun WelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Icon/Logo
        Card(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎯",
                    style = MaterialTheme.typography.displayLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Willkommen bei Daily List!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Die intelligente To-Do App, die sich an deinen Arbeitsrhythmus anpasst",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "✨ Was macht Daily List besonders?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(12.dp))

                FeatureItem(
                    icon = "🎯",
                    text = "Smart Focus zeigt dir immer die 5 wichtigsten Aufgaben"
                )

                FeatureItem(
                    icon = "🧠",
                    text = "Intelligente Priorisierung basierend auf Deadlines und Kontext"
                )

                FeatureItem(
                    icon = "⚡",
                    text = "Quick Wins für produktive Pausen"
                )

                FeatureItem(
                    icon = "📊",
                    text = "Produktivitäts-Tracking und Motivation"
                )
            }
        }
    }
}

@Composable
fun FeatureItem(icon: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun WorkStylePage(
    selectedWorkStyle: WorkStyle?,
    onWorkStyleSelected: (WorkStyle) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Wie arbeitest du am liebsten?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Das hilft uns, die App an deinen Arbeitsrhythmus anzupassen",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        WorkStyle.values().forEach { workStyle ->
            WorkStyleCard(
                workStyle = workStyle,
                isSelected = selectedWorkStyle == workStyle,
                onSelect = { onWorkStyleSelected(workStyle) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun WorkStyleCard(
    workStyle: WorkStyle,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelect
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = workStyle.icon,
                style = MaterialTheme.typography.displaySmall
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workStyle.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = workStyle.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Ausgewählt",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ContextPreferencePage(
    selectedContexts: Set<TaskContext>,
    onContextsChanged: (Set<TaskContext>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "In welchen Bereichen arbeitest du?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Wähle die Arbeitsbereiche aus, die für dich relevant sind (mindestens einen)",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        TaskContext.values().chunked(2).forEach { contextPair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                contextPair.forEach { context ->
                    ContextCard(
                        context = context,
                        isSelected = context in selectedContexts,
                        onToggle = {
                            if (context in selectedContexts) {
                                onContextsChanged(selectedContexts - context)
                            } else {
                                onContextsChanged(selectedContexts + context)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Fill remaining space if odd number
                if (contextPair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ContextCard(
    context: TaskContext,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = context.icon,
                style = MaterialTheme.typography.displayMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = context.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SchedulePage(
    workingHours: Pair<Int, Int>,
    energyPeaks: Set<Int>,
    onWorkingHoursChanged: (Pair<Int, Int>) -> Unit,
    onEnergyPeaksChanged: (Set<Int>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Wann bist du am produktivsten?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Diese Informationen helfen bei der intelligenten Aufgabenplanung",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Working Hours Section
        Card {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "⏰ Arbeitszeiten",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Von:")

                    TimeSelector(
                        selectedHour = workingHours.first,
                        onHourSelected = { hour ->
                            onWorkingHoursChanged(hour to workingHours.second)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bis:")

                    TimeSelector(
                        selectedHour = workingHours.second,
                        onHourSelected = { hour ->
                            onWorkingHoursChanged(workingHours.first to hour)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Energy Peaks Section
        Card {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "⚡ Energie-Hochzeiten",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Wann hast du die meiste Energie für schwierige Aufgaben?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                EnergyPeakSelector(
                    selectedHours = energyPeaks,
                    onSelectionChanged = onEnergyPeaksChanged
                )
            }
        }
    }
}

@Composable
fun TimeSelector(
    selectedHour: Int,
    onHourSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = String.format("%02d:00", selectedHour),
            onValueChange = { },
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier.menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            (6..22).forEach { hour ->
                DropdownMenuItem(
                    text = { Text(String.format("%02d:00", hour)) },
                    onClick = {
                        onHourSelected(hour)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun EnergyPeakSelector(
    selectedHours: Set<Int>,
    onSelectionChanged: (Set<Int>) -> Unit
) {
    val timeSlots = listOf(
        6 to "Früh morgens",
        9 to "Vormittag",
        12 to "Mittag",
        15 to "Nachmittag",
        18 to "Abend",
        21 to "Spät abends"
    )

    timeSlots.forEach { (hour, label) ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (hour in selectedHours) {
                        onSelectionChanged(selectedHours - hour)
                    } else {
                        onSelectionChanged(selectedHours + hour)
                    }
                }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = hour in selectedHours,
                onCheckedChange = { checked ->
                    if (checked) {
                        onSelectionChanged(selectedHours + hour)
                    } else {
                        onSelectionChanged(selectedHours - hour)
                    }
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${hour}:00 Uhr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun CompletePage(
    userName: String,
    onUserNameChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎉",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Perfekt! Du bist startklar!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Daily List ist jetzt an deine Arbeitsweise angepasst. Du kannst alle Einstellungen später in den Settings ändern.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = userName,
            onValueChange = onUserNameChanged,
            label = { Text("Dein Name (optional)") },
            placeholder = { Text("z.B. Alex") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "🚀 Bereit für maximale Produktivität!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Smart Focus wird dir automatisch die wichtigsten Aufgaben anzeigen. Starte mit dem + Button deine erste Aufgabe!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun OnboardingNavigation(
    currentPage: Int,
    totalPages: Int,
    canProceed: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back/Skip Button
            if (currentPage == 0) {
                TextButton(onClick = onSkip) {
                    Text("Überspringen")
                }
            } else {
                TextButton(onClick = onPrevious) {
                    Text("Zurück")
                }
            }

            // Next/Finish Button
            Button(
                onClick = onNext,
                enabled = canProceed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    if (currentPage == totalPages - 1) "Los geht's!" else "Weiter"
                )
            }
        }
    }
}

// Work Style Enum
enum class WorkStyle(
    val title: String,
    val description: String,
    val icon: String,
    val defaultTaskDuration: Int,
    val preferredEnergyLevel: EnergyLevel
) {
    FOCUSED(
        "Deep Work",
        "Ich arbeite gerne in langen, fokussierten Blöcken ohne Unterbrechungen",
        "🎯",
        90,
        EnergyLevel.HIGH
    ),
    FLEXIBLE(
        "Flexibel",
        "Ich wechsle gerne zwischen verschiedenen Aufgaben und passe mich an",
        "🔄",
        45,
        EnergyLevel.MEDIUM
    ),
    STRUCTURED(
        "Strukturiert",
        "Ich bevorzuge feste Routinen und gut geplante Arbeitsblöcke",
        "📋",
        60,
        EnergyLevel.MEDIUM
    ),
    CREATIVE(
        "Kreativ",
        "Ich arbeite am besten, wenn ich kreativ und spontan sein kann",
        "🎨",
        30,
        EnergyLevel.HIGH
    ),
    EFFICIENT(
        "Effizient",
        "Ich liebe Quick Wins und möchte möglichst viel in kurzer Zeit schaffen",
        "⚡",
        20,
        EnergyLevel.LOW
    )
}

// Helper function to save onboarding data
private fun saveOnboardingData(
    settingsManager: SettingsManager,
    userName: String,
    workStyle: WorkStyle?,
    contexts: Set<TaskContext>,
    workingHours: Pair<Int, Int>,
    energyPeaks: Set<Int>
) {
    // Save basic settings
    settingsManager.hasCompletedOnboarding = true

    // Save work style preferences
    workStyle?.let { style ->
        settingsManager.defaultTaskDuration = style.defaultTaskDuration
        settingsManager.defaultEnergyLevel = style.preferredEnergyLevel
    }

    // Save context preferences
    if (contexts.isNotEmpty()) {
        settingsManager.setPreferredContexts(contexts.toList())
    }

    // Save working hours
    settingsManager.setWorkingHours(workingHours.first, workingHours.second)

    // Save energy peaks
    settingsManager.setProductivityPeaks(energyPeaks.toList())

    // Enable smart features by default
    settingsManager.useSmartFocus = true
    settingsManager.enableSmartScoring = true
    settingsManager.enableSmartSuggestions = true
    settingsManager.showMotivationalMessages = true
    settingsManager.productivityTrackingEnabled = true
}