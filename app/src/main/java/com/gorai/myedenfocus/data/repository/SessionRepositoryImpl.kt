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

class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao
): SessionRepository {
    override suspend fun insertSession(session: Session): Long {
        return sessionDao.insertSession(session)
    }

    override suspend fun deleteSession(session: Session) {
        sessionDao.deleteSession(session)
    }

    override suspend fun getSessionById(sessionId: Long): Session? {
        return sessionDao.getSessionById(sessionId)
    }

    override suspend fun updateSession(session: Session) {
        sessionDao.updateSession(session)
    }

    override fun getAllSessions(): Flow<List<Session>> {
        return sessionDao.getAllSessions().map { sessions -> sessions.sortedByDescending { it.startTime } }
    }

    override fun getRecentFiveSessions(subjectId: Int): Flow<List<Session>> {
        return sessionDao.getAllSessions()
            .map { sessions -> sessions.sortedByDescending { it.startTime } }
            .take(count = 5)
    }

    override fun getRecentTenSessionsForSubject(subjectId: Int): Flow<List<Session>> {
        return sessionDao.getRecentTenSessionsForSubject(subjectId)
    }

    override fun getTotalSessionsDuration(): Flow<Float> {
        return sessionDao.getTotalSessionsDuration().map { it.toFloat() / 60.0f }
    }

    override fun getTotalSessionsDurationBySubject(subjectId: Int): Flow<Float> {
        return sessionDao.getTotalSessionsDurationBySubject(subjectId).map { it.toFloat() / 60.0f }
    }

    override fun getTodaySessionsDuration(): Flow<Float> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfDay = calendar.timeInMillis

        return sessionDao.getAllSessions().map { sessions ->
            sessions.filter { it.startTime >= startOfDay }
                   .sumOf { it.duration.toDouble() }
                   .toFloat() / 60.0f
        }
    }
}