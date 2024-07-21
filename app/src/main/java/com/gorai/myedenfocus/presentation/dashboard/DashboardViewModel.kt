package com.gorai.myedenfocus.presentation.dashboard

import androidx.lifecycle.ViewModel
import com.gorai.myedenfocus.domain.repository.SubjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository
): ViewModel() {
}