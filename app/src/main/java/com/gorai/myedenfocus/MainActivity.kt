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
}