package com.gorai.myedenfocus.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesDataStore @Inject constructor(
    private val context: Context
) {
    private object PreferencesKeys {
        val DAILY_STUDY_GOAL = stringPreferencesKey("daily_study_goal")
    }

    val dailyStudyGoal: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DAILY_STUDY_GOAL] ?: ""
    }

    suspend fun saveDailyStudyGoal(hours: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_STUDY_GOAL] = hours
        }
    }
} 