package com.gorai.myedenfocus.presentation.meditation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorai.myedenfocus.domain.model.MeditationSession
import com.gorai.myedenfocus.domain.repository.MeditationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class MeditationViewModel @Inject constructor(
    private val repository: MeditationRepository
) : ViewModel() {
    private val _sessions = MutableStateFlow<List<MeditationSession>>(emptyList())
    val sessions = _sessions.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getRecentSessions(7).collect {
                _sessions.value = it
            }
        }
    }

    fun addSession(duration: Int) {
        viewModelScope.launch {
            val session = MeditationSession(
                duration = duration,
                timestamp = LocalDateTime.now()
            )
            repository.insertSession(session)
        }
    }

    fun deleteSession(session: MeditationSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }
} 