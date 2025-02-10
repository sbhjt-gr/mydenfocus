package com.gorai.myedenfocus.domain.repository

import com.gorai.myedenfocus.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    suspend fun insertSession(session: Session): Long
    suspend fun deleteSession(session: Session)
    suspend fun getSessionById(sessionId: Long): Session?
    suspend fun updateSession(session: Session)
    fun getAllSessions(): Flow<List<Session>>
    fun getRecentFiveSessions(subjectId: Int): Flow<List<Session>>
    fun getRecentTenSessionsForSubject(subjectId: Int): Flow<List<Session>>
    fun getTotalSessionsDuration(): Flow<Float>
    fun getTotalSessionsDurationBySubject(subjectId: Int): Flow<Float>
    fun getTodaySessionsDuration(): Flow<Float>
}