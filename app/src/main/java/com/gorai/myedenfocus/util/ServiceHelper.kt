package com.gorai.myedenfocus.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.gorai.myedenfocus.MainActivity
import com.gorai.myedenfocus.R
import com.gorai.myedenfocus.presentation.session.StudySessionTimerService

object ServiceHelper {
    const val CHANNEL_ID = "study_timer_channel"
    const val NOTIFICATION_ID = 1
    const val FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Study Timer",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Used for study timer notifications"
            }
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createNotification(
        context: Context,
        hours: String,
        minutes: String,
        seconds: String
    ): NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle("Study Timer")
            .setContentText("$hours:$minutes:$seconds")
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    }

    fun triggerForegroundService(
        context: Context,
        action: String,
        duration: Int = 0,
        topicId: Int = -1,
        subjectId: Int = -1
    ) {
        Intent(context, StudySessionTimerService::class.java).apply {
            this.action = action
            putExtra("DURATION", duration)
            putExtra("TOPIC_ID", topicId)
            putExtra("SUBJECT_ID", subjectId)
            context.startService(this)
        }
    }
} 