package com.gorai.myedenfocus.presentation.meditation

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorai.myedenfocus.domain.model.MeditationSession
import com.gorai.myedenfocus.domain.repository.MeditationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class MeditationViewModel @Inject constructor(
    private val repository: MeditationRepository
) : ViewModel() {
    private val _sessions = MutableStateFlow<List<MeditationSession>>(emptyList())
    val sessions = _sessions.asStateFlow()

    private val _isDndEnabled = MutableStateFlow(false)
    val isDndEnabled = _isDndEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getRecentSessions(7).collect {
                _sessions.value = it
            }
        }
    }

    fun addSession(duration: Int) {
        viewModelScope.launch {
            val session = MeditationSession(
                duration = duration,
                timestamp = LocalDateTime.now()
            )
            repository.insertSession(session)
        }
    }

    fun deleteSession(session: MeditationSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }

    fun checkDndPermission(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }

    fun openDndSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun enableDnd(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.isNotificationPolicyAccessGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Allow all sounds except notifications
                notificationManager.notificationPolicy = NotificationManager.Policy(
                    NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS or    // Allow alarms
                    NotificationManager.Policy.PRIORITY_CATEGORY_SYSTEM or    // Allow system sounds
                    NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA or     // Allow media
                    NotificationManager.Policy.PRIORITY_CATEGORY_CALLS or     // Allow calls
                    NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES,    // Allow messages
                    NotificationManager.Policy.PRIORITY_SENDERS_ANY,          // Allow from anyone
                    NotificationManager.Policy.PRIORITY_SENDERS_ANY,          // Allow repeat callers
                    NotificationManager.Policy.SUPPRESSED_EFFECT_NOTIFICATION_LIST // Only suppress notification list
                )
            }
            
            // Set to priority mode
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            _isDndEnabled.value = true
        }
    }

    fun disableDnd(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            _isDndEnabled.value = false
        }
    }
} 