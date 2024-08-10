package com.gorai.myedenfocus.data.di

import android.app.Application
import androidx.room.Room
import com.gorai.myedenfocus.data.local.AppDatabase
import com.gorai.myedenfocus.data.local.SessionDao
import com.gorai.myedenfocus.data.local.SubjectDao
import com.gorai.myedenfocus.data.local.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        application: Application
    ): AppDatabase {
        return Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            "database.db"
        ).build()
    }
    @Provides
    @Singleton
    fun provideSubjectDao(database: AppDatabase): SubjectDao {
        return database.subjectDao()
    }
    @Provides
    @Singleton
    fun provideTaskDao(database: AppDatabase): TaskDao {
        return database.taskDao()
    }
    @Provides
    @Singleton
    fun provideSessionDao(database: AppDatabase): SessionDao {
        return database.sessionDao()
    }
}