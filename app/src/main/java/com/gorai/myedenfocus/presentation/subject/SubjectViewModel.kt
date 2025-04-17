package com.gorai.myedenfocus.presentation.subject

import androidx.compose.material3.SnackbarDuration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorai.myedenfocus.domain.model.Subject
import com.gorai.myedenfocus.domain.model.Task
import com.gorai.myedenfocus.domain.repository.SessionRepository
import com.gorai.myedenfocus.domain.repository.SubjectRepository
import com.gorai.myedenfocus.domain.repository.TaskRepository
import com.gorai.myedenfocus.presentation.navArgs
import com.gorai.myedenfocus.presentation.session.StudySessionTimerService
import com.gorai.myedenfocus.util.SnackbarEvent
import com.gorai.myedenfocus.util.minutesToHours
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SubjectViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    private val navArgs: SubjectScreenNavArgs = savedStateHandle.navArgs()
    private val _state = MutableStateFlow(SubjectState(currentSubjectId = navArgs.subjectId))
    private val _snackbarEventFlow = MutableSharedFlow<SnackbarEvent>()
    val snackbarEventFlow = _snackbarEventFlow.asSharedFlow()

    private val _refreshTrigger = MutableStateFlow(0)

    private val upcomingTasks = combine(
        taskRepository.getUpcomingTasksForSubject(navArgs.subjectId),
        _refreshTrigger
    ) { tasks, _ -> tasks }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val completedTasks = combine(
        taskRepository.getCompletedTasksForSubject(navArgs.subjectId),
        _refreshTrigger
    ) { tasks, _ -> tasks }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val recentSessions = combine(
        sessionRepository.getRecentTenSessionsForSubject(navArgs.subjectId),
        _refreshTrigger
    ) { sessions, _ -> sessions }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val totalDuration = combine(
        sessionRepository.getTotalSessionsDurationBySubject(navArgs.subjectId),
        _refreshTrigger,
        recentSessions
    ) { duration, _, _ -> duration }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0f
        )

    private val allSubjects = combine(
        subjectRepository.getAllSubjects(),
        _refreshTrigger
    ) { subjects, _ -> subjects }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Create a separate flow for subject details
    private val subjectDetails = combine(
        flow {
            val subject = subjectRepository.getSubjectById(navArgs.subjectId)
            emit(subject)
        },
        _refreshTrigger
    ) { subject, _ -> subject }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val state: StateFlow<SubjectState> = combine(
        subjectDetails,
        upcomingTasks,
        completedTasks,
        recentSessions,
        totalDuration
    ) { subject, upcoming, completed, sessions, duration ->
        Pair(
            Triple(subject, upcoming, completed),
            Pair(sessions, duration)
        )
    }.combine(allSubjects) { (subjectData, sessionsData), subjects ->
        Triple(subjectData, sessionsData, subjects)
    }.combine(_state) { (subjectData, sessionsData, subjects), currentState ->
        val (subject, upcoming, completed) = subjectData
        val (sessions, duration) = sessionsData
        
        currentState.copy(
            currentSubjectId = subject?.subjectId,
            subjectName = subject?.name ?: "",
            goalStudyHours = subject?.goalHours?.toString() ?: "",
            subjectCardColors = subject?.colors?.map { Color(it) } ?: Subject.subjectCardColors.random(),
            upcomingTasks = upcoming,
            completedTasks = completed,
            recentSessions = sessions,
            studiedHours = String.format("%.1f", duration),
            allSubjects = subjects,
            dailyStudyGoal = "8"
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SubjectState(currentSubjectId = navArgs.subjectId)
    )

    init {
        fetchSubject()
        updateProgress()
    }

    fun onEvent(event: SubjectEvent) {
        when(event) {
            SubjectEvent.UpdateSubject -> updateSubject()
            SubjectEvent.DeleteSession -> deleteSession()
            SubjectEvent.DeleteSubject -> deleteSubject()
            is SubjectEvent.OnDeleteSessionButtonClick -> {
                _state.update { it.copy(session = event.session) }
            }
            is SubjectEvent.OnGoalStudyHoursChange -> {
                _state.update { it.copy(goalStudyHours = event.hours) }
                updateProgress()
            }
            is SubjectEvent.OnSubjectCardColorChange -> {
                _state.update { it.copy(subjectCardColors = event.colors) }
            }
            is SubjectEvent.OnSubjectNameChange -> {
                _state.update { it.copy(subjectName = event.name) }
            }
            is SubjectEvent.OnTaskIsCompletedChange -> updateTask(event.task)
            SubjectEvent.UpdateProgress -> updateProgress()
            is SubjectEvent.OnSubjectDaysPerWeekChange -> {
                _state.update { it.copy(subjectDaysPerWeek = event.days) }
            }
        }
    }

    private fun fetchSubject() {
        viewModelScope.launch {
            try {
                val subject = withContext(Dispatchers.IO) {
                    subjectRepository.getSubjectById(navArgs.subjectId)
                }
                
                if (subject == null) {
                    _snackbarEventFlow.emit(
                        SnackbarEvent.ShowSnackbar(
                            "Subject not found",
                            SnackbarDuration.Long
                        )
                    )
                    _snackbarEventFlow.emit(SnackbarEvent.NavigateUp)
                    return@launch
                }

                // Get total duration for this subject
                sessionRepository.getTotalSessionsDurationBySubject(subject.subjectId ?: 0)
                    .collect { totalDuration ->
                        _state.update { currentState ->
                            currentState.copy(
                                currentSubjectId = subject.subjectId,
                                subjectName = subject.name,
                                studiedHours = String.format("%.1f", totalDuration),
                                goalStudyHours = subject.goalHours.toString(),
                                subjectCardColors = subject.colors.map { Color(it) }
                            )
                        }
                        updateProgress()
                    }
            } catch (e: Exception) {
                _snackbarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        "Error loading subject: ${e.message}",
                        SnackbarDuration.Long
                    )
                )
            }
        }
    }

    private fun deleteSubject() {
        viewModelScope.launch {
            try {
                state.value.currentSubjectId?.let { id ->
                    withContext(Dispatchers.IO) {
                        subjectRepository.deleteSubject(id)
                    }
                    _snackbarEventFlow.emit(SnackbarEvent.ShowSnackbar("Subject deleted successfully"))
                    _snackbarEventFlow.emit(SnackbarEvent.NavigateUp)
                }
            } catch (e: Exception) {
                _snackbarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        "Couldn't delete subject. ${e.message}",
                        SnackbarDuration.Long
                    )
                )
            }
        }
    }

    private fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                taskRepository.upsertTask(task.copy(isComplete = !task.isComplete))
                _snackbarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        message = if (task.isComplete) "Topic marked as complete" else "Topic marked as incomplete"
                    )
                )
            } catch (e: Exception) {
                _snackbarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        "Couldn't update task. ${e.message}",
                        SnackbarDuration.Long
                    )
                )
            }
        }
    }

    private fun deleteSession() {
        viewModelScope.launch {
            try {
                state.value.session?.let {
                    sessionRepository.deleteSession(it)
                    _snackbarEventFlow.emit(SnackbarEvent.ShowSnackbar("Session deleted successfully"))
                }
            } catch (e: Exception) {
                _snackbarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        "Couldn't delete session. ${e.message}",
                        SnackbarDuration.Long
                    )
                )
            }
        }
    }

    private fun updateProgress() {
        val goalStudyHours = state.value.goalStudyHours.toFloatOrNull() ?: 1f
        val studiedHours = state.value.studiedHours.toFloatOrNull() ?: 0f
        _state.update { 
            it.copy(progress = (studiedHours / goalStudyHours).coerceIn(0f, 1f))
        }
    }

    private fun updateSubject() {
        viewModelScope.launch {
            try {
                subjectRepository.upsertSubject(
                    Subject(
                        subjectId = state.value.currentSubjectId,
                        name = state.value.subjectName,
                        goalHours = state.value.goalStudyHours.toFloatOrNull() ?: 1f,
                        colors = state.value.subjectCardColors.map { it.toArgb() }
                    )
                )
                _snackbarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar("Subject updated successfully")
                )
            } catch (e: Exception) {
                _snackbarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        "Couldn't update subject. ${e.message}",
                        SnackbarDuration.Long
                    )
                )
            }
        }
    }

    fun updateStudiedHours(additionalHours: Float) {
        viewModelScope.launch {
            val currentStudiedHours = state.value.studiedHours.toFloatOrNull() ?: 0f
            val newStudiedHours = currentStudiedHours + additionalHours
            
            _state.update { currentState ->
                currentState.copy(
                    studiedHours = String.format("%.1f", newStudiedHours)
                )
            }
            updateProgress()
        }
    }

    // Function to update progress based on elapsed seconds
    fun updateElapsedTime(elapsedSeconds: Int) {
        viewModelScope.launch {
            val elapsedHours = elapsedSeconds / 3600f
            _state.update { currentState ->
                val baseHours = currentState.studiedHours.toFloatOrNull() ?: 0f
                currentState.copy(
                    studiedHours = String.format("%.1f", baseHours + elapsedHours)
                )
            }
            updateProgress()
        }
    }

    // Function to trigger refresh
    fun refreshData() {
        viewModelScope.launch {
            _refreshTrigger.value += 1
        }
    }

    fun collectTimerUpdates(timerService: StudySessionTimerService) {
        viewModelScope.launch {
            timerService.elapsedTimeFlow.collect { elapsedSeconds ->
                if (elapsedSeconds > 0) {
                    val elapsedHours = elapsedSeconds / 3600f
                    _state.update { currentState ->
                        val studiedHours = currentState.studiedHours.toFloatOrNull() ?: 0f
                        currentState.copy(
                            studiedHours = String.format("%.1f", studiedHours + elapsedHours)
                        )
                    }
                    updateProgress()
                }
            }
        }
    }
}