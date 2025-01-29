package com.gorai.myedenfocus.domain.repository

import androidx.datastore.preferences.core.*
import com.gorai.myedenfocus.data.local.DataStoreProvider
import com.gorai.myedenfocus.domain.model.Theme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(
    private val dataStoreProvider: DataStoreProvider
) {
    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val DAILY_STUDY_GOAL = stringPreferencesKey("daily_study_goal")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val REMINDER_TIME = stringPreferencesKey("reminder_time")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val NOTIFICATION_PERMISSION_REQUESTED = booleanPreferencesKey("notification_permission_requested")
    }

    val themeFlow: Flow<Theme> = dataStoreProvider.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeName = preferences[PreferencesKeys.THEME] ?: Theme.SYSTEM.name
            try {
                Theme.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                Theme.SYSTEM
            }
        }

    val dailyStudyGoalFlow: Flow<String> = dataStoreProvider.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.DAILY_STUDY_GOAL] ?: "2"
        }

    val notificationsEnabledFlow: Flow<Boolean> = dataStoreProvider.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: false
        }

    val reminderTimeFlow: Flow<String> = dataStoreProvider.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.REMINDER_TIME] ?: "09:00"
        }

    val isOnboardingCompleted: Flow<Boolean> = dataStoreProvider.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        }

    val isNotificationPermissionRequested: Flow<Boolean> = dataStoreProvider.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_PERMISSION_REQUESTED] ?: false
        }

    suspend fun saveTheme(theme: Theme) {
        dataStoreProvider.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    suspend fun saveDailyStudyGoal(hours: String) {
        dataStoreProvider.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_STUDY_GOAL] = hours
        }
    }

    suspend fun saveNotificationsEnabled(enabled: Boolean) {
        dataStoreProvider.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun saveReminderTime(time: String) {
        dataStoreProvider.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMINDER_TIME] = time
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStoreProvider.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setNotificationPermissionRequested(requested: Boolean) {
        dataStoreProvider.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_PERMISSION_REQUESTED] = requested
        }
    }
} 