package com.gorai.myedenfocus.presentation.chat

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorai.myedenfocus.service.GeminiService
import com.gorai.myedenfocus.util.DocumentParser
import com.gorai.myedenfocus.util.SnackbarEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val geminiService: GeminiService,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val TAG = "ChatViewModel"
    
    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state
    
    private val _snackbarEventFlow = MutableSharedFlow<SnackbarEvent>()
    val snackbarEventFlow = _snackbarEventFlow.asSharedFlow()
    
    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.OnMessageInputChange -> {
                _state.update { it.copy(inputMessage = event.message) }
            }
            is ChatEvent.OnSendMessage -> {
                sendMessage()
            }
            is ChatEvent.OnClearChat -> {
                _state.update { it.copy(messages = emptyList(), activeDocument = null) }
            }
            is ChatEvent.OnDocumentSelected -> {
                processDocument(event.uri)
            }
            is ChatEvent.OnClearDocument -> {
                _state.update { it.copy(activeDocument = null) }
            }
        }
    }
    
    private fun processDocument(uri: Uri) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting document processing for URI: $uri")
                _state.update { it.copy(isDocumentLoading = true) }
                
                // Get document name
                val documentName = getDocumentName(uri)
                Log.d(TAG, "Document name: $documentName")
                
                // Check file extension
                val extension = documentName.substringAfterLast('.', "").lowercase()
                val mimeType = context.contentResolver.getType(uri)
                Log.d(TAG, "File extension: $extension, MIME type: $mimeType")
                
                // Validate supported file types
                if (extension !in listOf("pdf", "docx", "doc", "txt") && 
                    !mimeType.toString().contains("pdf") && 
                    !mimeType.toString().contains("word") && 
                    !mimeType.toString().contains("text/plain")) {
                    
                    Log.e(TAG, "Unsupported file type: $extension, MIME: $mimeType")
                    _state.update { it.copy(isDocumentLoading = false) }
                    _snackbarEventFlow.emit(
                        SnackbarEvent.ShowSnackbar(
                            message = "Unsupported file type. Please upload PDF, DOCX, DOC, or TXT files."
                        )
                    )
                    return@launch
                }
                
                // Extract text content
                Log.d(TAG, "Extracting text content from document...")
                val documentContent = DocumentParser.extractTextFromDocument(context, uri)
                
                if (documentContent.isNullOrBlank()) {
                    Log.e(TAG, "Failed to extract text: Content is null or blank")
                    _state.update { it.copy(isDocumentLoading = false) }
                    _snackbarEventFlow.emit(
                        SnackbarEvent.ShowSnackbar(
                            message = "Failed to extract text from document. Please try another file."
                        )
                    )
                    return@launch
                }
                
                Log.d(TAG, "Successfully extracted ${documentContent.length} characters from document")
                
                // Create ChatDocument
                val chatDocument = ChatDocument(
                    uri = uri,
                    name = documentName,
                    content = documentContent
                )
                
                // Add system message indicating document was loaded
                val systemMessage = ChatMessage(
                    content = "Document loaded: $documentName (${formatFileSize(documentContent.length)})",
                    isFromUser = false
                )
                
                Log.d(TAG, "Updating state with document and system message")
                _state.update { 
                    it.copy(
                        activeDocument = chatDocument,
                        messages = it.messages + systemMessage,
                        isDocumentLoading = false
                    )
                }
                
                // Generate an initial summary of the document
                val prompt = "I've uploaded a document titled '$documentName'. Please provide a brief summary of its contents."
                _state.update { 
                    it.copy(
                        inputMessage = prompt
                    )
                }
                Log.d(TAG, "Document processing completed successfully")
            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("permission") == true -> 
                        "Permission denied. Please select a different file."
                    e.message?.contains("OutOfMemoryError") == true || e.toString().contains("OutOfMemoryError") -> 
                        "File is too large to process. Please try a smaller file."
                    e.message?.contains("timeout") == true -> 
                        "Operation timed out. File may be too large or complex."
                    e.message?.contains("PDFBox") == true || e.message?.contains("XWPF") == true || e.message?.contains("HWPF") == true -> 
                        "Error parsing document format. The file may be corrupted or password-protected."
                    else -> "Error processing document: ${e.message ?: "Unknown error"}"
                }
                
                Log.e(TAG, "Error processing document: ${e.message}", e)
                _state.update { it.copy(isDocumentLoading = false) }
                _snackbarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        message = errorMsg
                    )
                )
            }
        }
    }
    
    private fun getDocumentName(uri: Uri): String {
        var documentName = "Unknown Document"
        
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    documentName = cursor.getString(nameIndex)
                }
            }
        }
        
        return documentName
    }
    
    private fun formatFileSize(textLength: Int): String {
        return when {
            textLength < 1024 -> "$textLength bytes"
            textLength < 1024 * 1024 -> "${textLength / 1024} KB"
            else -> "${textLength / (1024 * 1024)} MB"
        }
    }
    
    private fun sendMessage() {
        val message = state.value.inputMessage.trim()
        if (message.isBlank()) return
        
        Log.d(TAG, "Sending message: ${message.take(20)}...")
        
        // Add user message to chat
        val userMessage = ChatMessage(
            content = message,
            isFromUser = true
        )
        
        _state.update { 
            it.copy(
                messages = it.messages + userMessage,
                inputMessage = "",
                isLoading = true,
                error = null // Clear previous errors
            ) 
        }
        
        viewModelScope.launch {
            try {
                // Convert previous messages to pairs for Gemini API
                val chatHistory = state.value.messages
                    .dropLast(1) // exclude the message we just added
                    .chunked(2)
                    .mapNotNull { chunk ->
                        if (chunk.size == 2 && chunk[0].isFromUser && !chunk[1].isFromUser) {
                            Pair(chunk[0].content, chunk[1].content)
                        } else null
                    }
                
                Log.d(TAG, "Calling GeminiService with ${chatHistory.size} previous exchanges")
                
                // Append document content to message if there's an active document
                val finalMessage = state.value.activeDocument?.let {
                    """
                    $message
                    
                    Document content:
                    ${it.content}
                    """.trimIndent()
                } ?: message
                
                val response = geminiService.sendChatMessage(finalMessage, chatHistory)
                Log.d(TAG, "Received response from GeminiService")
                
                // Add AI response to chat
                val aiMessage = ChatMessage(
                    content = response,
                    isFromUser = false
                )
                
                _state.update { 
                    it.copy(
                        messages = it.messages + aiMessage,
                        isLoading = false,
                        error = null
                    ) 
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing message", e)
                
                // Handle specific API key errors
                val errorMessage = when {
                    e.message?.contains("API ERROR") == true -> "API key issue: Please check API key configuration"
                    e.message?.contains("API key") == true -> "API Key issue: ${e.message}"
                    e.message?.contains("timeout") == true -> "Network timeout. Please check your connection."
                    e.message?.contains("Unable to resolve host") == true -> "Network connection error. Please check your internet."
                    else -> "Error: ${e.message}"
                }
                
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = errorMessage
                    ) 
                }
                
                // Add an error message to the chat
                val errorChatMessage = ChatMessage(
                    content = "Error: Unable to get response. $errorMessage",
                    isFromUser = false
                )
                
                _state.update {
                    it.copy(
                        messages = it.messages + errorChatMessage
                    )
                }
                
                _snackbarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        message = errorMessage,
                    )
                )
            }
        }
    }
} 