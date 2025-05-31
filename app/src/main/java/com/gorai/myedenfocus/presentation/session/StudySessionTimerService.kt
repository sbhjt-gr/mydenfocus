package com.gorai.myedenfocus.presentation.session

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Binder
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import com.gorai.myedenfocus.R
import com.gorai.myedenfocus.domain.repository.SessionRepository
import com.gorai.myedenfocus.domain.repository.TaskRepository
import com.gorai.myedenfocus.util.Constants.ACTION_SERVICE_CANCEL
import com.gorai.myedenfocus.util.Constants.ACTION_SERVICE_START
import com.gorai.myedenfocus.util.Constants.ACTION_SERVICE_STOP
import com.gorai.myedenfocus.util.Constants.NOTIFICATION_CHANNEL_ID
import com.gorai.myedenfocus.util.Constants.NOTIFICATION_CHANNEL_NAME
import com.gorai.myedenfocus.util.Constants.NOTIFICATION_ID
import com.gorai.myedenfocus.util.pad
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject
import kotlinx.coroutines.launch
import android.util.Log
import com.gorai.myedenfocus.domain.model.Session
import com.gorai.myedenfocus.domain.model.Task
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@AndroidEntryPoint
class StudySessionTimerService : Service() {

    var onSessionComplete: (() -> Unit)? = null

    companion object {
        const val ACTION_STOP_ALARM = "STOP_ALARM"
        const val ACTION_SHOW_COMPLETION_DIALOG = "SHOW_COMPLETION_DIALOG"
        
        private val _isAlarmPlaying = MutableStateFlow(false)
        val isAlarmPlaying: StateFlow<Boolean> = _isAlarmPlaying.asStateFlow()
        
        private var currentRingtone: Ringtone? = null
        
        fun stopAlarmStatic() {
            try {
                currentRingtone?.stop()
                currentRingtone = null
                _isAlarmPlaying.value = false
            } catch (e: Exception) {
                Log.e("StudyTimer", "Error stopping alarm sound", e)
            }
        }
    }

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var notificationBuilder: NotificationCompat.Builder

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var taskRepository: TaskRepository

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val binder = StudySessionTimerBinder()

    private var timer: Timer? = null
    private var totalDurationMinutes: Int = 0
    private var elapsedSeconds: Int = 0
    private var remainingSeconds: Int = 0
    private var wasManuallyFinished = false
    private var selectedTopicId: Int? = null
    private var lastSavedMinute: Int = -1
    private var startTime: Long = 0
    private var pausedTime: Long = 0

    var duration: Int = 0
        private set
    var seconds = mutableStateOf("00")
        private set
    var minutes = mutableStateOf("00")
        private set
    var hours = mutableStateOf("00")
        private set
    var currentTimerState = mutableStateOf(TimerState.IDLE)
        private set
    var subjectId = mutableStateOf<Int?>(null)
    var topicId = mutableStateOf<Int?>(null)
        private set
    var selectedTopic = mutableStateOf<Task?>(null)
        private set

    private val _elapsedTimeFlow = MutableStateFlow(0)
    val elapsedTimeFlow: StateFlow<Int> = _elapsedTimeFlow.asStateFlow()

    private val _sessionCompleted = MutableStateFlow(false)
    val sessionCompleted = _sessionCompleted.asStateFlow()

    private var wakeLock: PowerManager.WakeLock? = null

    private val _minuteUpdateFlow = MutableSharedFlow<Int>()
    val minuteUpdateFlow = _minuteUpdateFlow.asSharedFlow()

