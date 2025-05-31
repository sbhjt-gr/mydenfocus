package com.gorai.myedenfocus.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log

class StudyReminderReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "StudyReminder"
        private const val WAKE_LOCK_TIMEOUT = 10*1000L // 10 seconds
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")
        
        if (intent.action == DailyStudyReminderService.ACTION_SHOW_NOTIFICATION) {
            Log.d(TAG, "Starting notification service")
            
            // Acquire wake lock to ensure device processes the notification
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MydenFocus:StudyReminderWakeLock"
            )
            wakeLock.acquire(WAKE_LOCK_TIMEOUT)
            
            try {
                // Start the service to show notification
                val serviceIntent = Intent(context, DailyStudyReminderService::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                Log.d(TAG, "Service started successfully")
                
                // Reschedule for next day
                val hour = intent.getIntExtra("notification_hour", -1)
                val minute = intent.getIntExtra("notification_minute", -1)
                
                if (hour != -1 && minute != -1) {
                    Log.d(TAG, "Rescheduling for next day at $hour:$minute")
                    DailyStudyReminderService.scheduleReminder(context, hour, minute)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service", e)
            } finally {
                // Release wake lock
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            }
        }
    }
} 