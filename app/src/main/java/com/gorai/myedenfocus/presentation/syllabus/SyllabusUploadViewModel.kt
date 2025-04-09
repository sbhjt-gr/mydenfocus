package com.gorai.myedenfocus.presentation.syllabus

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorai.myedenfocus.service.GeminiService
import com.gorai.myedenfocus.service.SubjectTopic
import com.gorai.myedenfocus.util.DocumentParser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyllabusUploadViewModel @Inject constructor(
    private val geminiService: GeminiService,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow(SyllabusUploadState())
    val state: StateFlow<SyllabusUploadState> = _state.asStateFlow()
    
    private val _events = MutableSharedFlow<SyllabusUploadEvent>()
    val events: SharedFlow<SyllabusUploadEvent> = _events.asSharedFlow()
    
    fun onDocumentSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }
                
                // Parse document text
                val documentText = DocumentParser.extractTextFromDocument(context, uri)
                if (documentText == null) {
                    _state.update { it.copy(isLoading = false, error = "Could not read document content") }
                    _events.emit(SyllabusUploadEvent.ShowError("Could not read document content"))
                    return@launch
                }
                
                // Extract subjects and topics using Gemini
                val subjectsAndTopics = geminiService.extractSubjectsAndTopics(documentText)
                
                if (subjectsAndTopics.isEmpty()) {
                    _state.update { it.copy(isLoading = false, error = "No subjects or topics found in document") }
                    _events.emit(SyllabusUploadEvent.ShowError("No subjects or topics found in document"))
                } else {
                    _state.update { 
                        it.copy(
                            isLoading = false, 
                            extractedSubjectsAndTopics = subjectsAndTopics,
                            error = null
                        ) 
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Unknown error occurred") }
                _events.emit(SyllabusUploadEvent.ShowError(e.message ?: "Unknown error occurred"))
            }
        }
    }
}

data class SyllabusUploadState(
    val isLoading: Boolean = false,
    val extractedSubjectsAndTopics: List<SubjectTopic> = emptyList(),
    val error: String? = null
)

sealed class SyllabusUploadEvent {
    data class ShowError(val message: String) : SyllabusUploadEvent()
} 