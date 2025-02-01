package com.gorai.myedenfocus.presentation.dashboard

import androidx.compose.material3.SnackbarDuration
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorai.myedenfocus.domain.model.Session
import com.gorai.myedenfocus.domain.model.Subject
import com.gorai.myedenfocus.domain.model.Task
import com.gorai.myedenfocus.domain.repository.SessionRepository
import com.gorai.myedenfocus.domain.repository.SubjectRepository
import com.gorai.myedenfocus.domain.repository.TaskRepository
import com.gorai.myedenfocus.util.SnackbarEvent
import com.gorai.myedenfocus.util.toHours
import com.gorai.myedenfocus.data.local.PreferencesDataStore
import com.gorai.myedenfocus.presentation.session.StudySessionTimerService
import com.gorai.myedenfocus.util.minutesToHours
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val sessionRepository: SessionRepository,
    private val taskRepository: TaskRepository,
    private val preferencesDataStore: PreferencesDataStore
): ViewModel() {
    private val _state = MutableStateFlow(DashboardState())
    
    private val subjectCount = subjectRepository.getTotalSubjectCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    
    private val subjects = subjectRepository.getAllSubjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
    private val totalSessionDuration = sessionRepository.getTotalSessionsDuration()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0f
        )
        
    private val todaySessionDuration = sessionRepository.getTodaySessionsDuration()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0f
        )

    val state: StateFlow<DashboardState> = combine(
        _state,
        subjectCount,
        subjects,
        totalSessionDuration,
        todaySessionDuration
    ) { state, count, subjectList, totalDuration, todayDuration ->
        state.copy(
            totalSubjectCount = count,
            subjects = subjectList,
            totalStudiedHours = totalDuration.toString(),
            dailyStudiedHours = todayDuration.toString(),
            dailyStudyGoal = state.dailyStudyGoal
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )
    val tasks: StateFlow<List<Task>> = taskRepository.getAllUpcomingTasks().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val recentSessions: StateFlow<List<Session>> = sessionRepository.getRecentFiveSessions(subjectId = 1).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    private val _snackbarEventFlow = MutableSharedFlow<SnackbarEvent>()
    val snackbarEventFlow = _snackbarEventFlow.asSharedFlow()

    private var isSavingSubject = false

    init {
        viewModelScope.launch {
            preferencesDataStore.dailyStudyGoal.collect { hours ->
                _state.update { it.copy(dailyStudyGoal = hours) }
            }
        }
    }

    fun onEvent(event: DashboardEvent) {
        when(event) {
            DashboardEvent.SaveSubject -> saveSubject()
            DashboardEvent.DeleteSubject -> deleteSubject()
            DashboardEvent.DeleteSession -> deleteSession()
            DashboardEvent.SaveDailyStudyGoal -> {
                viewModelScope.launch {
                    try {
                        preferencesDataStore.saveDailyStudyGoal(state.value.dailyStudyGoal)
                        _snackbarEventFlow.emit(
                            SnackbarEvent.ShowSnackbar("Daily study goal saved successfully")
                        )
                    } catch (e: Exception) {
                        _snackbarEventFlow.emit(
                            SnackbarEvent.ShowSnackbar(
                                "Couldn't save daily study goal. ${e.message}",
                                SnackbarDuration.Long
                            )
                        )
                    }
                }
            }
            is DashboardEvent.OnDeleteSessionButtonClick -> {
                _state.update { it.copy(session = event.session) }
            }
            is DashboardEvent.OnGoalStudyHoursChange -> {
                _state.update { it.copy(goalStudyHours = event.hours) }
            }
            is DashboardEvent.OnSubjectCardColorChange -> {
                _state.update { it.copy(subjectCardColors = event.colors) }
            }
            is DashboardEvent.OnSubjectNameChange -> {
                _state.update { it.copy(subjectName = event.name) }
            }
            is DashboardEvent.OnTaskIsCompleteChange -> updateTask(event.task)
            is DashboardEvent.OnDailyStudyGoalChange -> {
                _state.update { it.copy(dailyStudyGoal = event.goal) }
            }
            is DashboardEvent.OnStudyDaysPerWeekChange -> {
                _state.update { it.copy(studyDaysPerWeek = event.days) }
            }
            is DashboardEvent.OnSubjectDaysPerWeekChange -> {
                _state.update { it.copy(subjectDaysPerWeek = event.days) }
            }
        }
    }

    private fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                taskRepository.upsertTask(
                    task = task.copy(
                        isComplete = !task.isComplete
                    )
                )
                _snackbarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        "Topic saved in completed topics"
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

    private fun saveSubject() {
        viewModelScope.launch {
            try {
                subjectRepository.upsertSubject(
                    subject =  Subject(
                        name = state.value.subjectName,
                        goalHours = state.value.goalStudyHours.toFloatOrNull() ?: 1f,
                        colors = state.value.subjectCardColors.map { it.toArgb() }
                    )
                )
                _state.update {
                    it.copy(
                        subjectName = "",
                        goalStudyHours = "",
                        subjectCardColors = Subject.subjectCardColors.random()
                    )
                }
            } catch (e: Exception) {
                _snackbarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        "Couldn't save subject. ${e.message}",
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

    private fun deleteSubject() {
        viewModelScope.launch {
            try {
                state.value.currentSubjectId?.let { id ->
                    subjectRepository.deleteSubject(id)
                    _snackbarEventFlow.emit(
                        SnackbarEvent.ShowSnackbar("Subject deleted successfully")
                    )
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

    fun updateStudiedHours(additionalHours: Float) {
        _state.update { currentState ->
            val currentDailyHours = currentState.dailyStudiedHours.toFloatOrNull() ?: 0f
            val currentTotalHours = currentState.totalStudiedHours.toFloatOrNull() ?: 0f
            
            currentState.copy(
                dailyStudiedHours = (currentDailyHours + additionalHours).toString(),
                totalStudiedHours = (currentTotalHours + additionalHours).toString()
            )
        }
    }

    fun collectTimerUpdates(timerService: StudySessionTimerService) {
        viewModelScope.launch {
            timerService.elapsedTimeFlow.collect { elapsedSeconds ->
                if (elapsedSeconds > 0) {
                    val elapsedHours = elapsedSeconds / 3600f
                    _state.update { currentState ->
                        val dailyHours = currentState.dailyStudiedHours.toFloatOrNull() ?: 0f
                        val totalHours = currentState.totalStudiedHours.toFloatOrNull() ?: 0f
                        currentState.copy(
                            dailyStudiedHours = String.format("%.1f", dailyHours + elapsedHours),
                            totalStudiedHours = String.format("%.1f", totalHours + elapsedHours)
                        )
                    }
                }
            }
        }
    }
}