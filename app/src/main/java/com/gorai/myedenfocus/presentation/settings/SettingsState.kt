package com.gorai.myedenfocus.presentation.settings

import com.gorai.myedenfocus.domain.model.Theme

data class SettingsState(
    val dailyStudyGoal: String = "",
    val notificationsEnabled: Boolean = false,
    val reminderTime: String = "09:00",
    val theme: Theme = Theme.SYSTEM,
) 