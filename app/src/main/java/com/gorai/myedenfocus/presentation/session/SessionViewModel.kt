package com.gorai.myedenfocus.presentation.session

import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorai.myedenfocus.domain.model.Session
import com.gorai.myedenfocus.domain.repository.SessionRepository
import com.gorai.myedenfocus.domain.repository.SubjectRepository
import com.gorai.myedenfocus.domain.repository.TaskRepository
import com.gorai.myedenfocus.util.SnackbarEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val sessionRepository: SessionRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SessionState())
    
    private val subjects = subjectRepository.getAllSubjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
    private val topics = taskRepository.getAllUpcomingTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val state = combine(
        _state,
        subjects,
        topics,
        sessionRepository.getRecentFiveSessions(subjectId = 0)
    ) { state, subjects, topics, sessions ->
        state.copy(
            subjects = subjects,
            topics = topics,
            sessions = sessions
        )
    }.stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SessionState()
    )

    private val _snackbarEventFlow = MutableSharedFlow<SnackbarEvent>()
    val snackbarEventFlow = _snackbarEventFlow.asSharedFlow()

    fun onEvent(event: SessionEvent) {
        when(event) {
            is SessionEvent.OnTopicSelect -> {
                _state.update { 
                    it.copy(
                        selectedTopicId = event.task.taskId,
                        subjectId = event.task.taskSubjectId,
                        selectedDuration = event.task.taskDuration
                    )
                }
            }
            is SessionEvent.OnRelatedSubjectChange -> {
                _state.update {
                    it.copy(
                        subjectId = event.subject.subjectId,
                        relatedToSubject = event.subject.name
                    )
                }
            }
            is SessionEvent.DeleteSession -> deleteSession()
            is SessionEvent.OnDeleteSessionButtonClick -> {
                _state.update {
                    it.copy(session = event.session)
                }
            }
            is SessionEvent.SaveSession -> {
                viewModelScope.launch {
                    try {
                        sessionRepository.insertSession(
                            session = Session(
                                sessionSubjectId = state.value.subjectId ?: -1,
                                relatedToSubject = state.value.relatedToSubject ?: "",
                                date = Instant.now().toEpochMilli(),
                                duration = state.value.selectedDuration.toLong()
                            )
                        )
                        _snackbarEventFlow.emit(
                            SnackbarEvent.ShowSnackbar(message = "Session saved successfully.")
                        )
                    } catch (e: Exception) {
                        _snackbarEventFlow.emit(
                            SnackbarEvent.ShowSnackbar(
                                "Couldn't save session",
                                SnackbarDuration.Long
                            )
                        )
                    }
                }
            }
            is SessionEvent.OnDurationSelected -> {
                _state.update { it.copy(selectedDuration = event.minutes) }
            }
            is SessionEvent.CheckSubjectId -> notifyToUpdateSubject()
        }
    }

    private fun notifyToUpdateSubject() {
        viewModelScope.launch {
            if (_state.value.subjects.isEmpty()) {
                _snackbarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        message = "Please create a subject first."
                    )
                )
            } else if (_state.value.subjectId == null) {
                // Auto-select first subject if none selected
                val firstSubject = _state.value.subjects.first()
                onEvent(SessionEvent.OnRelatedSubjectChange(firstSubject))
            }
        }
    }

    private fun deleteSession() {
        viewModelScope.launch {
            try {
                state.value.session?.let {
                    sessionRepository.deleteSession(it)
                    _snackbarEventFlow.emit(SnackbarEvent.ShowSnackbar(message = "Session deleted successfully"))
                }
            } catch (e: Exception) {
                _snackbarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        message = "Couldn't delete session. ${e.message}",
                        duration = SnackbarDuration.Long
                    )
                )
            }
        }
    }
}