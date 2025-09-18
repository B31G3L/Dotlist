// MainActivity.kt - Korrigierte Version

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
import de.beigel.list.repository.TaskRepository
import de.beigel.list.ui.screens.TaskListScreen
import de.beigel.list.ui.screens.HistoryScreen
import de.beigel.list.ui.screens.SettingsScreen
import de.beigel.list.ui.theme.DailyListTheme
import de.beigel.list.viewmodel.TaskViewModel
import de.beigel.list.settings.SettingsManager
import de.beigel.list.widget.WidgetManager
import de.beigel.list.viewmodel.DialogState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize database and repository
        val database = TaskDatabase.getDatabase(this)
        val repository = TaskRepository(database.taskDao())
        val settingsManager = SettingsManager(this)

        // Handle widget intents
        handleWidgetIntent(intent)

        setContent {
            // Theme State für Live-Updates
            var useSystemTheme by remember { mutableStateOf(settingsManager.useSystemTheme) }
            var isDarkMode by remember { mutableStateOf(settingsManager.isDarkMode) }

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
                    val navController = rememberNavController()
                    val viewModel: TaskViewModel = viewModel {
                        TaskViewModel(repository)
                    }

                    // Apply saved interaction mode
                    LaunchedEffect(Unit) {
                        viewModel.setInteractionMode(settingsManager.interactionMode)

                        // Initialize widgets
                        WidgetManager.initializeWidgets(this@MainActivity)
                    }

                    // Handle widget-triggered actions
                    LaunchedEffect(intent) {
                        handleWidgetActions(intent, viewModel)
                    }

                    // Standard Navigation mit einfachen Animationen
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
                                onThemeChange = onThemeChange
                            )
                        }
                    }
                }
            }
        }
    }

    // Korrigierte onNewIntent Methode
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Update widgets when app becomes visible
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
                    // Dies wird in handleWidgetActions behandelt
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