package com.gorai.myedenfocus.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorai.myedenfocus.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _shouldNavigateNext = MutableStateFlow(false)
    val shouldNavigateNext = _shouldNavigateNext.asStateFlow()

    fun onNextClick() {
        viewModelScope.launch {
            userPreferencesRepository.setOnboardingCompleted(true)
            _shouldNavigateNext.value = true
        }
    }

    fun onPageChange(page: Int) {
        _shouldNavigateNext.value = false
    }

    fun onPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setNotificationPermissionRequested(true)
        }
    }

    fun resetNavigationFlag() {
        _shouldNavigateNext.value = false
    }
} 