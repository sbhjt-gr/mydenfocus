package com.gorai.myedenfocus.presentation.settings

import com.gorai.myedenfocus.domain.model.Theme

sealed interface SettingsEvent {
    data class OnDailyStudyGoalChange(val hours: String): SettingsEvent
    object SaveDailyStudyGoal : SettingsEvent
    object ToggleNotifications : SettingsEvent
    data class SetReminderTime(val time: String) : SettingsEvent
    data class SetTheme(val theme: Theme) : SettingsEvent
} 