package de.beigel.list

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.beigel.list.data.TaskDatabase
import de.beigel.list.data.DatabaseUtils
import de.beigel.list.data.DatabaseMaintenanceWorker
import de.beigel.list.repository.TaskRepository
import de.beigel.list.repository.SmartFocusRepository
import de.beigel.list.ui.screens.SmartFocusScreen
import de.beigel.list.ui.screens.TaskListScreen
import de.beigel.list.ui.screens.HistoryScreen
import de.beigel.list.ui.screens.SettingsScreen
import de.beigel.list.ui.screens.OnboardingScreen
import de.beigel.list.ui.theme.DailyListTheme
import de.beigel.list.viewmodel.TaskViewModel
import de.beigel.list.viewmodel.TaskViewModelFactory
import de.beigel.list.viewmodel.SmartFocusViewModel
import de.beigel.list.viewmodel.SmartFocusViewModelFactory
import de.beigel.list.utils.TaskPriorityCalculator
import de.beigel.list.settings.SettingsManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var database: TaskDatabase
    private lateinit var taskRepository: TaskRepository
    private lateinit var smartFocusRepository: SmartFocusRepository
    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize components
        initializeComponents()

        // Setup background workers
        setupBackgroundTasks()

        setContent {
            DailyListTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DailyListApp()
                }
            }
        }
    }

    private fun initializeComponents() {
        // Initialize database
        database = TaskDatabase.getDatabase(this)

        // Initialize repositories
        taskRepository = TaskRepository(database.taskDao())
        smartFocusRepository = SmartFocusRepository(
            taskDao = database.taskDao(),
            calculator = TaskPriorityCalculator()
        )

        // Initialize settings
        settingsManager = SettingsManager(this)

        // Initialize database with default tasks if needed
        lifecycleScope.launch {
            try {
                DatabaseUtils.initializeDefaultTasks(database.taskDao())
            } catch (e: Exception) {
                // Handle initialization error
                android.util.Log.e("MainActivity", "Failed to initialize default tasks", e)
            }
        }
    }

    private fun setupBackgroundTasks() {
        // Schedule database maintenance
        DatabaseMaintenanceWorker.schedule(this)

        // Schedule notifications if enabled
        if (settingsManager.notificationsEnabled) {
            de.beigel.list.notification.NotificationWorker.scheduleDaily(
                this,
                settingsManager.notificationHour,
                settingsManager.notificationMinute
            )
        }

        // Schedule midnight reset
        de.beigel.list.service.MidnightResetWorker.scheduleInitialWork(this)
    }

    @Composable
    fun DailyListApp() {
        val navController = rememberNavController()
        var showOnboarding by remember { mutableStateOf(settingsManager.isFirstLaunch) }

        // ViewModels
        val taskViewModel: TaskViewModel = viewModel(
            factory = TaskViewModelFactory(taskRepository)
        )

        val smartFocusViewModel: SmartFocusViewModel = viewModel(
            factory = SmartFocusViewModelFactory(smartFocusRepository, taskRepository)
        )

        if (showOnboarding) {
            OnboardingScreen(
                onComplete = {
                    settingsManager.isFirstLaunch = false
                    showOnboarding = false
                }
            )
        } else {
            NavHost(
                navController = navController,
                startDestination = if (settingsManager.useSmartFocus) "smart_focus" else "task_list"
            ) {
                // Smart Focus Screen (neue Hauptansicht)
                composable("smart_focus") {
                    SmartFocusScreen(
                        viewModel = smartFocusViewModel,
                        onNavigateToAllTasks = {
                            navController.navigate("task_list")
                        },
                        onNavigateToHistory = {
                            navController.navigate("history")
                        },
                        onNavigateToSettings = {
                            navController.navigate("settings")
                        }
                    )
                }

                // Klassische Task List (für Nutzer die das bevorzugen)
                composable("task_list") {
                    TaskListScreen(
                        viewModel = taskViewModel,
                        onNavigateToHistory = {
                            navController.navigate("history")
                        },
                        onNavigateToSettings = {
                            navController.navigate("settings")
                        },
                        onNavigateToSmartFocus = {
                            navController.navigate("smart_focus")
                        }
                    )
                }

                // History Screen
                composable("history") {
                    HistoryScreen(
                        repository = taskRepository,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                // Settings Screen
                composable("settings") {
                    SettingsScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onToggleSmartFocus = { useSmartFocus ->
                            settingsManager.useSmartFocus = useSmartFocus
                            if (useSmartFocus) {
                                navController.navigate("smart_focus") {
                                    popUpTo("task_list") { inclusive = true }
                                }
                            } else {
                                navController.navigate("task_list") {
                                    popUpTo("smart_focus") { inclusive = true }
                                }
                            }
                        }
                    )
                }

                // Productivity Analytics Screen (neue Funktion)
                composable("analytics") {
                    ProductivityAnalyticsScreen(
                        smartFocusRepository = smartFocusRepository,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Update smart scores when app becomes active
        lifecycleScope.launch {
            try {
                smartFocusRepository.updateAllSmartScores()
            } catch (e: Exception) {
                // Handle error silently in background
                android.util.Log.w("MainActivity", "Failed to update smart scores", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Cleanup database connections
        try {
            database.close()
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Error closing database", e)
        }
    }
}

// Neue Productivity Analytics Screen
@Composable
fun ProductivityAnalyticsScreen(
    smartFocusRepository: SmartFocusRepository,
    onNavigateBack: () -> Unit
) {
    var insights by remember { mutableStateOf<de.beigel.list.repository.ProductivityInsights?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            insights = smartFocusRepository.getProductivityInsights(30) // 30 Tage
        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoading = false
        }
    }

    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    androidx.compose.material3.Text("📊 Produktivitäts-Analyse")
                },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onNavigateBack) {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        } else {
            insights?.let { data ->
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
                ) {
                    // Motivational Header
                    item {
                        de.beigel.list.ui.components.MotivationalBanner(
                            message = data.getMotivationalMessage(),
                            type = when {
                                data.currentStreak >= 7 -> de.beigel.list.ui.components.MotivationType.ACHIEVEMENT
                                data.totalCompletedTasks >= 20 -> de.beigel.list.ui.components.MotivationType.CELEBRATION
                                else -> de.beigel.list.ui.components.MotivationType.ENCOURAGEMENT
                            }
                        )
                    }

                    // Statistics Cards
                    item {
                        androidx.compose.foundation.layout.Row(
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                        ) {
                            de.beigel.list.ui.components.TaskStatCard(
                                title = "Streak",
                                value = "${data.currentStreak}",
                                subtitle = "Tage am Stück",
                                icon = "🔥",
                                modifier = androidx.compose.ui.Modifier.weight(1f)
                            )

                            de.beigel.list.ui.components.TaskStatCard(
                                title = "Gesamt",
                                value = "${data.totalCompletedTasks}",
                                subtitle = "Aufgaben erledigt",
                                icon = "✅",
                                modifier = androidx.compose.ui.Modifier.weight(1f)
                            )
                        }
                    }

                    // Progress Card
                    item {
                        de.beigel.list.ui.components.CircularProgressCard(
                            title = "Durchschnittliche Completion Rate",
                            progress = data.averageDailyCompletion.toFloat() / 10f, // Angenommen max 10 Tasks/Tag
                            progressText = "${data.averageDailyCompletion.toInt()}",
                            icon = "🎯"
                        )
                    }

                    // Best Day
                    data.bestDay?.let { bestDay ->
                        item {
                            androidx.compose.material3.Card {
                                androidx.compose.foundation.layout.Column(
                                    modifier = androidx.compose.ui.Modifier.padding(16.dp)
                                ) {
                                    androidx.compose.material3.Text(
                                        text = "🏆 Bester Tag",
                                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )

                                    androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(8.dp))

                                    androidx.compose.material3.Text(
                                        text = "${bestDay.date}: ${bestDay.completedTasks} Aufgaben erledigt",
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                                    )

                                    androidx.compose.material3.Text(
                                        text = "${bestDay.getCompletionPercentage()}% Completion Rate",
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Weekly Breakdown
                    item {
                        androidx.compose.material3.Card {
                            androidx.compose.foundation.layout.Column(
                                modifier = androidx.compose.ui.Modifier.padding(16.dp)
                            ) {
                                androidx.compose.material3.Text(
                                    text = "📈 Wöchentlicher Verlauf",
                                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )

                                androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(12.dp))

                                data.dailyBreakdown.takeLast(7).forEach { day ->
                                    androidx.compose.foundation.layout.Row(
                                        modifier = androidx.compose.ui.Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                                    ) {
                                        androidx.compose.material3.Text(
                                            text = day.date,
                                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                                        )

                                        androidx.compose.material3.Text(
                                            text = "${day.completedTasks}/${day.totalTasks}",
                                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}