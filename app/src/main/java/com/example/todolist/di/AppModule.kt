package com.example.todolist.di

import android.content.Context
import com.example.todolist.data.api.TodoApi
import com.example.todolist.data.local.AppDatabase
import com.example.todolist.data.local.TaskDao
import com.example.todolist.data.local.UserPreferences
import com.example.todolist.data.reminder.AndroidReminderScheduler
import com.example.todolist.data.repository.AuthRepositoryImpl
import com.example.todolist.data.repository.FeedRepositoryImpl
import com.example.todolist.data.repository.SessionRepositoryImpl
import com.example.todolist.data.repository.TaskRepositoryImpl
import com.example.todolist.domain.repository.AuthRepository
import com.example.todolist.domain.repository.FeedRepository
import com.example.todolist.domain.repository.SessionRepository
import com.example.todolist.domain.repository.TaskRepository
import com.example.todolist.domain.service.ReminderScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideTaskDao(database: AppDatabase): TaskDao {
        return database.taskDao()
    }

    @Provides
    @Singleton
    fun provideTodoApi(userPreferences: UserPreferences): TodoApi {
        return TodoApi("http://10.0.2.2:8080", userPreferences)  // ← Передаём userPreferences
    }

    @Provides
    @Singleton
    fun provideTaskRepository(
        api: TodoApi,
        taskDao: TaskDao,
        userPreferences: UserPreferences
    ): TaskRepository {
        return TaskRepositoryImpl(api, taskDao, userPreferences)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        api: TodoApi,
        userPreferences: UserPreferences
    ): AuthRepository {
        return AuthRepositoryImpl(api, userPreferences)
    }

    @Provides
    @Singleton
    fun provideFeedRepository(api: TodoApi): FeedRepository {
        return FeedRepositoryImpl(api)
    }

    @Provides
    @Singleton
    fun provideSessionRepository(
        userPreferences: UserPreferences
    ): SessionRepository {
        return SessionRepositoryImpl(userPreferences)
    }

    @Provides
    @Singleton
    fun provideReminderScheduler(@ApplicationContext context: Context): ReminderScheduler {
        return AndroidReminderScheduler(context.applicationContext)
    }

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences {
        return UserPreferences(context)
    }
}
