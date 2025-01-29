package com.gorai.myedenfocus.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.gorai.myedenfocus.R
import com.gorai.myedenfocus.receiver.StudyReminderReceiver
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class DailyStudyReminderService : Service() {

    companion object {
        private const val TAG = "DailyStudyReminderService"
        private const val CHANNEL_ID = "study_reminder_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_SHOW_NOTIFICATION = "com.gorai.myedenfocus.SHOW_NOTIFICATION"
        const val ALARM_REQUEST_CODE = 123
        
        fun scheduleReminder(context: Context, hour: Int, minute: Int) {
            Log.d(TAG, "Scheduling reminder for $hour:$minute")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(AlarmManager::class.java)
                if (!alarmManager.canScheduleExactAlarms()) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                    return
                }
            }

            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                val intent = Intent(context, StudyReminderReceiver::class.java).apply {
                    action = ACTION_SHOW_NOTIFICATION
                    putExtra("hour", hour)
                    putExtra("minute", minute)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    ALARM_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Cancel any existing alarms
                alarmManager.cancel(pendingIntent)

                val calendar = Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis()
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // If time has already passed today, schedule for tomorrow
                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }

                val scheduledTime = calendar.timeInMillis
                Log.d(TAG, "Current time: ${System.currentTimeMillis()}")
                Log.d(TAG, "Scheduled time: $scheduledTime")
                Log.d(TAG, "Time difference: ${scheduledTime - System.currentTimeMillis()}")

                // Set the alarm using AlarmManager.setAlarmClock
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(scheduledTime, pendingIntent),
                    pendingIntent
                )

                // Show confirmation notification
                showConfirmationNotification(context, hour, minute)
                Log.d(TAG, "Alarm scheduled successfully for ${calendar.time}")
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling reminder", e)
            }
        }

        fun cancelReminder(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, StudyReminderReceiver::class.java).apply {
                action = ACTION_SHOW_NOTIFICATION
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d(TAG, "Cancelled study reminder alarm")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling alarm", e)
            }
        }

        private fun showConfirmationNotification(context: Context, hour: Int, minute: Int) {
            createNotificationChannel(context)
            
            val formattedTime = String.format("%02d:%02d", hour, minute)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Study Reminder Set")
                .setContentText("You'll be reminded to study at $formattedTime")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID + 1, notification)
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
                }
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopSelf()
        return START_NOT_STICKY
    }
} 