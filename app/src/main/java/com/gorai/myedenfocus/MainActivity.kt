package com.gorai.myedenfocus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.gorai.myedenfocus.domain.repository.PreferencesRepository
import com.gorai.myedenfocus.presentation.NavGraphs
import com.gorai.myedenfocus.presentation.destinations.OnboardingScreenDestination
import com.gorai.myedenfocus.presentation.destinations.SessionScreenRouteDestination
import com.gorai.myedenfocus.presentation.navigation.NavigationViewModel
import com.gorai.myedenfocus.presentation.session.StudySessionTimerService
import com.gorai.myedenfocus.presentation.theme.MydenFocusTheme
import com.gorai.myedenfocus.service.DailyStudyReminderService
import com.gorai.myedenfocus.util.LocalTimerService
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.navigation.dependency
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var reviewManager: ReviewManager

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private val navigationViewModel: NavigationViewModel by viewModels()

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

    private var currentRoute: String? = null

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
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        setupReviewManager()
        
        setContent {
            val isOnboardingCompleted by preferencesRepository.isOnboardingCompleted.collectAsStateWithLifecycle(initialValue = null)
            val navController = rememberNavController()
            
            if (isOnboardingCompleted != null) {
                MydenFocusTheme {
                    if (isBound) {
                        CompositionLocalProvider(LocalTimerService provides timerService) {
                            DestinationsNavHost(
                                navController = navController,
                                navGraph = NavGraphs.root,
                                startRoute = if (isOnboardingCompleted == true) NavGraphs.root.startRoute else OnboardingScreenDestination,
                                dependenciesContainerBuilder = {
                                    dependency(SessionScreenRouteDestination) { timerService }
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            LaunchedEffect(navController) {
                                navController.currentBackStackEntryFlow.collect { entry ->
                                    currentRoute = entry.destination.route
                                }
                            }

                            // Handle saved state
                            LaunchedEffect(Unit) {
                                savedInstanceState?.getString("LAST_ROUTE")?.let { lastRoute ->
                                    if (lastRoute != navController.currentDestination?.route) {
                                        navController.navigate(lastRoute)
                                    }
                                }
                            }

                            // Handle notification click navigation
                            LaunchedEffect(intent) {
                                if (intent?.action == "OPEN_FROM_NOTIFICATION" || 
                                    intent?.getBooleanExtra("navigate_to_session", false) == true) {
                                    navigationViewModel.navigateTo(SessionScreenRouteDestination.route)
                                }
                            }
                        }
                    } else {
                        // Show loading state or placeholder while service is binding
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }

    private fun setupReviewManager() {
        reviewManager = ReviewManagerFactory.create(this)
    }

    private fun checkAndShowReview() {
        lifecycleScope.launch {
            val hasRated = preferencesRepository.hasRatedApp.first()
            val completedSessions = preferencesRepository.completedSessionsCount.first()
            val lastReviewTime = preferencesRepository.lastReviewTime.first()
            
            val shouldShowReview = !hasRated && 
                completedSessions >= 5 && 
                (System.currentTimeMillis() - lastReviewTime >= 7 * 24 * 60 * 60 * 1000) // 7 days
            
            if (shouldShowReview) {
                val request = reviewManager.requestReviewFlow()
                request.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val reviewInfo = task.result
                        val flow = reviewManager.launchReviewFlow(this@MainActivity, reviewInfo)
                        flow.addOnCompleteListener {
                            lifecycleScope.launch {
                                preferencesRepository.updateLastReviewTime()
                                if (it.isSuccessful) {
                                    preferencesRepository.setHasRatedApp(true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndShowReview()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent?.action == "OPEN_FROM_NOTIFICATION") {
            navigationViewModel.navigateTo(SessionScreenRouteDestination.route)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        isBound = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentRoute?.let { route ->
            outState.putString("LAST_ROUTE", route)
        }
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