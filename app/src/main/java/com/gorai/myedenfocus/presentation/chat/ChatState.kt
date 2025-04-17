package com.gorai.myedenfocus.presentation.chat

import android.net.Uri

data class ChatMessage(
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatDocument(
    val uri: Uri,
    val name: String,
    val content: String
)

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputMessage: String = "",
    val isLoading: Boolean = false,
    val isDocumentLoading: Boolean = false,
    val error: String? = null,
    val activeDocument: ChatDocument? = null
) 