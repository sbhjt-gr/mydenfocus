package com.gorai.myedenfocus.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.gorai.myedenfocus.domain.model.Session
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: Session)

    @Delete
    suspend fun deleteSession(session: Session)

    @Query("SELECT * FROM session")
    fun getAllSessions(): Flow<List<Session>>

    @Query("""
        SELECT * FROM session 
        WHERE sessionSubjectId = :subjectId 
        ORDER BY sessionId DESC 
        LIMIT 10
    """)
    fun getRecentTenSessionsForSubject(subjectId: Int): Flow<List<Session>>

    @Query("SELECT SUM(duration) FROM Session")
    fun getTotalSessionsDuration(): Flow<Long>

    @Query("SELECT SUM(duration) FROM Session WHERE sessionSubjectId = :subjectId")
    fun getTotalSessionsDurationBySubject(subjectId: Int): Flow<Long>

    @Query("DELETE FROM Session WHERE sessionSubjectId = :subjectId")
    suspend fun deleteSessionsBySubjectId(subjectId: Int)
}
