package com.gorai.myedenfocus.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.ZoneId

@Entity
data class Session(
    @PrimaryKey(autoGenerate = true)
    val sessionId: Long = 0L,
    
    // Subject and Topic Information
    val sessionSubjectId: Int,
    val relatedToSubject: String,
    val topicId: Int? = null,
    val topicName: String = "",
    
    // Timing Information
    val startTime: Long,  // Session start timestamp
    val endTime: Long,    // Session end timestamp
    val duration: Long,   // Duration in minutes
    val plannedDuration: Long, // Originally planned duration in minutes
    
    // Session Details
    val wasCompleted: Boolean = false,  // Whether session was completed or stopped early
    val focusScore: Int = 0,           // 0-100 score based on pauses and interruptions
    val pauseCount: Int = 0,           // Number of times session was paused
    val totalPauseDuration: Long = 0,  // Total pause duration in minutes
    
    // Additional Metadata
    val notes: String = "",            // Optional notes about the session
    val mood: SessionMood = SessionMood.NEUTRAL,
    val productivityRating: Int = 0,   // 0-5 rating given by user
    val tags: List<String> = emptyList()
) {
    // Computed properties
    val effectiveDuration: Long
        get() = duration - totalPauseDuration
        
    val completionPercentage: Float
        get() = (duration.toFloat() / plannedDuration.toFloat()) * 100

    val date: Long
        get() = startTime

    companion object {
        fun create(
            subjectId: Int,
            subjectName: String,
            topicId: Int? = null,
            topicName: String = "",
            plannedDurationMinutes: Long,
            startTimeMillis: Long = System.currentTimeMillis()
        ): Session = Session(
            sessionSubjectId = subjectId,
            relatedToSubject = subjectName,
            topicId = topicId,
            topicName = topicName,
            startTime = startTimeMillis,
            endTime = startTimeMillis, // Will be updated when session ends
            duration = 0,              // Will be updated when session ends
            plannedDuration = plannedDurationMinutes
        )
    }
}

enum class SessionMood {
    VERY_PRODUCTIVE,
    PRODUCTIVE,
    NEUTRAL,
    DISTRACTED,
    VERY_DISTRACTED;

    companion object {
        fun fromProductivityRating(rating: Int): SessionMood = when(rating) {
            5 -> VERY_PRODUCTIVE
            4 -> PRODUCTIVE
            3 -> NEUTRAL
            2 -> DISTRACTED
            1 -> VERY_DISTRACTED
            else -> NEUTRAL
        }
    }
}
