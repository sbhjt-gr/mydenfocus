package com.gorai.myedenfocus.util

import androidx.compose.runtime.compositionLocalOf
import com.gorai.myedenfocus.presentation.session.StudySessionTimerService

val LocalTimerService = compositionLocalOf<StudySessionTimerService> { 
    error("No TimerService provided") 
} 