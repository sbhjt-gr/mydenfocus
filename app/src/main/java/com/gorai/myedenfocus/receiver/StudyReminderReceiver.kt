package com.gorai.myedenfocus.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.gorai.myedenfocus.MainActivity
import com.gorai.myedenfocus.R
import com.gorai.myedenfocus.service.DailyStudyReminderService
import java.util.Calendar

class StudyReminderReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "StudyReminderReceiver"
        private const val CHANNEL_ID = "study_reminder_channel"
        private const val NOTIFICATION_ID = 2
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MydenFocus:StudyReminderWakeLock"
        )
        wakeLock.acquire(30000) // 30 seconds

        try {
            if (intent.action == DailyStudyReminderService.ACTION_SHOW_NOTIFICATION) {
                val hour = intent.getIntExtra("hour", -1)
                val minute = intent.getIntExtra("minute", -1)
                Log.d(TAG, "Processing alarm for $hour:$minute")

                // Show the notification
                showNotification(context)

                // Schedule next alarm for tomorrow
                if (hour != -1 && minute != -1) {
                    scheduleNextAlarm(context, hour, minute)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing alarm", e)
        } finally {
            wakeLock.release()
        }
    }

    private fun showNotification(context: Context) {
        createNotificationChannel(context)

        // Create intent for MainActivity
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to_session", true)
            action = "OPEN_FROM_NOTIFICATION"
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Time to Study!")
            .setContentText("It is your time that you start studying right now.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setTimeoutAfter(300000) // 5 minutes
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun scheduleNextAlarm(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, StudyReminderReceiver::class.java).apply {
            action = DailyStudyReminderService.ACTION_SHOW_NOTIFICATION
            putExtra("hour", hour)
            putExtra("minute", minute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DailyStudyReminderService.ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1) // Schedule for tomorrow
        }

        val scheduledTime = calendar.timeInMillis
        Log.d(TAG, "Scheduling next alarm for ${calendar.time}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(scheduledTime, pendingIntent),
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                scheduledTime,
                pendingIntent
            )
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Study Reminder"
            val descriptionText = "Channel for study reminder notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setBypassDnd(true) // Bypass Do Not Disturb
                setSound(null, null) // We'll use notification defaults
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
} 