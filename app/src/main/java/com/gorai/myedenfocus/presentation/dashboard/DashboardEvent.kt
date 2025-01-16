package com.gorai.myedenfocus.presentation.dashboard

import androidx.compose.ui.graphics.Color
import com.gorai.myedenfocus.domain.model.Session
import com.gorai.myedenfocus.domain.model.Task

sealed interface DashboardEvent {
    data class OnSubjectNameChange(val name: String): DashboardEvent
    data class OnGoalStudyHoursChange(val hours: String): DashboardEvent
    data class OnSubjectCardColorChange(val colors: List<Color>): DashboardEvent
    data class OnDeleteSessionButtonClick(val session: Session): DashboardEvent
    data class OnTaskIsCompleteChange(val task: Task): DashboardEvent
    data class OnDailyStudyGoalChange(val goal: String): DashboardEvent
    object SaveSubject: DashboardEvent
    object DeleteSubject: DashboardEvent
    object DeleteSession: DashboardEvent
    object SaveDailyStudyGoal: DashboardEvent
}