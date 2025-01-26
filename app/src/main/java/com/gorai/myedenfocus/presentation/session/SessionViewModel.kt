package com.gorai.myedenfocus.presentation.session

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorai.myedenfocus.domain.model.Session
import com.gorai.myedenfocus.domain.model.Task
import com.gorai.myedenfocus.domain.repository.SessionRepository
import com.gorai.myedenfocus.domain.repository.SubjectRepository
import com.gorai.myedenfocus.domain.repository.TaskRepository
import com.gorai.myedenfocus.util.SnackbarEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
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

    init {
        viewModelScope.launch {
            println("SessionViewModel: Checking pending timer...")
            checkPendingTimer()
        }
    }

    private suspend fun checkPendingTimer() {
        val prefs = context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
        val completionTime = prefs.getLong("completion_time", 0)
        val topicId = prefs.getInt("topic_id", -1)
        val subjectId = prefs.getInt("subject_id", -1)
        val durationMinutes = prefs.getInt("duration_minutes", 0)

        println("SessionViewModel: Checking timer - completionTime: $completionTime, currentTime: ${System.currentTimeMillis()}")

        if (completionTime > 0 && topicId != -1) {
            val currentTime = System.currentTimeMillis()

            // If current time is past completion time
            if (currentTime >= completionTime) {
                println("SessionViewModel: Timer should have completed, marking topic as complete")
                try {
                    // Get task and subject info
                    taskRepository.getTaskById(topicId)?.let { task ->
                        subjectRepository.getSubjectById(subjectId)?.let { subject ->
                            // Save session with proper subject and topic names
                            sessionRepository.insertSession(
                                Session(
                                    sessionSubjectId = subjectId,
                                    relatedToSubject = subject.name,
                                    topicName = task.title,
                                    date = Instant.now().toEpochMilli(),
                                    duration = durationMinutes.toLong()
                                )
                            )

                            // Show completion message
                            _snackbarEventFlow.emit(
                                SnackbarEvent.ShowSnackbar(
                                    message = "Previous study session completed. Topic marked as complete.",
                                    duration = SnackbarDuration.Long
                                )
                            )
                            println("SessionViewModel: Timer completion processed successfully")
                        }
                    }
                } catch (e: Exception) {
                    println("SessionViewModel: Error processing timer completion: ${e.message}")
                    e.printStackTrace()
                }
                
                // Clear timer state after completion
                prefs.edit().clear().apply()
                println("SessionViewModel: Timer state cleared")
            } else {
                println("SessionViewModel: Timer not yet completed")
            }
        } else {
            println("SessionViewModel: No pending timer found")
        }
    }

    suspend fun getTaskById(taskId: Int): Task? {
        return taskRepository.getTaskById(taskId)
    }

    fun onEvent(event: SessionEvent) {
        when(event) {
            is SessionEvent.OnTopicSelect -> {
                _state.update { 
                    it.copy(
                        selectedTopicId = event.task?.taskId,
                        selectedDuration = event.task?.taskDuration ?: 0
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
                        state.value.selectedTopicId?.let { topicId ->
                            taskRepository.getTaskById(topicId)?.let { task ->
                                sessionRepository.insertSession(
                                    session = Session(
                                        sessionSubjectId = state.value.subjectId ?: -1,
                                        relatedToSubject = state.value.relatedToSubject ?: "",
                                        topicName = task.title,
                                        date = Instant.now().toEpochMilli(),
                                        duration = state.value.selectedDuration.toLong()
                                    )
                                )

                                taskRepository.upsertTask(
                                    task.copy(isComplete = true)
                                )

                                _snackbarEventFlow.emit(
                                    SnackbarEvent.ShowSnackbar(message = "Session saved and topic marked as complete")
                                )
                            }
                        }
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
            is SessionEvent.InitializeWithTopic -> {
                viewModelScope.launch {
                    taskRepository.getTaskById(event.topicId)?.let { task ->
                        subjectRepository.getSubjectById(task.taskSubjectId)?.let { subject ->
                            _state.update { 
                                it.copy(
                                    selectedTopicId = task.taskId,
                                    subjectId = subject.subjectId,
                                    relatedToSubject = subject.name,
                                    selectedDuration = task.taskDuration
                                )
                            }
                        }
                    }
                }
            }
            is SessionEvent.CompleteTask -> {
                viewModelScope.launch {
                    taskRepository.getTaskById(event.taskId)?.let { task ->
                        taskRepository.upsertTask(
                            task.copy(isComplete = true)
                        )
                    }
                }
            }
            is SessionEvent.CancelSession -> {
                // Reset states without saving or marking complete
                _state.update { 
                    it.copy(
                        selectedTopicId = null,
                        selectedDuration = 0
                    )
                }
            }
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