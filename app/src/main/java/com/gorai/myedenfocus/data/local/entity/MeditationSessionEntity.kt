package com.gorai.myedenfocus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "meditation_sessions")
data class MeditationSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val duration: Int,
    val timestamp: LocalDateTime
) 