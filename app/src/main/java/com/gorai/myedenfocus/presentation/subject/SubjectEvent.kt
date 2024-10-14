package com.gorai.myedenfocus.presentation.subject

import androidx.compose.ui.graphics.Color
import com.gorai.myedenfocus.domain.model.Task

sealed class SubjectEvent {
    data object UpdateSubject : SubjectEvent()
    data object DeleteSubject : SubjectEvent()
    data object DeleteSession : SubjectEvent()
    data class OnTaskIsCompletedChange(val task: Task) : SubjectEvent()
    data class OnSubjectCardColorChange(val color: List<Color>) : SubjectEvent()
    data class OnSubjectNameChange(val name: String) : SubjectEvent()
    data class OnGoalStudyHoursChange(val hours: String) : SubjectEvent()
    data class OnDeleteSessionButtonClick(val session: String) : SubjectEvent()
}