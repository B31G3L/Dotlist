package com.beigel.list2share.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Lokale Datenbank für Listen, die (noch) nicht geteilt sind.
 * Getrennt von Firestore: eine Liste lebt entweder hier oder in der Cloud.
 */
@Database(
    entities = [LocalListEntity::class, LocalTodoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun listDao(): LocalListDao
    abstract fun todoDao(): LocalTodoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dotlist_local.db"
                ).build().also { INSTANCE = it }
            }
    }
}
