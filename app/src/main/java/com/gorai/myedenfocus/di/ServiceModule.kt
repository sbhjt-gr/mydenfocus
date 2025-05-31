package com.gorai.myedenfocus.di

import android.content.Context
import com.gorai.myedenfocus.service.GeminiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideGeminiService(
        @ApplicationContext context: Context
    ): GeminiService {
        return GeminiService(context)
    }
} 