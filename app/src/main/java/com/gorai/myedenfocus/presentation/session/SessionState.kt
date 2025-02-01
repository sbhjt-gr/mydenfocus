package com.gorai.myedenfocus.presentation.session

import com.gorai.myedenfocus.domain.model.Session
import com.gorai.myedenfocus.domain.model.Subject
import com.gorai.myedenfocus.domain.model.Task

data class SessionState(
    val subjects: List<Subject> = emptyList(),
    val topics: List<Task> = emptyList(),
    val sessions: List<Session> = emptyList(),
    val selectedSubjectId: Int? = null,
    val selectedTopicId: Int? = null,
    val selectedDuration: Int = 0,
    val subjectId: Int? = null,
    val relatedToSubject: String? = null,
    val showDeleteDialog: Boolean = false,
    val showMeditationDialog: Boolean = false,
    val session: Session? = null
)