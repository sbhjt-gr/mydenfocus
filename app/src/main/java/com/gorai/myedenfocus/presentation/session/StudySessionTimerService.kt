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
import javax.inject.Inject
import kotlin.concurrent.fixedRateTimer
import kotlinx.coroutines.launch
import android.util.Log
import com.gorai.myedenfocus.domain.model.Session

@AndroidEntryPoint
class StudySessionTimerService : Service() {

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
    private var wasManuallyFinished = false
    private var selectedTopicId: Int? = null
    private var lastSavedMinute: Int = -1

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

    private val _elapsedTimeFlow = MutableStateFlow(0)
    val elapsedTimeFlow: StateFlow<Int> = _elapsedTimeFlow.asStateFlow()

    private val _sessionCompleted = MutableStateFlow(false)
    val sessionCompleted: StateFlow<Boolean> = _sessionCompleted.asStateFlow()

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(p0: Intent?) = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SERVICE_START -> {
                val minutes = intent.getIntExtra("DURATION", 0)
                if (minutes > 0) {
                    totalDurationMinutes = minutes
                    elapsedSeconds = 0  // Reset elapsed seconds
                    wasManuallyFinished = false
                    _sessionCompleted.value = false
                    selectedTopicId = intent.getIntExtra("TOPIC_ID", -1).let { if (it == -1) null else it }
                    // Get subject ID from intent
                    subjectId.value = intent.getIntExtra("SUBJECT_ID", -1).let { if (it == -1) null else it }
                    Log.d("StudyTimer", "Starting timer with subjectId: ${subjectId.value}")
                    startTimer()
                }
            }
            ACTION_SERVICE_STOP -> pauseTimer()
            ACTION_SERVICE_CANCEL -> {
                wasManuallyFinished = true
                stopTimer()
            }
            ACTION_SHOW_COMPLETION_DIALOG -> showCompletionDialog()
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
        currentTimerState.value = TimerState.STARTED
        startForegroundService()
        lastSavedMinute = -1

        timer?.cancel()
        timer = fixedRateTimer(initialDelay = 0L, period = 1000L) {
            elapsedSeconds++
            duration = elapsedSeconds
            _elapsedTimeFlow.value = elapsedSeconds

            // Calculate current minute
            val currentMinute = elapsedSeconds / 60

            // Save session when we enter a new minute
            if (currentMinute > lastSavedMinute) {
                lastSavedMinute = currentMinute
                serviceScope.launch(Dispatchers.IO) {
                    saveSession(elapsedSeconds)
                }
            }

            // Check if timer should stop
            if (elapsedSeconds >= totalDurationMinutes * 60) {
                stopTimer()
                return@fixedRateTimer
            }

            val remainingSeconds = (totalDurationMinutes * 60) - elapsedSeconds
            val h = remainingSeconds / 3600
            val m = (remainingSeconds % 3600) / 60
            val s = remainingSeconds % 60

            hours.value = h.pad()
            minutes.value = m.pad()
            seconds.value = s.pad()

            updateNotification(hours.value, minutes.value, seconds.value)
        }
    }

    private fun pauseTimer() {
        timer?.cancel()
        timer = null
        currentTimerState.value = TimerState.STOPPED
    }

    private fun stopTimer() {
        val wasTimerCompleted = elapsedSeconds >= totalDurationMinutes * 60
        val finalElapsedTime = elapsedSeconds
        
        timer?.cancel()
        timer = null
        
        if (finalElapsedTime > 0) {
            serviceScope.launch(Dispatchers.IO) {
                saveSession(finalElapsedTime)
                if (wasTimerCompleted && !wasManuallyFinished) {
                    _sessionCompleted.value = true
                    showCompletionDialog()
                    // Don't stop the service here, let it run until alarm is dismissed
                } else if (wasManuallyFinished) {
                    _sessionCompleted.value = true
                    stopForegroundService() // Only stop service if manually finished
                }
            }
        } else {
            stopForegroundService()
        }

        lastSavedMinute = -1
        elapsedSeconds = 0
        totalDurationMinutes = 0
        hours.value = "00"
        minutes.value = "00"
        seconds.value = "00"
        currentTimerState.value = TimerState.IDLE
        _elapsedTimeFlow.value = 0
    }

    private fun saveSession(duration: Int) {
        try {
            Log.d("StudyTimer", "Saving session with duration: $duration, subjectId: ${subjectId.value}")
            val currentTime = System.currentTimeMillis()
            val sessionDuration = (duration / 60).toLong() // Convert seconds to minutes
            
            if (subjectId.value == null) {
                Log.e("StudyTimer", "Cannot save session: No subject ID")
                return
            }

            serviceScope.launch(Dispatchers.IO) {
                try {
                    selectedTopicId?.let { topicId ->
                        taskRepository.getTaskById(topicId)?.let { task ->
                            sessionRepository.insertSession(
                                Session(
                                    sessionSubjectId = subjectId.value!!,
                                    relatedToSubject = task.title,
                                    topicName = task.title,
                                    date = currentTime,
                                    duration = sessionDuration
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
                                date = currentTime,
                                duration = sessionDuration
                            )
                        )
                        Log.d("StudyTimer", "Session saved successfully without topic")
                    }
                } catch (e: Exception) {
                    Log.e("StudyTimer", "Error saving session", e)
                }
            }
        } catch (e: Exception) {
            Log.e("StudyTimer", "Error in saveSession", e)
        }
    }

    private fun showCompletionDialog() {
        try {
            // Acquire wake lock to keep CPU running
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                    "MyedenFocus::AlarmWakeLock"
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
}
enum class TimerState {
    IDLE,
    STARTED,
    STOPPED
}