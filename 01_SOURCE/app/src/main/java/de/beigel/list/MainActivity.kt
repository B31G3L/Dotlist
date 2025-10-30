package de.beigel.list

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import de.beigel.list.data.TaskDatabase
import de.beigel.list.onboarding.OnboardingManager
import de.beigel.list.repository.TaskRepository
import de.beigel.list.ui.screens.*
import de.beigel.list.ui.theme.DailyListTheme
import de.beigel.list.ui.theme.CustomTheme
import de.beigel.list.viewmodel.TaskViewModel
import de.beigel.list.settings.SettingsManager
import de.beigel.list.utils.TestDataGenerator
import de.beigel.list.widget.WidgetManager
import de.beigel.list.viewmodel.DialogState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var onboardingManager: OnboardingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize managers
        onboardingManager = OnboardingManager(this)
        val database = TaskDatabase.getDatabase(this)
        val repository = TaskRepository(database.taskDao())
        val settingsManager = SettingsManager(this)
        loadTestData(repository)

        // Handle widget intents
        handleWidgetIntent(intent)

        setContent {
            // Theme States für Live-Updates
            var useSystemTheme by remember { mutableStateOf(settingsManager.useSystemTheme) }
            var isDarkMode by remember { mutableStateOf(settingsManager.isDarkMode) }
            var customTheme by remember { mutableStateOf(settingsManager.getCustomTheme()) }
            var showOnboarding by remember { mutableStateOf(!onboardingManager.isOnboardingCompleted) }

            // Theme Callback für Settings
            val onThemeChange: (Boolean, Boolean, CustomTheme?) -> Unit = { systemTheme, darkMode, newCustomTheme ->
                useSystemTheme = systemTheme
                isDarkMode = darkMode
                newCustomTheme?.let { customTheme = it }
            }

            DailyListTheme(
                darkTheme = if (useSystemTheme) {
                    isSystemInDarkTheme()
                } else {
                    isDarkMode
                },
                customTheme = customTheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Onboarding oder Main App
                    AnimatedContent(
                        targetState = showOnboarding,
                        transitionSpec = {
                            if (targetState) {
                                slideInHorizontally(
                                    initialOffsetX = { -it },
                                    animationSpec = tween(700, easing = EaseInOutCubic)
                                ) + fadeIn(animationSpec = tween(700))
                            } else {
                                slideInHorizontally(
                                    initialOffsetX = { it },
                                    animationSpec = tween(700, easing = EaseInOutCubic)
                                ) + fadeIn(animationSpec = tween(700))
                            } togetherWith slideOutHorizontally(
                                targetOffsetX = { if (targetState) it else -it },
                                animationSpec = tween(700, easing = EaseInOutCubic)
                            ) + fadeOut(animationSpec = tween(700))
                        },
                        label = "onboarding_transition"
                    ) { shouldShowOnboarding ->
                        if (shouldShowOnboarding) {
                            OnboardingScreen(
                                onFinish = {
                                    onboardingManager.completeOnboarding()
                                    showOnboarding = false
                                }
                            )
                        } else {
                            MainAppContent(
                                repository = repository,
                                settingsManager = settingsManager,
                                onThemeChange = onThemeChange,
                                onShowOnboarding = {
                                    onboardingManager.resetOnboarding()
                                    showOnboarding = true
                                },
                                intent = intent
                            )
                        }
                    }
                }
            }
        }
    }

    private fun loadTestData(repository: TaskRepository) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Prüfen ob schon Daten vorhanden sind
                val existingTasks = repository.getDailyTasksForToday().first()

                if (existingTasks.isEmpty()) {
                    // Nur laden wenn noch keine Daten da sind
                    TestDataGenerator.generateAllTestData(repository)
                    android.util.Log.d("MainActivity", "Testdaten erfolgreich geladen")
                } else {
                    android.util.Log.d("MainActivity", "Testdaten bereits vorhanden")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Fehler beim Laden der Testdaten", e)
            }
        }
    }

    @Composable
    private fun MainAppContent(
        repository: TaskRepository,
        settingsManager: SettingsManager,
        onThemeChange: (Boolean, Boolean, CustomTheme?) -> Unit,
        onShowOnboarding: () -> Unit,
        intent: Intent?
    ) {
        val viewModel: TaskViewModel = viewModel {
            TaskViewModel(repository)
        }

        // Navigation State
        var currentDestination by remember { mutableStateOf(NavigationDestination.TODAY) }

        // Apply saved interaction mode
        LaunchedEffect(Unit) {
            viewModel.setInteractionMode(settingsManager.interactionMode)
            WidgetManager.initializeWidgets(this@MainActivity)
        }

        // Handle widget-triggered actions
        LaunchedEffect(intent) {
            handleWidgetActions(intent, viewModel)
        }

        // Main Content mit Bottom Navigation
        TaskListScreen(
            viewModel = viewModel,
            currentDestination = currentDestination,
            onNavigationChange = { destination ->
                currentDestination = destination
                // Clear selection when changing destinations
                viewModel.clearSelection()
            }
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        WidgetManager.updateAllWidgets(this)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        intent?.let {
            when (it.getStringExtra("widget_action")) {
                "open_app" -> {
                    // App wurde durch Widget geöffnet - keine weitere Aktion nötig
                }
                "add_task" -> {
                    // Widget möchte Add-Task Dialog öffnen
                }
            }
        }
    }

    private fun handleWidgetActions(intent: Intent?, viewModel: TaskViewModel) {
        intent?.let {
            when (it.getStringExtra("widget_action")) {
                "add_task" -> {
                    val addToDaily = it.getBooleanExtra("add_to_daily", true)
                    viewModel.setDialogState(DialogState.AddTask(addToDaily))
                }
            }
        }
    }
}