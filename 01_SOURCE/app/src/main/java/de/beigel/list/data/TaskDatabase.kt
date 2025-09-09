package de.beigel.list.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import kotlinx.coroutines.flow.first

@Database(
    entities = [TaskEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: TaskDatabase? = null

        fun getDatabase(context: Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "task_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Backup alte Daten
                database.execSQL("CREATE TABLE tasks_backup AS SELECT * FROM tasks")

                // Lösche alte Tabelle
                database.execSQL("DROP TABLE tasks")

                // Erstelle neue Tabelle mit erweiterten Feldern
                database.execSQL("""
                    CREATE TABLE tasks (
                        id TEXT PRIMARY KEY NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        priority TEXT NOT NULL DEFAULT 'MEDIUM',
                        position INTEGER NOT NULL DEFAULT 0,
                        date TEXT NOT NULL,
                        createdAt TEXT NOT NULL,
                        completedAt TEXT,
                        lastModified TEXT NOT NULL,
                        dueDate TEXT,
                        estimatedMinutes INTEGER,
                        actualMinutes INTEGER,
                        energyLevel TEXT NOT NULL DEFAULT 'MEDIUM',
                        context TEXT,
                        isRecurring INTEGER NOT NULL DEFAULT 0,
                        recurrencePattern TEXT,
                        tags TEXT NOT NULL DEFAULT '',
                        reminderTime TEXT,
                        smartScore REAL NOT NULL DEFAULT 0.0,
                        lastScoreUpdate TEXT NOT NULL
                    )
                """)

                // Kopiere alte Daten zurück mit Standardwerten für neue Felder
                database.execSQL("""
                    INSERT INTO tasks (
                        id, title, description, isCompleted, priority, position, 
                        date, createdAt, completedAt, lastModified, smartScore, lastScoreUpdate
                    )
                    SELECT 
                        id, title, description, isCompleted, priority, position,
                        date, createdAt, completedAt, 
                        COALESCE(createdAt, datetime('now')),
                        0.0,
                        datetime('now')
                    FROM tasks_backup
                """)

                // Lösche Backup-Tabelle
                database.execSQL("DROP TABLE tasks_backup")

                // Erstelle neue Indices
                database.execSQL("CREATE INDEX index_tasks_date ON tasks(date)")
                database.execSQL("CREATE INDEX index_tasks_isCompleted ON tasks(isCompleted)")
                database.execSQL("CREATE INDEX index_tasks_date_isCompleted ON tasks(date, isCompleted)")
                database.execSQL("CREATE INDEX index_tasks_dueDate ON tasks(dueDate)")
                database.execSQL("CREATE INDEX index_tasks_priority ON tasks(priority)")
                database.execSQL("CREATE INDEX index_tasks_context ON tasks(context)")
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority): String = priority.name

    @TypeConverter
    fun toTaskPriority(priority: String): TaskPriority =
        try {
            TaskPriority.valueOf(priority)
        } catch (e: IllegalArgumentException) {
            TaskPriority.MEDIUM
        }

    @TypeConverter
    fun fromEnergyLevel(level: EnergyLevel): String = level.name

    @TypeConverter
    fun toEnergyLevel(level: String): EnergyLevel =
        try {
            EnergyLevel.valueOf(level)
        } catch (e: IllegalArgumentException) {
            EnergyLevel.MEDIUM
        }

    @TypeConverter
    fun fromTaskContext(context: TaskContext?): String? = context?.name

    @TypeConverter
    fun toTaskContext(context: String?): TaskContext? =
        context?.let {
            try {
                TaskContext.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

    @TypeConverter
    fun fromRecurrencePattern(pattern: RecurrencePattern?): String? = pattern?.name

    @TypeConverter
    fun toRecurrencePattern(pattern: String?): RecurrencePattern? =
        pattern?.let {
            try {
                RecurrencePattern.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
}

// Datenbank-Utilities
object DatabaseUtils {

    suspend fun initializeDefaultTasks(taskDao: TaskDao) {
        val existingTasks = taskDao.getActiveTasksCount()

        if (existingTasks == 0) {
            // Erstelle ein paar Beispiel-Aufgaben für neue Nutzer
            val defaultTasks = listOf(
                TaskEntity(
                    title = "Willkommen bei Daily List! 🎉",
                    description = "Tippe auf die Aufgabe um mehr Details zu sehen. Mit dem Info-Icon siehst du warum Aufgaben wichtig sind.",
                    priority = TaskPriority.HIGH,
                    estimatedMinutes = 5,
                    energyLevel = EnergyLevel.LOW,
                    context = null,
                    tags = "tutorial,welcome"
                ),
                TaskEntity(
                    title = "Erste echte Aufgabe hinzufügen",
                    description = "Nutze den + Button um eine neue Aufgabe zu erstellen. Probiere die Smart-Eingabe aus!",
                    priority = TaskPriority.MEDIUM,
                    estimatedMinutes = 10,
                    energyLevel = EnergyLevel.LOW,
                    context = null,
                    tags = "tutorial"
                ),
                TaskEntity(
                    title = "Quick Win: E-Mails checken",
                    description = "Eine schnelle Aufgabe für Zwischendurch",
                    priority = TaskPriority.LOW,
                    estimatedMinutes = 15,
                    energyLevel = EnergyLevel.LOW,
                    context = TaskContext.COMPUTER,
                    tags = "quick-win,daily"
                )
            )

            defaultTasks.forEach { task ->
                taskDao.insertTask(task)
            }
        }
    }

    suspend fun cleanupOldData(taskDao: TaskDao, daysToKeep: Int = 30) {
        val cutoffDate = java.time.LocalDate.now()
            .minusDays(daysToKeep.toLong())
            .toString()

        taskDao.deleteOldCompletedTasks(cutoffDate)
    }

    suspend fun resetAllSmartScores(taskDao: TaskDao) {
        val cutoffTime = java.time.LocalDateTime.now()
            .minusHours(1)
            .toString()

        taskDao.resetOutdatedSmartScores(cutoffTime)
    }

    suspend fun exportTasksToJson(taskDao: TaskDao): String {
        val allTasks = taskDao.getAllActiveTasks().first()

        // Vereinfachter JSON-Export (in echter App würde man Gson/Moshi verwenden)
        return buildString {
            append("{\n")
            append("  \"tasks\": [\n")

            allTasks.forEachIndexed { index: Int, task: TaskEntity ->
                append("    {\n")
                append("      \"id\": \"${task.id}\",\n")
                append("      \"title\": \"${task.title.replace("\"", "\\\"")}\",\n")
                append("      \"description\": \"${task.description.replace("\"", "\\\"")}\",\n")
                append("      \"priority\": \"${task.priority.name}\",\n")
                append("      \"isCompleted\": ${task.isCompleted},\n")
                append("      \"date\": \"${task.date}\",\n")
                append("      \"dueDate\": ${if (task.dueDate != null) "\"${task.dueDate}\"" else "null"},\n")
                append("      \"estimatedMinutes\": ${task.estimatedMinutes ?: "null"},\n")
                append("      \"energyLevel\": \"${task.energyLevel.name}\",\n")
                append("      \"context\": ${if (task.context != null) "\"${task.context!!.name}\"" else "null"},\n")
                append("      \"tags\": \"${task.tags}\"\n")
                append("    }")

                if (index < allTasks.size - 1) {
                    append(",")
                }
                append("\n")
            }

            append("  ],\n")
            append("  \"exportDate\": \"${java.time.LocalDateTime.now()}\",\n")
            append("  \"version\": \"2.0\"\n")
            append("}")
        }
    }

    fun validateTaskData(task: TaskEntity): List<String> {
        val errors = mutableListOf<String>()

        if (task.title.isBlank()) {
            errors.add("Titel darf nicht leer sein")
        }

        if (task.title.length > 200) {
            errors.add("Titel ist zu lang (max. 200 Zeichen)")
        }

        if (task.description.length > 1000) {
            errors.add("Beschreibung ist zu lang (max. 1000 Zeichen)")
        }

        task.dueDate?.let { dateStr ->
            try {
                java.time.LocalDate.parse(dateStr)
            } catch (e: Exception) {
                errors.add("Ungültiges Fälligkeitsdatum")
            }
        }

        task.estimatedMinutes?.let { minutes ->
            if (minutes < 1 || minutes > 1440) { // 1 Min bis 24 Stunden
                errors.add("Ungültige Zeitschätzung (1-1440 Minuten)")
            }
        }

        if (task.tags.length > 500) {
            errors.add("Tags sind zu lang")
        }

        return errors
    }
}

// Repository Extensions für bessere Datenbank-Performance
suspend fun TaskDao.insertTaskWithValidation(task: TaskEntity): Result<TaskEntity> {
    return try {
        val errors = DatabaseUtils.validateTaskData(task)
        if (errors.isNotEmpty()) {
            Result.failure(IllegalArgumentException("Validation errors: ${errors.joinToString(", ")}"))
        } else {
            insertTask(task)
            Result.success(task)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend fun TaskDao.updateTaskWithValidation(task: TaskEntity): Result<TaskEntity> {
    return try {
        val errors = DatabaseUtils.validateTaskData(task)
        if (errors.isNotEmpty()) {
            Result.failure(IllegalArgumentException("Validation errors: ${errors.joinToString(", ")}"))
        } else {
            val updatedTask = task.copy(
                lastModified = java.time.LocalDateTime.now().toString()
            )
            updateTask(updatedTask)
            Result.success(updatedTask)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Background-Task für regelmäßige Datenbankwartung
class DatabaseMaintenanceWorker(
    context: Context,
    workerParams: androidx.work.WorkerParameters
) : androidx.work.CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        return try {
            val database = TaskDatabase.getDatabase(applicationContext)
            val taskDao = database.taskDao()

            // Bereinige alte Daten
            DatabaseUtils.cleanupOldData(taskDao)

            // Setze veraltete Smart Scores zurück
            DatabaseUtils.resetAllSmartScores(taskDao)

            androidx.work.ListenableWorker.Result.success()
        } catch (e: Exception) {
            androidx.work.ListenableWorker.Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val workRequest = androidx.work.PeriodicWorkRequestBuilder<DatabaseMaintenanceWorker>(
                24, java.util.concurrent.TimeUnit.HOURS
            )
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(true)
                        .setRequiresCharging(false)
                        .setRequiresDeviceIdle(true)
                        .build()
                )
                .build()

            androidx.work.WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "database_maintenance",
                    androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
        }
    }
}