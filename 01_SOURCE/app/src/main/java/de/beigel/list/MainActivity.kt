package de.beigel.list

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("📊 Produktivitäts-Analyse")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            insights?.let { data ->
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            de.beigel.list.ui.components.TaskStatCard(
                                title = "Streak",
                                value = "${data.currentStreak}",
                                subtitle = "Tage am Stück",
                                icon = "🔥",
                                modifier = Modifier.weight(1f)
                            )

                            de.beigel.list.ui.components.TaskStatCard(
                                title = "Gesamt",
                                value = "${data.totalCompletedTasks}",
                                subtitle = "Aufgaben erledigt",
                                icon = "✅",
                                modifier = Modifier.weight(1f)
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
                            Card {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "🏆 Bester Tag",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "${bestDay.date}: ${bestDay.completedTasks} Aufgaben erledigt",
                                        style = MaterialTheme.typography.bodyLarge
                                    )

                                    Text(
                                        text = "${bestDay.getCompletionPercentage()}% Completion Rate",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Weekly Breakdown
                    item {
                        Card {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "📈 Wöchentlicher Verlauf",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                data.dailyBreakdown.takeLast(7).forEach { day ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = day.date,
                                            style = MaterialTheme.typography.bodyMedium
                                        )

                                        Text(
                                            text = "${day.completedTasks}/${day.totalTasks}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
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