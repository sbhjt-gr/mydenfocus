package com.gorai.myedenfocus.domain.model

import java.time.LocalDateTime

data class MeditationSession(
    val id: Int = 0,
    val duration: Int,  // in minutes
    val timestamp: LocalDateTime,
) 