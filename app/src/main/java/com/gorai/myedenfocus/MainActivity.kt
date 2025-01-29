package com.gorai.myedenfocus

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.gorai.myedenfocus.domain.repository.PreferencesRepository
import com.gorai.myedenfocus.presentation.NavGraphs
import com.gorai.myedenfocus.presentation.destinations.OnboardingScreenDestination
import com.gorai.myedenfocus.presentation.destinations.SessionScreenRouteDestination
import com.gorai.myedenfocus.presentation.navigation.NavigationViewModel
import com.gorai.myedenfocus.presentation.session.StudySessionTimerService
import com.gorai.myedenfocus.presentation.theme.MyedenFocusTheme
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.navigation.dependency
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.gorai.myedenfocus.service.DailyStudyReminderService
import java.util.*

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private var isBound by mutableStateOf(false)
    private lateinit var timerService: StudySessionTimerService
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, service: IBinder?) {
            val binder = service as StudySessionTimerService.StudySessionTimerBinder
            timerService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isBound = false
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, StudySessionTimerService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleAlarmIntent(intent)
        
        setContent {
            val isOnboardingCompleted by preferencesRepository.isOnboardingCompleted.collectAsStateWithLifecycle(initialValue = null)
            
            if (isOnboardingCompleted != null) {
                MyedenFocusTheme {
                    val navController = rememberNavController()
                    
                    // Handle notification click navigation
                    LaunchedEffect(intent) {
                        if (intent?.getBooleanExtra("navigate_to_session", false) == true) {
                            // Clear the extra to prevent repeated navigation
                            intent.removeExtra("navigate_to_session")
                            // Navigate using navController
                            navController.navigate(SessionScreenRouteDestination.route)
                        }
                    }

                    DestinationsNavHost(
                        navGraph = NavGraphs.root,
                        startRoute = if (isOnboardingCompleted == true) NavGraphs.root.startRoute else OnboardingScreenDestination,
                        dependenciesContainerBuilder = {
                            dependency(SessionScreenRouteDestination) { timerService }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleAlarmIntent(intent)
        // Handle new intents while app is running
        intent?.getStringExtra("navigation_route")?.let { route ->
            val viewModel = (this as ComponentActivity).defaultViewModelProviderFactory
                .create(NavigationViewModel::class.java)
            viewModel.navigateTo(route)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        isBound = false
    }

    private fun handleAlarmIntent(intent: Intent?) {
        if (intent?.action == DailyStudyReminderService.ACTION_SHOW_NOTIFICATION && 
            intent.getBooleanExtra("isFromAlarm", false)) {
            showNotification()
            
            // Reschedule for next day
            val calendar = Calendar.getInstance()
            DailyStudyReminderService.scheduleReminder(
                this,
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE)
            )
        }
    }

    private fun showNotification() {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Time to Study!")
            .setContentText("Your scheduled study time has started")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
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
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "study_reminder_channel"
        private const val NOTIFICATION_ID = 2
    }
}