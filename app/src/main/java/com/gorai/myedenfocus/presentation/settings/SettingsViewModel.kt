package com.gorai.myedenfocus.presentation.settings

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorai.myedenfocus.domain.repository.PreferencesRepository
import com.gorai.myedenfocus.service.DailyStudyReminderService
import com.gorai.myedenfocus.util.SnackbarEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    @ApplicationContext private val context: Context
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

        viewModelScope.launch {
            // Collect notifications state
            preferencesRepository.notificationsEnabledFlow.collect { enabled ->
                _state.update { it.copy(notificationsEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            // Collect reminder time
            preferencesRepository.reminderTimeFlow.collect { time ->
                _state.update { it.copy(reminderTime = time) }
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
                    
                    if (newState) {
                        // Schedule notification with current reminder time
                        val (hour, minute) = state.value.reminderTime.split(":").map { it.toInt() }
                        DailyStudyReminderService.scheduleReminder(context, hour, minute)
                        _snackbarEventFlow.emit(
                            SnackbarEvent.ShowSnackbar("Daily study reminders enabled")
                        )
                    } else {
                        // Cancel scheduled notifications
                        DailyStudyReminderService.cancelReminder(context)
                        _snackbarEventFlow.emit(
                            SnackbarEvent.ShowSnackbar("Daily study reminders disabled")
                        )
                    }
                }
            }
            is SettingsEvent.SetReminderTime -> {
                viewModelScope.launch {
                    preferencesRepository.saveReminderTime(event.time)
                    _state.update { it.copy(reminderTime = event.time) }
                    
                    if (state.value.notificationsEnabled) {
                        // Reschedule notification with new time
                        val (hour, minute) = event.time.split(":").map { it.toInt() }
                        DailyStudyReminderService.scheduleReminder(context, hour, minute)
                        _snackbarEventFlow.emit(
                            SnackbarEvent.ShowSnackbar("Reminder time updated")
                        )
                    }
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