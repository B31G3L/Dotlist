package de.beigel.list.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.beigel.list.data.TaskDao
import de.beigel.list.data.TaskDatabase
import de.beigel.list.repository.TaskRepository
import de.beigel.list.settings.SettingsManager
import de.beigel.list.utils.TaskPriorityCalculator
import de.beigel.list.notification.NotificationManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTaskDatabase(@ApplicationContext context: Context): TaskDatabase {
        return TaskDatabase.getDatabase(context)
    }

    @Provides
    fun provideTaskDao(database: TaskDatabase): TaskDao {
        return database.taskDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }

    @Provides
    @Singleton
    fun provideTaskPriorityCalculator(): TaskPriorityCalculator {
        return TaskPriorityCalculator()
    }

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        settingsManager: SettingsManager,
        priorityCalculator: TaskPriorityCalculator
    ): TaskRepository {
        return TaskRepository(taskDao, settingsManager, priorityCalculator)
    }

    @Provides
    @Singleton
    fun provideNotificationManager(@ApplicationContext context: Context): NotificationManager {
        return NotificationManager(context)
    }
}