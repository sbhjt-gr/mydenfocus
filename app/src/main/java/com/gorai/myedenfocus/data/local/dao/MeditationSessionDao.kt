package com.gorai.myedenfocus.data.local.dao

import androidx.room.*
import com.gorai.myedenfocus.data.local.entity.MeditationSessionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface MeditationSessionDao {
    @Query("SELECT * FROM meditation_sessions WHERE timestamp >= :startDate ORDER BY timestamp DESC")
    fun getRecentSessions(startDate: LocalDateTime): Flow<List<MeditationSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: MeditationSessionEntity)

    @Delete
    suspend fun deleteSession(session: MeditationSessionEntity)
} 