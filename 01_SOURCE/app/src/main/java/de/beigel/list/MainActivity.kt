package de.beigel.list

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import de.beigel.list.data.TaskDatabase
import de.beigel.list.onboarding.OnboardingManager
import de.beigel.list.repository.TaskRepository
import de.beigel.list.ui.screens.*
import de.beigel.list.ui.theme.DailyListTheme
import de.beigel.list.viewmodel.TaskViewModel
import de.beigel.list.settings.SettingsManager
import de.beigel.list.widget.WidgetManager
import de.beigel.list.viewmodel.DialogState

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

        // Handle widget intents
        handleWidgetIntent(intent)

        setContent {
            // Theme State für Live-Updates
            var useSystemTheme by remember { mutableStateOf(settingsManager.useSystemTheme) }
            var isDarkMode by remember { mutableStateOf(settingsManager.isDarkMode) }
            var showOnboarding by remember { mutableStateOf(!onboardingManager.isOnboardingCompleted) }

            // Theme Callback für Settings
            val onThemeChange: (Boolean, Boolean) -> Unit = { systemTheme, darkMode ->
                useSystemTheme = systemTheme
                isDarkMode = darkMode
            }

            DailyListTheme(
                darkTheme = if (useSystemTheme) {
                    androidx.compose.foundation.isSystemInDarkTheme()
                } else {
                    isDarkMode
                }
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
                                intent = intent
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MainAppContent(
        repository: TaskRepository,
        settingsManager: SettingsManager,
        onThemeChange: (Boolean, Boolean) -> Unit,
        intent: Intent?
    ) {
        val navController = rememberNavController()
        val viewModel: TaskViewModel = viewModel {
            TaskViewModel(repository)
        }

        // Apply saved interaction mode
        LaunchedEffect(Unit) {
            viewModel.setInteractionMode(settingsManager.interactionMode)
            WidgetManager.initializeWidgets(this@MainActivity)
        }

        // Handle widget-triggered actions
        LaunchedEffect(intent) {
            handleWidgetActions(intent, viewModel)
        }

        // Navigation mit Animationen
        NavHost(
            navController = navController,
            startDestination = "task_list",
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            composable("task_list") {
                TaskListScreen(
                    viewModel = viewModel,
                    onNavigateToHistory = {
                        navController.navigate("history") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings") {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable("history") {
                HistoryScreen(
                    repository = repository,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onThemeChange = onThemeChange,
                    onShowOnboarding = {
                        // Callback für "Onboarding erneut anzeigen" in den Einstellungen
                        onboardingManager.resetOnboarding()
                        recreate() // Restart activity to show onboarding
                    }
                )
            }
        }
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