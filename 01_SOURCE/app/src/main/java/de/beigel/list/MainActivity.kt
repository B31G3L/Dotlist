package de.beigel.list

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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash Screen

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize database and repository
        val database = TaskDatabase.getDatabase(this)
        val repository = TaskRepository(database.taskDao())
        val settingsManager = SettingsManager(this)

        setContent {
            DailyListTheme(
                darkTheme = if (settingsManager.useSystemTheme) {
                    androidx.compose.foundation.isSystemInDarkTheme()
                } else {
                    settingsManager.isDarkMode
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
                    }

                    // Navigation with animations
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
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}