package com.gorai.myedenfocus.data.repository

import com.gorai.myedenfocus.data.local.dao.MeditationSessionDao
import com.gorai.myedenfocus.data.local.entity.MeditationSessionEntity
import com.gorai.myedenfocus.domain.model.MeditationSession
import com.gorai.myedenfocus.domain.repository.MeditationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject

class MeditationRepositoryImpl @Inject constructor(
    private val dao: MeditationSessionDao
) : MeditationRepository {
    override fun getRecentSessions(days: Int): Flow<List<MeditationSession>> {
        val startDate = LocalDateTime.now().minusDays(days.toLong())
        return dao.getRecentSessions(startDate).map { entities ->
            entities.map { it.toMeditationSession() }
        }
    }

    override suspend fun insertSession(session: MeditationSession) {
        dao.insertSession(session.toMeditationSessionEntity())
    }

    override suspend fun deleteSession(session: MeditationSession) {
        dao.deleteSession(session.toMeditationSessionEntity())
    }
}

private fun MeditationSessionEntity.toMeditationSession(): MeditationSession {
    return MeditationSession(
        id = id,
        duration = duration,
        timestamp = timestamp
    )
}

private fun MeditationSession.toMeditationSessionEntity(): MeditationSessionEntity {
    return MeditationSessionEntity(
        id = id,
        duration = duration,
        timestamp = timestamp
    )
} 