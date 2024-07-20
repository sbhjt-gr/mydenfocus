package com.gorai.myedenfocus.data.local

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.gorai.myedenfocus.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionDao {
    @Insert
    suspend fun insertSession(session: Session)

    @Delete
    suspend fun deleteSession(session: Session)

    @Query("SELECT * FROM Session")
    fun getAllSessions(): Flow<List<Session>>

    @Query("SELECT * FROM Session WHERE sessionId = :sessionId")
    fun getRecentSessionsForSubject(sessionId: Int): Flow<List<Session>>

    @Query("SELECT SUM(duration) FROM Session")
    fun getTotalSessionsDuration(): Flow<Long>

    @Query("SELECT SUM(duration) FROM Session WHERE sessionSubjectId = :subjectId")
    fun getTotalSessionsDurationBySubjectId(): Flow<Int>

    @Query("DELETE FROM Session WHERE sessionId = :sessionId")
    fun deleteSessionBySubjectId(subjectId: Int)
}