package com.gorai.myedenfocus.data.repository

import androidx.compose.ui.graphics.Color
import com.gorai.myedenfocus.data.local.SessionDao
import com.gorai.myedenfocus.domain.model.Session
import com.gorai.myedenfocus.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao
): SessionRepository {
    override suspend fun insertSession(session: Session) {
        sessionDao.insertSession(session)
    }

    override suspend fun deleteSession(session: Session) {
        sessionDao.deleteSession(session)
    }

    override fun getAllSessions(): Flow<List<Session>> {
        return sessionDao.getAllSessions().map { sessions -> sessions.sortedByDescending { it.date } }
    }

    override fun getRecentFiveSessions(subjectId: Int): Flow<List<Session>> {
        return sessionDao.getAllSessions()
            .map { sessions -> sessions.sortedByDescending { it.date } }
            .take(count = 5)
    }

    override fun getRecentTenSessionsForSubject(subjectId: Int): Flow<List<Session>> {
        return sessionDao.getRecentTenSessionsForSubject(subjectId)
    }

    override fun getTotalSessionsDuration(): Flow<Long> {
        return sessionDao.getTotalSessionsDuration()
    }

    override fun getTotalSessionsDurationBySubject(subjectId: Int): Flow<Long> {
        return sessionDao.getTotalSessionsDurationBySubject(subjectId)
    }

    override fun getTodaySessionsDuration(): Flow<Long> = flow {
        val startOfDay = LocalDateTime.now().with(LocalTime.MIN).toInstant(ZoneOffset.UTC).toEpochMilli()
        val endOfDay = LocalDateTime.now().with(LocalTime.MAX).toInstant(ZoneOffset.UTC).toEpochMilli()
        
        emit(sessionDao.getTotalDurationBetween(startOfDay, endOfDay))
    }

    override fun getTodaySessionsDurationBySubject(subjectId: Int): Flow<Long> = flow {
        val startOfDay = LocalDateTime.now().with(LocalTime.MIN).toInstant(ZoneOffset.UTC).toEpochMilli()
        val endOfDay = LocalDateTime.now().with(LocalTime.MAX).toInstant(ZoneOffset.UTC).toEpochMilli()
        
        emit(sessionDao.getTotalDurationBetweenForSubject(startOfDay, endOfDay, subjectId))
    }
}