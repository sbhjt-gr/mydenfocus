package com.gorai.myedenfocus.presentation.meditation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.media.MediaPlayer
import android.media.RingtoneManager
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.gorai.myedenfocus.presentation.components.BottomBar
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreenTopBar() {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "MyedenFocus",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun MeditationScreen(
    navigator: DestinationsNavigator
) {
    var selectedMinutes by remember { mutableStateOf(16) }
    var remainingSeconds by remember { mutableStateOf(16 * 60) }
    var isTimerRunning by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    LaunchedEffect(isTimerRunning) {
        while(isTimerRunning && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
            
            if (remainingSeconds == 0) {
                isTimerRunning = false
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                MediaPlayer.create(context, notification).start()
            }
        }
    }

    val formattedTime = remember(remainingSeconds) {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    Scaffold(
        topBar = {
            DashboardScreenTopBar()
        },
        bottomBar = {
            BottomBar(
                currentRoute = "meditate",
                onNavigate = { route: String ->
                    when (route) {
                        "schedule" -> navigator.navigateUp()
                        else -> Unit
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!isTimerRunning) {
                Text(
                    text = "Set Meditation Duration",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        onClick = { 
                            if (selectedMinutes > 1) selectedMinutes-- 
                            remainingSeconds = selectedMinutes * 60
                        }
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown, 
                            contentDescription = "Decrease time"
                        )
                    }
                    
                    Text(
                        text = "$selectedMinutes min",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    
                    IconButton(
                        onClick = { 
                            if (selectedMinutes < 60) selectedMinutes++
                            remainingSeconds = selectedMinutes * 60
                        }
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp, 
                            contentDescription = "Increase time"
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.displayLarge
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { 
                        if (!isTimerRunning) {
                            remainingSeconds = selectedMinutes * 60
                        }
                        isTimerRunning = !isTimerRunning 
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTimerRunning) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isTimerRunning) 
                                Icons.Default.Close 
                            else 
                                Icons.Default.PlayArrow,
                            contentDescription = if (isTimerRunning) "Stop" else "Start"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isTimerRunning) "Stop" else "Start")
                    }
                }
                
                if (!isTimerRunning && remainingSeconds < selectedMinutes * 60) {
                    Button(
                        onClick = { remainingSeconds = selectedMinutes * 60 }
                    ) {
                        Text("Reset")
                    }
                }
            }
            
            if (isTimerRunning) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Take deep breaths...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
} 