package com.gorai.myedenfocus.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gorai.myedenfocus.R
import com.gorai.myedenfocus.domain.model.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

class TaskNotificationService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    
    companion object {
        const val CHANNEL_ID = "task_reminder_channel"
        const val ACTION_MARK_COMPLETE = "MARK_COMPLETE"
        const val ACTION_REMIND_LATER = "REMIND_LATER"
        const val EXTRA_TASK_ID = "task_id"
        const val ACTION_SCHEDULE = "SCHEDULE"
        const val EXTRA_DUE_DATE = "due_date"
        
        fun scheduleNotification(context: Context, task: Task) {
            val intent = Intent(context, TaskNotificationService::class.java).apply {
                action = ACTION_SCHEDULE
                putExtra(EXTRA_TASK_ID, task.taskId)
                putExtra(EXTRA_DUE_DATE, task.dueDate)
            }
            
            if (isToday(task.dueDate)) {
                context.startService(intent)
            } else {
                // For future dates, just start the service - it will handle the scheduling
                context.startService(intent)
            }
        }
        
        private fun isToday(timestamp: Long): Boolean {
            val today = Calendar.getInstance()
            val taskDate = Calendar.getInstance().apply {
                timeInMillis = timestamp
            }
            
            return today.get(Calendar.YEAR) == taskDate.get(Calendar.YEAR) &&
                   today.get(Calendar.DAY_OF_YEAR) == taskDate.get(Calendar.DAY_OF_YEAR)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskId = intent?.getIntExtra(EXTRA_TASK_ID, -1) ?: -1
        
        when(intent?.action) {
            ACTION_MARK_COMPLETE -> markTaskComplete(taskId)
            ACTION_REMIND_LATER -> scheduleReminder(taskId)
            ACTION_SCHEDULE -> {
                val dueDate = intent.getLongExtra(EXTRA_DUE_DATE, -1)
                if (dueDate != -1L) {
                    scheduleNotificationForDate(taskId, dueDate)
                }
            }
            else -> showTaskNotification(taskId)
        }
        
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for task reminders"
                setShowBadge(true)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showTaskNotification(taskId: Int) {
        val markCompleteIntent = Intent(this, TaskNotificationService::class.java).apply {
            action = ACTION_MARK_COMPLETE
            putExtra(EXTRA_TASK_ID, taskId)
        }
        
        val remindLaterIntent = Intent(this, TaskNotificationService::class.java).apply {
            action = ACTION_REMIND_LATER
            putExtra(EXTRA_TASK_ID, taskId)
        }
        
        val markCompletePendingIntent = PendingIntent.getService(
            this, 0, markCompleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val remindLaterPendingIntent = PendingIntent.getService(
            this, 1, remindLaterIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Task Due Today!")
            .setContentText("You have a task that needs to be completed today")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .addAction(0, "Mark Complete", markCompletePendingIntent)
            .addAction(0, "Remind in 3hrs", remindLaterPendingIntent)
            .build()

        startForeground(taskId, notification)
    }

    private fun markTaskComplete(taskId: Int) {
        // Update task in database
        // Stop the notification
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun scheduleReminder(taskId: Int) {
        serviceScope.launch {
            delay(3 * 60 * 60 * 1000) // 3 hours
            showTaskNotification(taskId)
        }
    }

    private fun scheduleNotificationForDate(taskId: Int, dueDate: Long) {
        serviceScope.launch {
            if (!isToday(dueDate)) {
                val delay = dueDate - System.currentTimeMillis()
                delay(delay)
            }
            showTaskNotification(taskId)
        }
    }
} 