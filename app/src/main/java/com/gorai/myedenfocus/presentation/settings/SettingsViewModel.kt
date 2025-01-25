package com.gorai.myedenfocus.presentation.settings

import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorai.myedenfocus.domain.repository.PreferencesRepository
import com.gorai.myedenfocus.util.SnackbarEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
): ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    private val _snackbarEventFlow = MutableSharedFlow<SnackbarEvent>()
    val snackbarEventFlow = _snackbarEventFlow.asSharedFlow()

    init {
        viewModelScope.launch {
            // Collect theme preferences
            preferencesRepository.themeFlow.collect { theme ->
                _state.update { it.copy(theme = theme) }
            }
        }

        viewModelScope.launch {
            // Collect daily study goal
            preferencesRepository.dailyStudyGoalFlow.collect { goal ->
                _state.update { it.copy(dailyStudyGoal = goal) }
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        when(event) {
            is SettingsEvent.OnDailyStudyGoalChange -> {
                _state.update { it.copy(dailyStudyGoal = event.hours) }
            }
            is SettingsEvent.SaveDailyStudyGoal -> saveDailyStudyGoal()
            is SettingsEvent.SetTheme -> {
                viewModelScope.launch {
                    try {
                        preferencesRepository.saveTheme(event.theme)
                        _state.update { it.copy(theme = event.theme) }
                        _snackbarEventFlow.emit(
                            SnackbarEvent.ShowSnackbar("Theme updated successfully")
                        )
                    } catch (e: Exception) {
                        _snackbarEventFlow.emit(
                            SnackbarEvent.ShowSnackbar(
                                "Couldn't save theme preference. ${e.message}",
                                SnackbarDuration.Long
                            )
                        )
                    }
                }
            }
            is SettingsEvent.ToggleNotifications -> {
                viewModelScope.launch {
                    val newState = !state.value.notificationsEnabled
                    preferencesRepository.saveNotificationsEnabled(newState)
                    _state.update { it.copy(notificationsEnabled = newState) }
                }
            }
            is SettingsEvent.SetReminderTime -> {
                viewModelScope.launch {
                    preferencesRepository.saveReminderTime(event.time)
                    _state.update { it.copy(reminderTime = event.time) }
                }
            }
        }
    }

    private fun saveDailyStudyGoal() {
        viewModelScope.launch {
            try {
                preferencesRepository.saveDailyStudyGoal(state.value.dailyStudyGoal)
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