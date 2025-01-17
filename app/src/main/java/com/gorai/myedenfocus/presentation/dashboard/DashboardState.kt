package com.gorai.myedenfocus.presentation.dashboard

import com.gorai.myedenfocus.domain.model.Session
import com.gorai.myedenfocus.domain.model.Subject
import androidx.compose.ui.graphics.Color

data class DashboardState(
    val currentSubjectId: Int? = null,
    val subjectName: String = "",
    val goalStudyHours: String = "",
    val dailyStudyGoal: String = "",
    val totalSubjectCount: Int = 0,
    val subjects: List<Subject> = emptyList(),
    val totalStudiedHours: String = "",
    val dailyStudiedHours: String = "",
    val subjectCardColors: List<Color> = Subject.subjectCardColors.random(),
    val session: Session? = null,
    val studyDaysPerWeek: Int = 5,
    val subjectDaysPerWeek: Int = 5
)
