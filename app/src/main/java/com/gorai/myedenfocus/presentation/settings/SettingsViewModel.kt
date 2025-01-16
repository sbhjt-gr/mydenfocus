package com.gorai.myedenfocus.presentation.settings

import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorai.myedenfocus.util.SnackbarEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
): ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    private val _snackbarEventFlow = MutableSharedFlow<SnackbarEvent>()
    val snackbarEventFlow = _snackbarEventFlow.asSharedFlow()

    fun onEvent(event: SettingsEvent) {
        when(event) {
            is SettingsEvent.OnDailyStudyGoalChange -> {
                _state.update { it.copy(dailyStudyGoal = event.hours) }
            }
            SettingsEvent.SaveDailyStudyGoal -> saveDailyStudyGoal()
            is SettingsEvent.ToggleTheme -> {
                _state.update { it.copy(isDarkMode = event.isDark) }
                // TODO: Save theme preference to DataStore
            }
            SettingsEvent.ToggleNotifications -> {
                _state.update { it.copy(notificationsEnabled = !state.value.notificationsEnabled) }
                // TODO: Handle notification permission and scheduling
            }
            is SettingsEvent.SetReminderTime -> {
                _state.update { it.copy(reminderTime = event.time) }
                // TODO: Update notification schedule if enabled
            }
        }
    }

    private fun saveDailyStudyGoal() {
        viewModelScope.launch {
            try {
                // TODO: Save to persistent storage
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
} 