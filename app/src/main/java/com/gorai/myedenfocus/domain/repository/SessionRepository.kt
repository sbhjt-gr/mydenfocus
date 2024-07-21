package com.gorai.myedenfocus.domain.repository

import com.gorai.myedenfocus.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    suspend fun insertSession(session: Session)
    suspend fun deleteSession(session: Session)
    fun getAllSessions(): Flow<List<Session>>
    fun getRecentFiveSessions(subjectId: Int): Flow<List<Session>>
    fun getRecentTenSessionsForSubject(sessionId: Int): Flow<List<Session>>
    fun getTotalSessionsDuration(): Flow<Long>
    fun getTotalSessionsDurationBySubjectId(subjectId: Int): Flow<Long>
}