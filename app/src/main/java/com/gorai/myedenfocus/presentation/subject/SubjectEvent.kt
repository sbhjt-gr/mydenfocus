package com.gorai.myedenfocus.presentation.subject

import androidx.compose.ui.graphics.Color
import com.gorai.myedenfocus.domain.model.Session
import com.gorai.myedenfocus.domain.model.Task

sealed interface SubjectEvent {
    data class OnSubjectNameChange(val name: String): SubjectEvent
    data class OnGoalStudyHoursChange(val hours: String): SubjectEvent
    data class OnSubjectCardColorChange(val colors: List<Color>): SubjectEvent
    data class OnTaskIsCompletedChange(val task: Task): SubjectEvent
    data class OnDeleteSessionButtonClick(val session: Session): SubjectEvent
    data class OnSubjectDaysPerWeekChange(val days: Int): SubjectEvent
    object UpdateSubject: SubjectEvent
    object DeleteSubject: SubjectEvent
    object DeleteSession: SubjectEvent
    object UpdateProgress: SubjectEvent
}