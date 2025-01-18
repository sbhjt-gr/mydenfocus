package com.gorai.myedenfocus.presentation.session

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import com.gorai.myedenfocus.R
import com.gorai.myedenfocus.domain.model.Session
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
import java.time.Instant
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StudySessionTimerService : Service() {

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var notificationBuilder: NotificationCompat.Builder

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var taskRepository: TaskRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val binder = StudySessionTimerBinder()

    private var timer: Timer? = null
    private var totalDurationMinutes: Int = 0
    private var elapsedSeconds: Int = 0
    private var wasManuallyFinished = false
    private var selectedTopicId: Int? = null

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

    companion object {
        private val _isAlarmPlaying = MutableStateFlow(false)
        val isAlarmPlaying: StateFlow<Boolean> = _isAlarmPlaying.asStateFlow()
        
        private var currentRingtone: Ringtone? = null
        
        fun stopAlarmStatic() {
            currentRingtone?.stop()
            currentRingtone = null
            _isAlarmPlaying.value = false
        }
    }

    override fun onBind(p0: Intent?) = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SERVICE_START -> {
                val minutes = intent.getIntExtra("DURATION", 0)
                if (minutes > 0) {
                    totalDurationMinutes = minutes
                    elapsedSeconds = 0  // Reset elapsed seconds
                    wasManuallyFinished = false
                    selectedTopicId = intent.getIntExtra("TOPIC_ID", -1).let { if (it == -1) null else it }
                    startTimer()
                }
            }
            ACTION_SERVICE_STOP -> pauseTimer()
            ACTION_SERVICE_CANCEL -> {
                wasManuallyFinished = true
                stopTimer()
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

        timer?.cancel()
        timer = fixedRateTimer(initialDelay = 0L, period = 1000L) {
            if (elapsedSeconds >= totalDurationMinutes * 60) {
                stopTimer()
                return@fixedRateTimer
            }

            elapsedSeconds++
            duration = elapsedSeconds

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
        
        timer?.cancel()
        timer = null
        elapsedSeconds = 0
        totalDurationMinutes = 0
        
        hours.value = "00"
        minutes.value = "00"
        seconds.value = "00"
        
        currentTimerState.value = TimerState.IDLE
        stopForegroundService()
        
        // Only play alarm and complete task if timer completed naturally and wasn't manually finished
        if (wasTimerCompleted && !wasManuallyFinished) {
            playAlarm()
            selectedTopicId?.let { topicId ->
                // Save session and complete task
                serviceScope.launch {
                    try {
                        sessionRepository.insertSession(
                            Session(
                                sessionSubjectId = subjectId.value ?: -1,
                                relatedToSubject = "",  // This will be updated by the UI if needed
                                date = Instant.now().toEpochMilli(),
                                duration = totalDurationMinutes.toLong()
                            )
                        )

                        taskRepository.getTaskById(topicId)?.let { task ->
                            taskRepository.upsertTask(
                                task.copy(isComplete = true)
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun playAlarm() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            currentRingtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                currentRingtone?.isLooping = true
            }
            currentRingtone?.play()
            _isAlarmPlaying.value = true
            
            // Show completion notification
            showCompletionNotification()
        } catch (e: Exception) {
            try {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                currentRingtone = RingtoneManager.getRingtone(applicationContext, fallbackUri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    currentRingtone?.isLooping = true
                }
                currentRingtone?.play()
                _isAlarmPlaying.value = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showCompletionNotification() {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Study Session Complete")
            .setContentText("Great job! You've completed your study session.")
            .setSmallIcon(com.gorai.myedenfocus.R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()

        notificationManager.notify(2, notification)
    }

    fun stopAlarm() {
        currentRingtone?.stop()
        currentRingtone = null
        _isAlarmPlaying.value = false
        notificationManager.cancel(2)
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
}
enum class TimerState {
    IDLE,
    STARTED,
    STOPPED
}