package com.beigel.list2share.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Lokale Datenbank für Listen, die (noch) nicht geteilt sind.
 * Getrennt von Firestore: eine Liste lebt entweder hier oder in der Cloud.
 */
@Database(
    entities = [LocalListEntity::class, LocalTodoEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun listDao(): LocalListDao
    abstract fun todoDao(): LocalTodoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Einkaufsmodus: `mode` an der Liste, `quantity` am Todo.
         *
         * Als echte Migration und nicht destruktiv – lokale Listen liegen nur
         * auf dem Gerät, ein Zurücksetzen der Datenbank würde sie ersatzlos
         * löschen.
         */
        /** Anschaffungsmodus: Preis und Link am Todo. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE local_todos ADD COLUMN price REAL")
                db.execSQL("ALTER TABLE local_todos ADD COLUMN link TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE local_lists ADD COLUMN mode TEXT NOT NULL DEFAULT 'AUFGABEN'")
                db.execSQL("ALTER TABLE local_todos ADD COLUMN quantity TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dotlist_local.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
            }
    }
}
