package com.gorai.myedenfocus.presentation.subject

import androidx.compose.ui.graphics.Color
import com.gorai.myedenfocus.domain.model.Session
import com.gorai.myedenfocus.domain.model.Subject
import com.gorai.myedenfocus.domain.model.Task

data class SubjectState(
    val currentSubjectId: Int? = null,
    val subjectName: String = "",
    val goalStudyHours: String = "",
    val dailyStudyGoal: String = "",
    val studiedHours: Float = 0f,
    val progress: Float = 0f,
    val subjectCardColors: List<Color> = Subject.subjectCardColors.random(),
    val upcomingTasks: List<Task> = emptyList(),
    val completedTasks: List<Task> = emptyList(),
    val recentSessions: List<Session> = emptyList(),
    val allSubjects: List<Subject> = emptyList(),
    val session: Session? = null,
    val subjectDaysPerWeek: Int = 5
)
