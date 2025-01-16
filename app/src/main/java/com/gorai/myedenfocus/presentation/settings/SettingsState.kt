package com.gorai.myedenfocus.presentation.settings

data class SettingsState(
    val dailyStudyGoal: String = "",
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val reminderTime: String = "09:00",
) 