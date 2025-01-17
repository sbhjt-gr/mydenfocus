package com.gorai.myedenfocus.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import com.gorai.myedenfocus.util.Constants.ACTION_SERVICE_CANCEL
import com.gorai.myedenfocus.util.Constants.ACTION_SERVICE_START
import com.gorai.myedenfocus.util.Constants.ACTION_SERVICE_STOP
import com.gorai.myedenfocus.util.ServiceHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StudySessionTimerService : Service() {
    private var timerJob: Job? = null
    private var totalTimeSeconds = 0
    private var elapsedTimeSeconds = 0

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
                    if (durationMinutes > 0) {
                        totalTimeSeconds = durationMinutes * 60
                        elapsedTimeSeconds = 0
                        startTimer()
                    }
                }
                ACTION_SERVICE_STOP -> pauseTimer()
                ACTION_SERVICE_CANCEL -> stopTimer()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startTimer() {
        currentTimerState.value = TimerState.STARTED
        
        GlobalScope.launch {
            while (elapsedTimeSeconds < totalTimeSeconds) {
                val remainingSeconds = totalTimeSeconds - elapsedTimeSeconds
                updateTimeDisplay(remainingSeconds)
                updateNotification()
                delay(1000)
                elapsedTimeSeconds++
            }
            stopTimer()
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

    private fun updateTimeDisplay(remainingSeconds: Int) {
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
        currentTimerState.value = TimerState.IDLE
        hours.value = "00"
        minutes.value = "00"
        seconds.value = "00"
        totalTimeSeconds = 0
        elapsedTimeSeconds = 0
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}

enum class TimerState {
    IDLE,
    STARTED,
    STOPPED
} 