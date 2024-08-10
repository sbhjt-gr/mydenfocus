package com.gorai.myedenfocus.presentation.dashboard

import com.gorai.myedenfocus.domain.model.Session
import com.gorai.myedenfocus.domain.model.Subject
import androidx.compose.ui.graphics.Color

data class DashboardState(
    val totalSubjectCount: Int = 0,
    val totalStudiedHours: String = "",
    val totalGoalStudyHours: String = "",
    val subjects: List<Subject> = emptyList(),
    val subjectName: String = "",
    val goalStudyHours: String = "",
    val subjectCardColors: List<Color> = Subject.subjectCardColors.random(),
    val session: Session? = null
)
