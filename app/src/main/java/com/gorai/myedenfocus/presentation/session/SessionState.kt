package com.gorai.myedenfocus.presentation.session

import com.gorai.myedenfocus.domain.model.Session
import com.gorai.myedenfocus.domain.model.Subject
import com.gorai.myedenfocus.domain.model.Task

data class SessionState(
    val subjectId: Int? = null,
    val selectedTopicId: Int? = null,
    val selectedDuration: Int = 0,
    val relatedToSubject: String? = null,
    val subjects: List<Subject> = emptyList(),
    val topics: List<Task> = emptyList(),
    val sessions: List<Session> = emptyList(),
    val session: Session? = null
)