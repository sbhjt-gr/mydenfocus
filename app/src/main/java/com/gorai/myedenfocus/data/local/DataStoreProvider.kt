package com.gorai.myedenfocus.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val dataStore: DataStore<Preferences> = context.dataStore

    companion object {
        private val Context.dataStore by preferencesDataStore(name = "user_preferences")
    }
} 