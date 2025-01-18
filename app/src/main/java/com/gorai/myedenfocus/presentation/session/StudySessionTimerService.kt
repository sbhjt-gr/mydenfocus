package com.gorai.myedenfocus.presentation.session

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import com.gorai.myedenfocus.util.Constants.ACTION_SERVICE_CANCEL
import com.gorai.myedenfocus.util.Constants.ACTION_SERVICE_START
import com.gorai.myedenfocus.util.Constants.ACTION_SERVICE_STOP
import com.gorai.myedenfocus.util.Constants.NOTIFICATION_CHANNEL_ID
import com.gorai.myedenfocus.util.Constants.NOTIFICATION_CHANNEL_NAME
import com.gorai.myedenfocus.util.Constants.NOTIFICATION_ID
import com.gorai.myedenfocus.util.pad
import dagger.hilt.android.AndroidEntryPoint
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class StudySessionTimerService : Service() {

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var notificationBuilder: NotificationCompat.Builder

    private val binder = StudySessionTimerBinder()

    private var timer: Timer? = null
    private var totalDurationMinutes: Int = 0
    private var elapsedSeconds: Int = 0

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

    override fun onBind(p0: Intent?) = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SERVICE_START -> {
                val minutes = intent.getIntExtra("DURATION", 0)
                if (minutes > 0) {
                    totalDurationMinutes = minutes
                    elapsedSeconds = 0  // Reset elapsed seconds
                    startTimer()
                }
            }
            ACTION_SERVICE_STOP -> pauseTimer()
            ACTION_SERVICE_CANCEL -> stopTimer()
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
        timer?.cancel()
        timer = null
        elapsedSeconds = 0
        totalDurationMinutes = 0
        
        hours.value = "00"
        minutes.value = "00"
        seconds.value = "00"
        
        currentTimerState.value = TimerState.IDLE
        stopForegroundService()
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