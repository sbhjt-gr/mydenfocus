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
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.os.CountDownTimer
import android.provider.Settings
import android.app.AlarmManager
import com.gorai.myedenfocus.presentation.meditation.MeditationCompleteActivity

class MeditationTimerService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var timeLeft = 0
    private var isTimerRunning = false
    private var mediaPlayer: MediaPlayer? = null
    private var selectedDuration = 0
    private var timer: CountDownTimer? = null
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var initialTime: Int = 0
    private var isPaused = false
    private var remainingTime: Long = 0
    
    companion object {
        const val CHANNEL_ID = "meditation_timer_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        const val ACTION_STOP_TIMER = "STOP_TIMER"
        const val ACTION_PAUSE = "PAUSE"
        const val ACTION_RESUME = "RESUME"
        const val ACTION_RESET = "RESET"
        const val ACTION_SHOW_COMPLETION_DIALOG = "SHOW_COMPLETION_DIALOG"
        const val EXTRA_TIME = "time_in_seconds"
        const val ACTION_STOP_ALARM = "STOP_ALARM"
        
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
            try {
                currentRingtone?.stop()
                currentRingtone?.play() // Force a state change
                currentRingtone?.stop() // Stop again to ensure it's stopped
                currentRingtone = null
                _isAlarmPlaying.value = false
            } catch (e: Exception) {
                android.util.Log.e("MeditationTimer", "Error stopping alarm sound", e)
            }
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
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                timeLeft = intent.getIntExtra(EXTRA_TIME, 0)
                selectedDuration = intent.getIntExtra("selected_duration", 0)
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
            ACTION_SHOW_COMPLETION_DIALOG -> showCompletionDialog()
            ACTION_STOP_ALARM -> {
                stopAlarm()
                stopSelf()
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
        
        // Set exact alarm for timer completion
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + (initialTime * 1000L)
        
        val completionIntent = Intent(this, MeditationTimerService::class.java).apply {
            action = ACTION_SHOW_COMPLETION_DIALOG
        }
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            completionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                pendingIntent
            )
        }
        
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
            }
            
            // Stop the timer and save session when it reaches zero
            if (timeLeft == 0) {
                isTimerRunning = false
                
                // Save meditation session only if not already saved
                if (!_isTimerCompleted.value) {
                    _isTimerCompleted.value = true
                    val prefs = getSharedPreferences("meditation_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putInt("last_meditation_duration", selectedDuration)
                        .putString("last_meditation_date", java.time.LocalDate.now().toString())
                        .apply()
                }
                
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                isBackgroundMusicPlaying = false
            }
        }
    }

    private fun stopTimer() {
        isTimerRunning = false
        timeLeft = 0
        _timerState.value = 0
        
        // Cancel the alarm
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val completionIntent = Intent(this, MeditationTimerService::class.java).apply {
            action = ACTION_SHOW_COMPLETION_DIALOG
        }
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            completionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        
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

    private fun showCompletionDialog() {
        try {
            // Play alarm sound
            try {
                currentRingtone?.stop()
                currentRingtone = null
                
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                currentRingtone = RingtoneManager.getRingtone(applicationContext, notification)
                currentRingtone?.play()
                _isAlarmPlaying.value = true
            } catch (e: Exception) {
                android.util.Log.e("MeditationTimer", "Error playing alarm sound", e)
            }

            // Launch the alarm activity
            val intent = Intent(this, MeditationCompleteActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)

            // Create a high-priority notification
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val alarmChannel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel(
                    "meditation_alarm_channel",
                    "Meditation Alarm",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    enableLights(true)
                    enableVibration(true)
                }
            } else null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && alarmChannel != null) {
                notificationManager.createNotificationChannel(alarmChannel)
            }

            val alarmIntent = Intent(this, MeditationCompleteActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, "meditation_alarm_channel")
                .setContentTitle("Meditation Complete!")
                .setContentText("Tap to stop the alarm")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setOngoing(true)
                .build()

            notificationManager.notify(2, notification)

        } catch (e: Exception) {
            android.util.Log.e("MeditationTimer", "Error showing completion dialog", e)
            e.printStackTrace()
        }
    }

    fun stopAlarm() {
        // Stop all sounds
        currentRingtone?.stop()
        currentRingtone = null
        _isAlarmPlaying.value = false
        
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        isBackgroundMusicPlaying = false
        
        // Cancel any pending alarms
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val completionIntent = Intent(this, MeditationTimerService::class.java).apply {
            action = ACTION_SHOW_COMPLETION_DIALOG
        }
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            completionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        
        // Clear notifications
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(2)
        notificationManager.cancel(NOTIFICATION_ID)
        
        // Reset all states
        isTimerRunning = false
        timeLeft = 0
        _timerState.value = 0
        _isTimerCompleted.value = false
        _isAlarmPlaying.value = false
        isServiceRunning = false
        
        // Stop service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        
        // Clean up all media resources
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        isBackgroundMusicPlaying = false
        
        // Stop alarm if service is destroyed
        currentRingtone?.stop()
        currentRingtone = null
        _isAlarmPlaying.value = false
        
        // Cancel any pending alarms
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val completionIntent = Intent(this, MeditationTimerService::class.java).apply {
            action = ACTION_SHOW_COMPLETION_DIALOG
        }
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            completionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Don't stop service when app is removed from recent tasks
        // Let it continue running in background
    }
} 