package com.gorai.myedenfocus.presentation.session

import com.gorai.myedenfocus.domain.model.Session
import com.gorai.myedenfocus.domain.model.Subject
import com.gorai.myedenfocus.domain.model.Task

sealed interface SessionEvent {
    data class OnTopicSelect(val task: Task?) : SessionEvent
    data class OnRelatedSubjectChange(val subject: Subject) : SessionEvent
    data class OnDeleteSessionButtonClick(val session: Session) : SessionEvent
    data class OnDurationSelected(val minutes: Int) : SessionEvent
    data class InitializeWithTopic(val topicId: Int) : SessionEvent
    data class CompleteTask(val taskId: Int) : SessionEvent
    object SaveSession : SessionEvent
    object DeleteSession : SessionEvent
    object CheckSubjectId : SessionEvent
    object CancelSession : SessionEvent
    object RefreshScreen : SessionEvent
}