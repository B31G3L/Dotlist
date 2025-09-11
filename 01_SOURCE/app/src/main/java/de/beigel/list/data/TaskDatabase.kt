package de.beigel.list.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate

@Database(
    entities = [TaskEntity::class],
    version = 2, // Version erhöht wegen neuer Felder
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: TaskDatabase? = null

        // Migration von Version 1 zu Version 2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Neue Spalten hinzufügen
                database.execSQL("ALTER TABLE tasks ADD COLUMN isInDailyList INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE tasks ADD COLUMN backlogPosition INTEGER NOT NULL DEFAULT 0")

                // Alle bestehenden Aufgaben als "Daily List" markieren
                database.execSQL("UPDATE tasks SET isInDailyList = 1 WHERE isInDailyList IS NULL")

                // Backlog-Positionen für bestehende Aufgaben setzen
                database.execSQL("""
                    UPDATE tasks 
                    SET backlogPosition = (
                        SELECT COUNT(*) 
                        FROM tasks t2 
                        WHERE t2.id < tasks.id AND t2.isInDailyList = 0
                    ) 
                    WHERE isInDailyList = 0
                """)
            }
        }

        fun getDatabase(context: android.content.Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "task_database"
                )
                    .addMigrations(MIGRATION_1_2) // Migration hinzufügen
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority): String = priority.name

    @TypeConverter
    fun toTaskPriority(priority: String): TaskPriority = TaskPriority.valueOf(priority)
}