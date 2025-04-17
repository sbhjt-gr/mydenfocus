package com.gorai.myedenfocus.presentation.chat

import android.net.Uri

sealed interface ChatEvent {
    data class OnMessageInputChange(val message: String) : ChatEvent
    object OnSendMessage : ChatEvent
    object OnClearChat : ChatEvent
    data class OnDocumentSelected(val uri: Uri) : ChatEvent
    object OnClearDocument : ChatEvent
} 