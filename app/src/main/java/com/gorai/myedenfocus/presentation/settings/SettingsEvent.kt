package com.gorai.myedenfocus.presentation.settings

sealed interface SettingsEvent {
    data class OnDailyStudyGoalChange(val hours: String): SettingsEvent
    object SaveDailyStudyGoal : SettingsEvent
} 