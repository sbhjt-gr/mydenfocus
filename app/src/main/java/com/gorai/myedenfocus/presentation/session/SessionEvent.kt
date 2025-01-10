package com.gorai.myedenfocus.presentation.session

import com.gorai.myedenfocus.domain.model.Session
import com.gorai.myedenfocus.domain.model.Subject

sealed class SessionEvent {
    object CheckSubjectId : SessionEvent()
    object DeleteSession : SessionEvent()
    data class OnDeleteSessionButtonClick(val session: Session) : SessionEvent()
    data class OnRelatedSubjectChange(val subject: Subject) : SessionEvent()
    data class SaveSession(val duration: Long) : SessionEvent()
    data class UpdateSubjectIdAndRelatedSubject(
        val subjectId: Int?,
        val relatedToSubject: String
    ) : SessionEvent()
}