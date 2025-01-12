package com.gorai.myedenfocus.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gorai.myedenfocus.R
import kotlinx.coroutines.*
import android.media.RingtoneManager
import android.os.Build
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MeditationTimerService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var timeLeft = 0
    private var isTimerRunning = false
    
    companion object {
        const val CHANNEL_ID = "meditation_timer_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        const val EXTRA_TIME = "time_in_seconds"
        
        private val _timerState = MutableStateFlow<Int>(17 * 60)
        val timerState: StateFlow<Int> = _timerState
        
        private var isServiceRunning = false
        fun isRunning() = isServiceRunning
        
        private val _isAlarmPlaying = MutableStateFlow(false)
        val isAlarmPlaying: StateFlow<Boolean> = _isAlarmPlaying
        
        private var currentRingtone: Ringtone? = null
        
        fun stopAlarmStatic() {
            currentRingtone?.stop()
            currentRingtone = null
            _isAlarmPlaying.value = false
        }
        
        private val _isTimerCompleted = MutableStateFlow(false)
        val isTimerCompleted: StateFlow<Boolean> = _isTimerCompleted
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                timeLeft = intent.getIntExtra(EXTRA_TIME, 0)
                isServiceRunning = true
                startTimer()
            }
            ACTION_STOP -> {
                isServiceRunning = false
                stopTimer()
            }
            "STOP_ALARM" -> {
                stopAlarmStatic()
            }
        }
        return START_STICKY
    }

    private fun startTimer() {
        try {
            isTimerRunning = true
            _isTimerCompleted.value = false
            startForeground(NOTIFICATION_ID, createNotification(timeLeft))
            
            serviceScope.launch {
                while (isTimerRunning && timeLeft > 0) {
                    delay(1000)
                    timeLeft--
                    _timerState.value = timeLeft
                    updateNotification(timeLeft)
                    
                    if (timeLeft == 0) {
                        isTimerRunning = false
                        _isTimerCompleted.value = true
                        showCompletionNotification()
                        stopSelf()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun stopTimer() {
        isTimerRunning = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                CHANNEL_ID,
                "Meditation Timer",
                NotificationManager.IMPORTANCE_LOW
            )
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(timeInSeconds: Int): android.app.Notification {
        // Create intent to open meditation screen
        val openAppIntent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("openMeditation", true)
            }
        
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MeditationTimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Meditation in Progress")
            .setContentText(formatTime(timeInSeconds))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(timeInSeconds: Int) {
        val notification = createNotification(timeInSeconds)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification() {
        try {
            // Create intent to open app when timer completes
            val openAppIntent = packageManager
                .getLaunchIntentForPackage(packageName)
                ?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("openMeditation", true)
                }

            // Create stop alarm intent
            val stopAlarmIntent = Intent(this, MeditationTimerService::class.java).apply {
                action = "STOP_ALARM"
            }
            val stopAlarmPendingIntent = PendingIntent.getService(
                this,
                1,
                stopAlarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Open the app automatically
            startActivity(openAppIntent)

            // Create and show notification with stop alarm action
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Meditation Complete")
                .setContentText("Great job! You've completed your meditation session.")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(
                    R.drawable.ic_launcher_foreground,
                    "Stop Alarm",
                    stopAlarmPendingIntent
                )
                .setFullScreenIntent(
                    PendingIntent.getActivity(
                        this,
                        0,
                        openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    ),
                    true
                )
                .build()

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(2, notification)

            // Then handle the alarm sound
            serviceScope.launch(Dispatchers.Main) {
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                currentRingtone = RingtoneManager.getRingtone(applicationContext, alarmUri)?.apply {
                    isLooping = true
                }
                
                if (currentRingtone == null) {
                    val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    currentRingtone = RingtoneManager.getRingtone(applicationContext, fallbackUri)?.apply {
                        isLooping = true
                    }
                }
                
                currentRingtone?.play()
                _isAlarmPlaying.value = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopAlarm() {
        currentRingtone?.stop()
        currentRingtone = null
        _isAlarmPlaying.value = false
    }

    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!_isTimerCompleted.value) {
            // Only stop alarm if service is destroyed before timer completion
            stopAlarm()
        }
        serviceScope.cancel()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Don't stop service when app is removed from recent tasks
        // Let it continue running in background
    }
} 