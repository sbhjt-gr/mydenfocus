package com.gorai.myedenfocus.presentation.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor() : ViewModel() {
    var currentRoute by mutableStateOf("schedule")
        private set

    fun navigateTo(route: String) {
        currentRoute = route
    }
} 