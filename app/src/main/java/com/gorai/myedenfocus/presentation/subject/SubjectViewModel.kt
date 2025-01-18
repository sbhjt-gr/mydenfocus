package com.gorai.myedenfocus.presentation.subject

import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorai.myedenfocus.domain.model.Session
import com.gorai.myedenfocus.domain.model.Subject
import com.gorai.myedenfocus.domain.model.Task
import com.gorai.myedenfocus.domain.repository.SessionRepository
import com.gorai.myedenfocus.domain.repository.SubjectRepository
import com.gorai.myedenfocus.domain.repository.TaskRepository
import com.gorai.myedenfocus.presentation.navArgs
import com.gorai.myedenfocus.util.SnackbarEvent
import com.gorai.myedenfocus.util.toHours
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

    private val upcomingTasks = taskRepository.getUpcomingTasksForSubject(navArgs.subjectId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val completedTasks = taskRepository.getCompletedTasksForSubject(navArgs.subjectId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val recentSessions = sessionRepository.getRecentTenSessionsForSubject(navArgs.subjectId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val totalDuration = sessionRepository.getTotalSessionsDurationBySubject(navArgs.subjectId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    private val allSubjects = subjectRepository.getAllSubjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val state: StateFlow<SubjectState> = upcomingTasks
        .combine(completedTasks) { upcoming, completed -> 
            _state.value.copy(
                upcomingTasks = upcoming,
                completedTasks = completed
            )
        }
        .combine(recentSessions) { state, sessions ->
            state.copy(recentSessions = sessions)
        }
        .combine(totalDuration) { state, duration ->
            state.copy(studiedHours = duration.toHours())
        }
        .combine(allSubjects) { state, subjects ->
            state.copy(
                allSubjects = subjects,
                dailyStudyGoal = "8"
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SubjectState(currentSubjectId = navArgs.subjectId)
        )

    init {
        fetchSubject()
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
            subjectRepository.getSubjectById(navArgs.subjectId)?.let { subject ->
                _state.update {
                    it.copy(
                        subjectName = subject.name,
                        goalStudyHours = subject.goalHours.toString(),
                        subjectCardColors = subject.colors.map { Color(it) },
                        currentSubjectId = subject.subjectId
                    )
                }
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
        _state.update { 
            it.copy(progress = (state.value.studiedHours / goalStudyHours).coerceIn(0f, 1f))
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
}