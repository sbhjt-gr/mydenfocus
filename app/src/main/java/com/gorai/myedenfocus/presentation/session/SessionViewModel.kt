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
    private val _refreshTrigger = MutableStateFlow(0)
    
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
        val wasSessionSaved = prefs.getBoolean("session_saved", false)
        val currentTime = System.currentTimeMillis()

        // Clear any stale preferences that are too old (more than 24 hours)
        if (completionTime > 0 && currentTime - completionTime > 24 * 60 * 60 * 1000) {
            prefs.edit().clear().apply()
            return
        }

        // Only proceed if we have valid data and session wasn't already saved
        if (completionTime > 0 && topicId != -1 && !wasSessionSaved && currentTime >= completionTime) {
            // Load topic and subject details
            taskRepository.getTaskById(topicId)?.let { task ->
                subjectRepository.getSubjectById(subjectId)?.let { subject ->
                    _state.update { it.copy(
                        selectedTopicId = task.taskId,
                        selectedSubjectId = subject.subjectId,
                        subjectId = subject.subjectId,
                        relatedToSubject = subject.name,
                        selectedDuration = task.taskDuration
                    )}

                    try {
                        // Save session with proper subject and topic names
                        sessionRepository.insertSession(
                            Session(
                                sessionSubjectId = subjectId,
                                relatedToSubject = subject.name,
                                topicName = task.title,
                                startTime = completionTime - (durationMinutes * 60 * 1000),
                                endTime = completionTime,
                                duration = durationMinutes.toLong(),
                                plannedDuration = durationMinutes.toLong(),
                                wasCompleted = true
                            )
                        )

                        // Mark that session was saved
                        prefs.edit().putBoolean("session_saved", true).apply()
                    } catch (e: Exception) {
                        println("SessionViewModel: Error processing timer completion: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        }
        
        // Always clear preferences after checking
        prefs.edit().clear().apply()
    }

    suspend fun getTaskById(taskId: Int): Task? {
        return taskRepository.getTaskById(taskId)
    }

    fun onEvent(event: SessionEvent) {
        when(event) {
            is SessionEvent.OnTopicSelect -> {
                viewModelScope.launch {
                    _state.update { it.copy(selectedTopicId = event.task?.taskId) }
                    event.task?.let { task ->
                        _state.update { it.copy(
                            selectedDuration = task.taskDuration,
                            subjectId = task.taskSubjectId
                        )}
                    }
                }
            }
            is SessionEvent.OnRelatedSubjectChange -> {
                _state.update { it.copy(
                    relatedToSubject = event.subject.name,
                    subjectId = event.subject.subjectId
                )}
            }
            is SessionEvent.OnDeleteSessionButtonClick -> {
                _state.update { it.copy(session = event.session) }
            }
            SessionEvent.DeleteSession -> deleteSession()
            SessionEvent.SaveSession -> saveSession()
            SessionEvent.CancelSession -> cancelSession()
            is SessionEvent.CompleteTask -> completeTask(event.taskId)
            is SessionEvent.InitializeWithTopic -> {
                viewModelScope.launch {
                    taskRepository.getTaskById(event.topicId)?.let { task ->
                        subjectRepository.getSubjectById(task.taskSubjectId)?.let { subject ->
                            _state.update { 
                                it.copy(
                                    selectedTopicId = task.taskId,
                                    subjectId = task.taskSubjectId,
                                    selectedDuration = task.taskDuration,
                                    relatedToSubject = subject.name
                                )
                            }
                        }
                    }
                }
            }
            SessionEvent.RefreshScreen -> refreshData()
            SessionEvent.CheckSubjectId -> notifyToUpdateSubject()
            is SessionEvent.OnDurationSelected -> {
                _state.update { it.copy(selectedDuration = event.minutes) }
            }
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            _refreshTrigger.value = _refreshTrigger.value + 1
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

    private fun saveSession() {
        viewModelScope.launch {
            try {
                // Check if session was already saved by timer service
                val prefs = context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
                val wasSessionSaved = prefs.getBoolean("session_saved", false)
                val completionTime = prefs.getLong("completion_time", 0)
                
                // Only save if there was no timer running (manual save) or if timer exists but session wasn't saved
                if (completionTime == 0L || !wasSessionSaved) {
                    state.value.selectedTopicId?.let { topicId ->
                        taskRepository.getTaskById(topicId)?.let { task ->
                            sessionRepository.insertSession(
                                session = Session(
                                    sessionSubjectId = state.value.subjectId ?: -1,
                                    relatedToSubject = state.value.relatedToSubject ?: "",
                                    topicName = task.title,
                                    startTime = System.currentTimeMillis() - (state.value.selectedDuration * 60 * 1000),
                                    endTime = System.currentTimeMillis(),
                                    duration = state.value.selectedDuration.toLong(),
                                    plannedDuration = state.value.selectedDuration.toLong(),
                                    wasCompleted = true
                                )
                            )

                            taskRepository.upsertTask(
                                task.copy(isComplete = true)
                            )

                            // Mark session as saved in preferences if timer exists
                            if (completionTime > 0) {
                                prefs.edit().putBoolean("session_saved", true).apply()
                            }

                            _snackbarEventFlow.emit(
                                SnackbarEvent.ShowSnackbar(message = "Session saved and topic marked as complete")
                            )
                        }
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

    private fun cancelSession() {
        // Reset states without saving or marking complete
        _state.update { 
            it.copy(
                selectedTopicId = null,
                selectedDuration = 0
            )
        }
    }

    private fun completeTask(taskId: Int) {
        viewModelScope.launch {
            taskRepository.getTaskById(taskId)?.let { task ->
                taskRepository.upsertTask(
                    task.copy(isComplete = true)
                )
            }
        }
    }
}