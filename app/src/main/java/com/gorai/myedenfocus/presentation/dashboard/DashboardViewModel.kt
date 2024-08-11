package com.gorai.myedenfocus.presentation.dashboard

import androidx.compose.ui.graphics.Path.Companion.combine
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorai.myedenfocus.domain.repository.SessionRepository
import com.gorai.myedenfocus.domain.repository.SubjectRepository
import com.gorai.myedenfocus.util.toHours
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository
    private val sessionRepository: SessionRepository
): ViewModel() {
    private val _state = MutableStateFlow(DashboardState())
    val state = combine(
        _state,
        subjectRepository.getTotalSubjectCount(),
        subjectRepository.getTotalGoalHours(),
        subjectRepository.getAllSubjects(),
        sessionRepository.getTotalSessionsDuration()
        ) { state, subjectCount, goalHours, subjects, totalSessionDuration -> state.copy(
            totalSubjectCount = subjectCount,
            totalGoalStudyHours = goalHours.toString(),
            subjects = subjects,
            totalStudiedHours = totalSessionDuration.toHours().toString()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )
    fun onEvent(event: DashboardEvent) {
        when(event) {
            DashboardEvent.DeleteSubject -> TODO()
            is DashboardEvent.OnDeleteSessionButtonClick -> TODO()
            is DashboardEvent.OnGoalStudyHoursChange -> {
                _state.update {
                    
                }
            }
            is DashboardEvent.OnSubjectCardColorChange -> TODO()
            is DashboardEvent.OnSubjectNameChange -> TODO()
            is DashboardEvent.OnTaskIsCompleteChange -> TODO()
            DashboardEvent.SaveSubject -> TODO()
        }
    }
}