    private var currentSessionId: Long? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onBind(p0: Intent?) = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SERVICE_START -> {
                val minutes = intent.getIntExtra("DURATION", 0)
                if (minutes > 0) {
                    totalDurationMinutes = minutes
                    selectedTopicId = intent.getIntExtra("TOPIC_ID", -1).let { if (it == -1) null else it }
                    topicId.value = selectedTopicId
                    subjectId.value = intent.getIntExtra("SUBJECT_ID", -1).let { if (it == -1) null else it }
                    
                    // Load topic details
                    serviceScope.launch(Dispatchers.IO) {
                        selectedTopicId?.let { id ->
                            taskRepository.getTaskById(id)?.let { task ->
                                selectedTopic.value = task
                            }
                        }
                    }
                    
                    enableDnd()
                    startTimer()
                }
            }
            ACTION_SERVICE_STOP -> {
                pauseTimer()
                disableDnd()
            }
            ACTION_SERVICE_CANCEL -> {
                stopTimer(completed = false)
                disableDnd()
            }
            ACTION_SHOW_COMPLETION_DIALOG -> {
                disableDnd()  // Disable DND before showing completion dialog
                showCompletionDialog()
            }
            ACTION_STOP_ALARM -> {
                stopAlarm()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundService() {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun stopForegroundService() {
        notificationManager.cancel(NOTIFICATION_ID)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }

        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification(hours: String, minutes: String, seconds: String) {
        notificationManager.notify(
            NOTIFICATION_ID,
            notificationBuilder
                .setContentText("$hours:$minutes:$seconds")
                .build()
        )
    }

    private fun startTimer() {
        if (currentTimerState.value == TimerState.STOPPED) {
            // Resume from paused state
            startTime = System.currentTimeMillis() - (elapsedSeconds * 1000L)
        } else {
            // Fresh start
            startTime = System.currentTimeMillis()
            elapsedSeconds = 0
            remainingSeconds = totalDurationMinutes * 60
            _elapsedTimeFlow.value = 0
            _sessionCompleted.value = false
            wasManuallyFinished = false
        }
        
        currentTimerState.value = TimerState.STARTED
        startForegroundService()

        // Save topic and subject IDs to preferences
        val prefs = applicationContext.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("topic_id", selectedTopicId ?: -1)
            putInt("subject_id", subjectId.value ?: -1)
            putInt("duration_minutes", totalDurationMinutes)
            putLong("start_time", startTime)
            putInt("elapsed_seconds", elapsedSeconds)
            putBoolean("session_saved", false)
            apply()
        }

        timer?.cancel()
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (currentTimerState.value == TimerState.STARTED) {
                    val currentTime = System.currentTimeMillis()
                    elapsedSeconds = ((currentTime - startTime) / 1000).toInt()
                    remainingSeconds = (totalDurationMinutes * 60) - elapsedSeconds
                    _elapsedTimeFlow.value = elapsedSeconds
                    
                    // Calculate remaining time
                    hours.value = String.format("%02d", remainingSeconds / 3600)
                    minutes.value = String.format("%02d", (remainingSeconds % 3600) / 60)
                    seconds.value = String.format("%02d", remainingSeconds % 60)
                    
                    // Update notification
                    updateNotification(hours.value, minutes.value, seconds.value)
                    
                    // Check if timer should complete
                    if (remainingSeconds <= 0) {
                        stopTimer(completed = true)
                    }

                    val currentMinute = elapsedSeconds / 60
                    if (currentMinute > lastSavedMinute) {
                        lastSavedMinute = currentMinute
                        serviceScope.launch {
                            updateSessionProgress()
                            _minuteUpdateFlow.emit(currentMinute)
                        }
                    }
                }
            }
        }, 0, 1000)
    }

    private fun pauseTimer() {
        timer?.cancel()
        timer = null
        pausedTime = System.currentTimeMillis()
        remainingSeconds = (totalDurationMinutes * 60) - elapsedSeconds
        
        // Save paused state
        val prefs = applicationContext.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putLong("paused_time", pausedTime)
            putInt("remaining_seconds", remainingSeconds)
            putInt("elapsed_seconds", elapsedSeconds)
            apply()
        }
        
        currentTimerState.value = TimerState.STOPPED
    }

    private fun stopTimer(completed: Boolean = false) {
        timer?.cancel()
        timer = null
        
        val finalElapsedTime = elapsedSeconds
        wasManuallyFinished = !completed

        serviceScope.launch(Dispatchers.IO) {
            try {
                // Update the final session
                if (finalElapsedTime > 0 && subjectId.value != null) {
                    val sessionDuration = (finalElapsedTime / 60).toLong() // Convert to minutes
                    if (currentSessionId != null) {
                        sessionRepository.getSessionById(currentSessionId!!)?.let { existingSession ->
                            val updatedSession = existingSession.copy(
                                endTime = System.currentTimeMillis(),
                                duration = sessionDuration,
                                wasCompleted = completed
                            )
                            sessionRepository.updateSession(updatedSession)
                        }
                    } else {
                        val session = Session(
                            sessionSubjectId = subjectId.value!!,
                            relatedToSubject = selectedTopic.value?.title ?: "",
                            topicName = selectedTopic.value?.title ?: "",
                            startTime = startTime,
                            endTime = System.currentTimeMillis(),
                            duration = sessionDuration,
                            plannedDuration = totalDurationMinutes.toLong(),
                            wasCompleted = completed
                        )
                        currentSessionId = sessionRepository.insertSession(session)
                    }
                }

                if (completed && selectedTopicId != null) {
                    // Mark task as complete if timer finished naturally
                    selectedTopicId?.let { topicId ->
                        taskRepository.getTaskById(topicId)?.let { task ->
                            taskRepository.upsertTask(task.copy(isComplete = true))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("StudyTimer", "Error in stopTimer", e)
            } finally {
                // Reset states
                currentSessionId = null
                currentTimerState.value = TimerState.IDLE
                hours.value = "00"
                minutes.value = "00"
                seconds.value = "00"
                _elapsedTimeFlow.value = 0
                elapsedSeconds = 0
                remainingSeconds = 0
                startTime = 0
                pausedTime = 0
                
                // Clear preferences
                val prefs = applicationContext.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
                prefs.edit().clear().apply()
                
                // Set completion state and show dialog
                if (completed) {
                    _sessionCompleted.value = true
                    showCompletionDialog()
                } else {
                    stopForegroundService()
                }
            }
        }
    }

    private suspend fun saveSession(duration: Int) {
        try {
            Log.d("StudyTimer", "Saving session with duration: $duration seconds")
            val currentTime = System.currentTimeMillis()
            // Calculate exact minutes, rounding up only if more than 30 seconds
            val sessionDuration = if (duration % 60 >= 30) {
                (duration / 60) + 1L
            } else {
                (duration / 60).toLong()
            }
            
            if (subjectId.value == null) {
                Log.e("StudyTimer", "Cannot save session: No subject ID")
                return
            }

            selectedTopicId?.let { topicId ->
                taskRepository.getTaskById(topicId)?.let { task ->
                    Log.d("StudyTimer", "Saving session with duration: $sessionDuration minutes")
                    sessionRepository.insertSession(
                        Session(
                            sessionSubjectId = subjectId.value!!,
                            relatedToSubject = task.title,
                            topicName = task.title,
                            startTime = currentTime - (duration * 1000),
                            endTime = currentTime,
                            duration = sessionDuration,
                            plannedDuration = totalDurationMinutes.toLong(),
                            wasCompleted = true
                        )
                    )
                    Log.d("StudyTimer", "Session saved successfully with topic")
                }
            } ?: run {
                sessionRepository.insertSession(
                    Session(
                        sessionSubjectId = subjectId.value!!,
                        relatedToSubject = "",
                        topicName = "",
                        startTime = currentTime - (duration * 1000),
                        endTime = currentTime,
                        duration = sessionDuration,
                        plannedDuration = totalDurationMinutes.toLong(),
                        wasCompleted = true
                    )
                )
                Log.d("StudyTimer", "Session saved successfully without topic")
            }
        } catch (e: Exception) {
            Log.e("StudyTimer", "Error in saveSession", e)
            throw e
        }
    }

    private fun showCompletionDialog() {
        try {
            // Ensure DND is disabled first
            disableDnd()

            // Acquire wake lock to keep CPU running
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                    "MydenFocus::AlarmWakeLock"
                ).apply {
                    acquire(10*60*1000L /*10 minutes*/)
                }
            }

            // Play alarm sound
            try {
                currentRingtone?.stop()
                currentRingtone = null
                
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                currentRingtone = RingtoneManager.getRingtone(applicationContext, notification)
                currentRingtone?.play()
                _isAlarmPlaying.value = true
            } catch (e: Exception) {
                Log.e("StudyTimer", "Error playing alarm sound", e)
            }

            // Launch the alarm activity
            val intent = Intent(this, StudySessionCompleteActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)

            // Create a high-priority notification
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val alarmChannel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel(
                    "study_alarm_channel",
                    "Study Session Alarm",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    enableLights(true)
                    enableVibration(true)
                }
            } else null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && alarmChannel != null) {
                notificationManager.createNotificationChannel(alarmChannel)
            }

            val alarmIntent = Intent(this, StudySessionCompleteActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, "study_alarm_channel")
                .setContentTitle("Study Session Complete!")
                .setContentText("Tap to stop the alarm")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setOngoing(true)
                .build()

            notificationManager.notify(2, notification)

        } catch (e: Exception) {
            Log.e("StudyTimer", "Error showing completion dialog", e)
            e.printStackTrace()
            // Make sure DND is disabled even if there's an error
            disableDnd()
        }
    }

    fun stopAlarm() {
        try {
            currentRingtone?.stop()
            currentRingtone = null
            _isAlarmPlaying.value = false
            
            // Release wake lock
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            wakeLock = null
            
            // Stop foreground service and remove notification
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                stopForeground(true)
            }
            
            // Clear any remaining notifications
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(2)
            notificationManager.cancel(NOTIFICATION_ID)
            
            stopSelf()
        } catch (e: Exception) {
            Log.e("StudyTimer", "Error stopping alarm", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopAlarm()
    }

    fun setDuration(durationMinutes: Int) {
        totalDurationMinutes = durationMinutes
        // Reset timer values and convert minutes to hours and minutes
        val h = durationMinutes / 60  // Get hours
        val m = durationMinutes % 60  // Get remaining minutes
        
        hours.value = h.pad()
        minutes.value = m.pad()
        seconds.value = "00"
    }

    inner class StudySessionTimerBinder : Binder() {
        fun getService(): StudySessionTimerService = this@StudySessionTimerService
    }

    fun resetSessionCompleted() {
        _sessionCompleted.value = false
    }

    private fun enableDnd() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
            // Block all interruptions for maximum focus
            notificationManager.notificationPolicy = NotificationManager.Policy(
                0,  // No priority categories - blocks all sounds
                0,  // No priority senders
                0,  // No repeat callers
                NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_ON or
                NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_OFF or
                NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK or
                NotificationManager.Policy.SUPPRESSED_EFFECT_STATUS_BAR or
                NotificationManager.Policy.SUPPRESSED_EFFECT_BADGE or
                NotificationManager.Policy.SUPPRESSED_EFFECT_AMBIENT or
                NotificationManager.Policy.SUPPRESSED_EFFECT_NOTIFICATION_LIST  // Suppress all visual effects
            )
            // Set to total silence mode
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        }
    }

    private fun disableDnd() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
                // First reset notification policy to default
                notificationManager.notificationPolicy = NotificationManager.Policy(
                    NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS or
                    NotificationManager.Policy.PRIORITY_CATEGORY_CALLS or
                    NotificationManager.Policy.PRIORITY_CATEGORY_EVENTS or
                    NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES or
                    NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA or
                    NotificationManager.Policy.PRIORITY_CATEGORY_REMINDERS or
                    NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS or
                    NotificationManager.Policy.PRIORITY_CATEGORY_SYSTEM,
                    NotificationManager.Policy.PRIORITY_SENDERS_ANY,
                    NotificationManager.Policy.PRIORITY_SENDERS_ANY
                )
                // Then disable DND
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        } catch (e: Exception) {
            Log.e("StudyTimer", "Error disabling DND", e)
        }
    }

    private fun handleSessionCompletion() {
        _sessionCompleted.value = true
        // ... existing code ...
    }

    private suspend fun updateSessionProgress() {
        try {
            if (subjectId.value != null) {
                val currentMinutes = (elapsedSeconds / 60).toLong()
                if (currentMinutes > 0) {
                    if (currentSessionId == null) {
                        // Create new session only if one doesn't exist
                        val session = Session(
                            sessionSubjectId = subjectId.value!!,
                            relatedToSubject = selectedTopic.value?.title ?: "",
                            topicName = selectedTopic.value?.title ?: "",
                            startTime = startTime,
                            endTime = System.currentTimeMillis(),
                            duration = currentMinutes,
                            plannedDuration = totalDurationMinutes.toLong(),
                            wasCompleted = false
                        )
                        currentSessionId = sessionRepository.insertSession(session)
                    } else {
                        // Update existing session
                        sessionRepository.getSessionById(currentSessionId!!)?.let { existingSession ->
                            val updatedSession = existingSession.copy(
                                endTime = System.currentTimeMillis(),
                                duration = currentMinutes
                            )
                            sessionRepository.updateSession(updatedSession)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("StudyTimer", "Error updating session progress", e)
        }
    }
}
enum class TimerState {
    IDLE,
    STARTED,
    STOPPED
}