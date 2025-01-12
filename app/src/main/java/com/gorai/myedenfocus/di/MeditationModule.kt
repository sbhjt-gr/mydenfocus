package com.gorai.myedenfocus.di

import android.app.Application
import androidx.room.Room
import com.gorai.myedenfocus.data.local.MeditationDatabase
import com.gorai.myedenfocus.data.repository.MeditationRepositoryImpl
import com.gorai.myedenfocus.domain.repository.MeditationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MeditationModule {

    @Provides
    @Singleton
    fun provideMeditationDatabase(app: Application): MeditationDatabase {
        return Room.databaseBuilder(
            app,
            MeditationDatabase::class.java,
            "meditation_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideMeditationRepository(db: MeditationDatabase): MeditationRepository {
        return MeditationRepositoryImpl(db.meditationSessionDao)
    }
} 