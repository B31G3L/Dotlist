package de.beigel.list

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import de.beigel.list.data.TaskDatabase
import de.beigel.list.repository.TaskRepository
import de.beigel.list.settings.SettingsManager
import de.beigel.list.service.NotificationWorker
import de.beigel.list.ui.screen.TaskScreen
import de.beigel.list.ui.theme.DailyListTheme
import de.beigel.list.utils.TaskPriorityCalculator
import de.beigel.list.viewmodel.TaskViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // ViewModels und Services
    private lateinit var taskViewModel: TaskViewModel
    private lateinit var settingsManager: SettingsManager
    private lateinit var repository: TaskRepository

    // Permission Launcher für Benachrichtigungen
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scheduleNotificationsIfEnabled()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Splash Screen
        val splashScreen = installSplashScreen()

        // Initialisiere Dependencies
        initializeDependencies()

        // Prüfe und fordere Berechtigungen an
        checkAndRequestPermissions()

        // Verarbeite Intent (falls von Benachrichtigung geöffnet)
        handleIntent()

        setContent {
            DailyListTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TaskScreen(viewModel = taskViewModel)
                }
            }
        }
    }

    private fun initializeDependencies() {
        // Initialisiere Database
        val database = TaskDatabase.getDatabase(this)

        // Initialisiere Settings Manager
        settingsManager = SettingsManager(this)

        // Initialisiere Repository
        val priorityCalculator = TaskPriorityCalculator()
        repository = TaskRepository(
            taskDao = database.taskDao(),
            settingsManager = settingsManager,
            priorityCalculator = priorityCalculator
        )

        // Initialisiere ViewModel
        taskViewModel = TaskViewModel(
            repository = repository,
            settingsManager = settingsManager,
            priorityCalculator = priorityCalculator
        )
    }

    private fun checkAndRequestPermissions() {
        // Prüfe Benachrichtigungsberechtigungen (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Berechtigung bereits erteilt
                    scheduleNotificationsIfEnabled()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Zeige Erklärung und fordere Berechtigung an
                    // TODO: Zeige Dialog mit Erklärung
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Fordere Berechtigung direkt an
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Für ältere Android-Versionen sind keine speziellen Berechtigungen erforderlich
            scheduleNotificationsIfEnabled()
        }
    }

    private fun scheduleNotificationsIfEnabled() {
        lifecycleScope.launch {
            val notificationsEnabled = settingsManager.notificationsEnabled.first()

            if (notificationsEnabled) {
                val hour = settingsManager.notificationHour.first()
                val minute = settingsManager.notificationMinute.first()

                NotificationWorker.scheduleDaily(this@MainActivity, hour, minute)
            }
        }
    }

    private fun handleIntent() {
        // Prüfe ob Activity von Benachrichtigung geöffnet wurde
        intent?.extras?.let { extras ->
            val taskId = extras.getLong("open_task_id", -1)
            if (taskId != -1L) {
                // Öffne spezifische Aufgabe
                lifecycleScope.launch {
                    val task = repository.getTaskById(taskId)
                    task?.let {
                        taskViewModel.showEditTaskDialog(it)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Aktualisiere Smart Scores wenn App wieder in den Vordergrund kommt
        lifecycleScope.launch {
            repository.refreshSmartScores()
            repository.refreshTodayListIfNeeded()
        }
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent()
    }
}