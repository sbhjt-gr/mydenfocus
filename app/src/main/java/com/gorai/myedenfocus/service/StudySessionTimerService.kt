package com.gorai.myedenfocus.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import com.gorai.myedenfocus.domain.model.Session
import com.gorai.myedenfocus.domain.repository.SessionRepository
import com.gorai.myedenfocus.domain.repository.TaskRepository
import com.gorai.myedenfocus.util.Constants.ACTION_SERVICE_CANCEL
import com.gorai.myedenfocus.util.Constants.ACTION_SERVICE_START
import com.gorai.myedenfocus.util.Constants.ACTION_SERVICE_STOP
import com.gorai.myedenfocus.util.ServiceHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class StudySessionTimerService : Service() {
    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var taskRepository: TaskRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private var timerJob: Job? = null
    private var totalTimeSeconds = 0
    private var elapsedTimeSeconds = 0
    private var wasManuallyFinished = false
    private var selectedTopicId: Int? = null
    private var totalDurationMinutes = 0
    private var startTime: Long = 0
    
    var hours = mutableStateOf("00")
    var minutes = mutableStateOf("00")
    var seconds = mutableStateOf("00")
    var currentTimerState = mutableStateOf(TimerState.IDLE)
    var subjectId = mutableStateOf<Int?>(null)

    override fun onCreate() {
        super.onCreate()
        ServiceHelper.createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_SERVICE_START -> {
                    val durationMinutes = it.getIntExtra("DURATION", 0)
                    selectedTopicId = it.getIntExtra("TOPIC_ID", -1).let { id -> if (id == -1) null else id }
                    if (durationMinutes > 0) {
                        totalTimeSeconds = durationMinutes * 60
                        totalDurationMinutes = durationMinutes
                        elapsedTimeSeconds = 0
                        wasManuallyFinished = false
                        startTime = System.currentTimeMillis()
                        
                        // Save timer info to SharedPreferences
                        saveTimerState(selectedTopicId, startTime, totalTimeSeconds)
                        
                        startTimer()
                    }
                }
                ACTION_SERVICE_STOP -> pauseTimer()
                ACTION_SERVICE_CANCEL -> {
                    wasManuallyFinished = true
                    clearTimerState()
                    stopTimer()
                }
            }
        }
        return START_REDELIVER_INTENT
    }

    private fun saveTimerState(topicId: Int?, startTime: Long, duration: Int) {
        val completionTime = startTime + (duration * 1000L)
        println("StudySessionTimerService: Saving timer state - topicId: $topicId, completionTime: $completionTime")
        
        val prefs = getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            topicId?.let { putInt("topic_id", it) }
            putLong("completion_time", completionTime)
            putInt("subject_id", subjectId.value ?: -1)
            putInt("duration_minutes", totalDurationMinutes)
            apply()
        }
    }

    private fun clearTimerState() {
        println("StudySessionTimerService: Clearing timer state")
        val prefs = getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    private fun startTimer() {
        currentTimerState.value = TimerState.STARTED
        
        timerJob = serviceScope.launch {
            try {
                while (true) {
                    val currentTime = System.currentTimeMillis()
                    elapsedTimeSeconds = ((currentTime - startTime) / 1000).toInt()
                    
                    if (elapsedTimeSeconds >= totalTimeSeconds) {
                        completeTimer()
                        break
                    }
                    
                    updateTimeDisplay()
                    updateNotification()
                    delay(1000)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        startForeground(
            ServiceHelper.NOTIFICATION_ID,
            ServiceHelper.createNotification(
                context = this,
                hours = hours.value,
                minutes = minutes.value,
                seconds = seconds.value
            ).build()
        )
    }

    private suspend fun completeTimer() {
        // Only complete if we haven't manually finished
        if (!wasManuallyFinished) {
            playAlarm()
            selectedTopicId?.let { topicId ->
                try {
                    // Only mark task as complete, don't save session
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
        stopTimer()
    }

    private fun updateTimeDisplay() {
        val remainingSeconds = totalTimeSeconds - elapsedTimeSeconds
        hours.value = String.format("%02d", remainingSeconds / 3600)
        minutes.value = String.format("%02d", (remainingSeconds % 3600) / 60)
        seconds.value = String.format("%02d", remainingSeconds % 60)
    }

    private fun updateNotification() {
        val notification = ServiceHelper.createNotification(
            context = this,
            hours = hours.value,
            minutes = minutes.value,
            seconds = seconds.value
        ).build()
        
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ServiceHelper.NOTIFICATION_ID, notification)
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        currentTimerState.value = TimerState.STOPPED
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        elapsedTimeSeconds = 0
        
        hours.value = "00"
        minutes.value = "00"
        seconds.value = "00"
        
        currentTimerState.value = TimerState.IDLE
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        stopSelf()
        
        clearTimerState()
        
        // Reset duration and wasManuallyFinished flag
        totalDurationMinutes = 0
        wasManuallyFinished = false
        startTime = 0
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.launch {
            if (elapsedTimeSeconds >= totalTimeSeconds) {
                completeTimer()
            }
        }
        timerJob?.cancel()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun playAlarm() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.isLooping = true
            }
            ringtone.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

enum class TimerState {
    IDLE,
    STARTED,
    STOPPED
} 