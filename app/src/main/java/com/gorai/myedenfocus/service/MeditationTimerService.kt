package com.gorai.myedenfocus.service

import android.app.Notification
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
import kotlinx.coroutines.flow.asStateFlow
import android.app.TaskStackBuilder
import com.gorai.myedenfocus.MainActivity
import android.net.Uri
import androidx.core.net.toUri
import android.media.MediaPlayer

class MeditationTimerService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var timeLeft = 0
    private var isTimerRunning = false
    private var mediaPlayer: MediaPlayer? = null
    
    companion object {
        const val CHANNEL_ID = "meditation_timer_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        const val EXTRA_TIME = "time_in_seconds"
        const val ACTION_STOP_TIMER = "STOP_TIMER"
        const val ACTION_PAUSE = "PAUSE"
        const val ACTION_RESUME = "RESUME"
        const val ACTION_RESET = "RESET"
        
        private val _timerState = MutableStateFlow<Int>(17 * 60)
        val timerState: StateFlow<Int> = _timerState
        
        private var isServiceRunning = false
        fun isRunning() = isServiceRunning
        
        private val _isAlarmPlaying = MutableStateFlow(false)
        val isAlarmPlaying: StateFlow<Boolean> = _isAlarmPlaying
        
        private var currentRingtone: Ringtone? = null
        
        private val _isPaused = MutableStateFlow(false)
        val isPaused: StateFlow<Boolean> = _isPaused
        
        private val _isTimerCompleted = MutableStateFlow(false)
        val isTimerCompleted = _isTimerCompleted.asStateFlow()
        
        private var isBackgroundMusicPlaying = false
        
        fun stopBackgroundMusic() {
            isBackgroundMusicPlaying = false
        }
        
        fun resetTimerCompleted() {
            _isTimerCompleted.value = false
        }
        
        fun stopAlarmStatic() {
            currentRingtone?.stop()
            currentRingtone = null
            _isAlarmPlaying.value = false
        }
        
        private fun createPendingIntent(context: Context): PendingIntent {
            val deepLinkIntent = Intent(
                Intent.ACTION_VIEW,
                "myedenfocus://meditation".toUri(),
                context,
                MainActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            
            return TaskStackBuilder.create(context).run {
                addNextIntentWithParentStack(deepLinkIntent)
                getPendingIntent(
                    0,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
        }
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
                val selectedMusic = intent.getStringExtra("selected_music") ?: "no_music"
                isServiceRunning = true
                _isPaused.value = false
                startTimer(timeLeft, selectedMusic)
            }
            ACTION_PAUSE -> {
                isTimerRunning = false
                _isPaused.value = true
                mediaPlayer?.pause()
                updateNotification(timeLeft)
            }
            ACTION_RESUME -> {
                isTimerRunning = true
                _isPaused.value = false
                mediaPlayer?.start()
                startTimer(timeLeft, intent.getStringExtra("selected_music") ?: "no_music")
            }
            ACTION_RESET -> {
                isTimerRunning = false
                _isPaused.value = false
                timeLeft = 0
                _timerState.value = 0
                _isTimerCompleted.value = false
                
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                isBackgroundMusicPlaying = false
                
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_STOP, ACTION_STOP_TIMER -> {
                isServiceRunning = false
                stopTimer()
                stopSelf()
            }
            "STOP_ALARM" -> {
                stopAlarmStatic()
            }
        }
        return START_STICKY
    }

    private fun startTimer(initialTime: Int, selectedMusic: String) {
        isTimerRunning = true
        timeLeft = initialTime
        _timerState.value = initialTime
        _isTimerCompleted.value = false
        startForeground(NOTIFICATION_ID, createNotification(timeLeft))
        
        if (mediaPlayer == null && selectedMusic != "no_music") {
            val musicResId = when (selectedMusic) {
                "wind_chimes_nature_symphony.mp3" -> R.raw.wind_chimes_nature_symphony
                "soothing_chime.mp3" -> R.raw.soothing_chime
                "full_brain_drop_down.mp3" -> R.raw.full_brain_drop_down
                "focus_on_yourself.mp3" -> R.raw.focus_on_yourself
                else -> 0
            }
            if (musicResId != 0) {
                mediaPlayer = MediaPlayer.create(this, musicResId)
                mediaPlayer?.isLooping = true
                mediaPlayer?.start()
                isBackgroundMusicPlaying = true
            }
        }
        
        serviceScope.launch {
            while (isTimerRunning && timeLeft > 0) {
                delay(1000)
                timeLeft--
                _timerState.value = timeLeft
                updateNotification(timeLeft)
                
                if (timeLeft == 0) {
                    isTimerRunning = false
                    _isTimerCompleted.value = true
                    isServiceRunning = false
                    
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                    isBackgroundMusicPlaying = false
                    
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    showCompletionNotification()
                }
            }
        }
    }

    private fun stopTimer() {
        isTimerRunning = false
        timeLeft = 0
        _timerState.value = 0
        
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        isBackgroundMusicPlaying = false
        
        stopForeground(STOP_FOREGROUND_REMOVE)
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

    private fun createNotification(remainingSeconds: Int): Notification {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        val title = "Meditation in Progress"
        val text = String.format("%02d:%02d remaining", minutes, seconds)

        // Create pending intents for actions
        val pauseIntent = Intent(this, MeditationTimerService::class.java).apply {
            action = ACTION_PAUSE
        }
        val resumeIntent = Intent(this, MeditationTimerService::class.java).apply {
            action = ACTION_RESUME
        }
        val resetIntent = Intent(this, MeditationTimerService::class.java).apply {
            action = ACTION_RESET
        }

        val pausePendingIntent = PendingIntent.getService(
            this, 4, pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val resumePendingIntent = PendingIntent.getService(
            this, 5, resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val resetPendingIntent = PendingIntent.getService(
            this, 6, resetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(createPendingIntent(this))

        // Add action buttons based on timer state
        if (_isPaused.value) {
            builder.addAction(R.drawable.ic_launcher_foreground, "Resume", resumePendingIntent)
            builder.addAction(R.drawable.ic_launcher_foreground, "Reset", resetPendingIntent)
        } else {
            builder.addAction(R.drawable.ic_launcher_foreground, "Pause", pausePendingIntent)
        }

        return builder.build()
    }

    private fun updateNotification(remainingSeconds: Int) {
        val notification = createNotification(remainingSeconds)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification() {
        try {
            // No need to manually cancel NOTIFICATION_ID since stopForeground(STOP_FOREGROUND_REMOVE) handles it
            
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).apply {
                // Create and show completion notification with ID 2
                val deepLinkIntent = Intent(
                    Intent.ACTION_VIEW,
                    "myedenfocus://meditation".toUri(),
                    this@MeditationTimerService,
                    MainActivity::class.java
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                val openMeditationPendingIntent = TaskStackBuilder.create(this@MeditationTimerService).run {
                    addNextIntentWithParentStack(deepLinkIntent)
                    getPendingIntent(
                        3,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }

                val stopAlarmIntent = Intent(this@MeditationTimerService, MeditationTimerService::class.java).apply {
                    action = "STOP_ALARM"
                }
                val stopAlarmPendingIntent = PendingIntent.getService(
                    this@MeditationTimerService,
                    1,
                    stopAlarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(this@MeditationTimerService, CHANNEL_ID)
                    .setContentTitle("Meditation Complete")
                    .setContentText("Great job! You've completed your meditation session.")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setOngoing(true)
                    .setContentIntent(openMeditationPendingIntent)
                    .addAction(
                        R.drawable.ic_launcher_foreground,
                        "Stop Alarm",
                        stopAlarmPendingIntent
                    )
                    .build()

                notify(2, notification)
            }

            // Play alarm sound
            serviceScope.launch(Dispatchers.Main) {
                try {
                    val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    currentRingtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        currentRingtone?.isLooping = true
                    }
                    currentRingtone?.play()
                    _isAlarmPlaying.value = true
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopAlarm() {
        currentRingtone?.stop()
        currentRingtone = null
        _isAlarmPlaying.value = false
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(2)
    }

    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        isBackgroundMusicPlaying = false
        
        if (!_isTimerCompleted.value) {
            stopAlarm()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Don't stop service when app is removed from recent tasks
        // Let it continue running in background
    }
} 