package com.gorai.myedenfocus.domain.repository

import com.gorai.myedenfocus.domain.model.MeditationSession
import kotlinx.coroutines.flow.Flow

interface MeditationRepository {
    fun getRecentSessions(days: Int): Flow<List<MeditationSession>>
    suspend fun insertSession(session: MeditationSession)
    suspend fun deleteSession(session: MeditationSession)
} 