package com.gorai.myedenfocus.presentation.settings

sealed interface SettingsEvent {
    data class OnDailyStudyGoalChange(val hours: String): SettingsEvent
    object SaveDailyStudyGoal : SettingsEvent
    data class ToggleTheme(val isDark: Boolean) : SettingsEvent
    object ToggleNotifications : SettingsEvent
    data class SetReminderTime(val time: String) : SettingsEvent
} 