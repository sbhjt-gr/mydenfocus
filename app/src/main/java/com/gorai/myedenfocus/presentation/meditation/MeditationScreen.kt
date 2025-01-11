package com.gorai.myedenfocus.presentation.meditation

import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gorai.myedenfocus.presentation.components.BottomBar
import com.gorai.myedenfocus.service.MeditationTimerService
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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

@Composable
fun MeditationTimer(
    modifier: Modifier = Modifier,
    totalSeconds: Int,
    remainingSeconds: Int,
    isRunning: Boolean
) {
    val progress by animateFloatAsState(
        targetValue = remainingSeconds.toFloat() / totalSeconds.toFloat(),
        animationSpec = tween(500),
        label = "timer_progress"
    )

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        TimerCircle(
            progress = progress,
            isRunning = isRunning
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                style = MaterialTheme.typography.displayLarge
            )
            if (isRunning) {
                Text(
                    text = "Breathe deeply...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun TimerCircle(
    progress: Float,
    isRunning: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2
        
        // Draw outer circle
        drawCircle(
            color = colorScheme.surfaceVariant.copy(alpha = 0.3f),
            radius = radius,
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Draw tick marks and numbers
        for (i in 0..60) {
            val angle = (2 * PI * i) / 60
            val tickLength = if (i % 5 == 0) 20.dp.toPx() else 10.dp.toPx()
            val startRadius = radius - tickLength
            val endRadius = radius
            
            val start = Offset(
                (center.x + cos(angle) * startRadius).toFloat(),
                (center.y + sin(angle) * startRadius).toFloat()
            )
            val end = Offset(
                (center.x + cos(angle) * endRadius).toFloat(),
                (center.y + sin(angle) * endRadius).toFloat()
            )
            
            val tickColor = if (i % 5 == 0)
                colorScheme.primary
            else
                colorScheme.outline.copy(alpha = 0.5f)
            
            drawLine(
                color = tickColor,
                start = start,
                end = end,
                strokeWidth = if (i % 5 == 0) 2.dp.toPx() else 1.dp.toPx()
            )
        }
        
        // Draw progress arc
        drawArc(
            color = colorScheme.primary,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            style = Stroke(
                width = 12.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
        
        // Draw inner circles
        drawCircle(
            color = colorScheme.surfaceVariant,
            radius = radius * 0.8f,
            style = Stroke(width = 4.dp.toPx())
        )
        
        drawCircle(
            color = colorScheme.surfaceVariant.copy(alpha = 0.3f),
            radius = radius * 0.7f,
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Draw moving dot
        if (isRunning) {
            val dotAngle = -90f + (360f * progress)
            val dotRadius = radius * 0.9f
            val dotCenter = Offset(
                center.x + (dotRadius * cos(Math.toRadians(dotAngle.toDouble()))).toFloat(),
                center.y + (dotRadius * sin(Math.toRadians(dotAngle.toDouble()))).toFloat()
            )
            drawCircle(
                color = colorScheme.primary,
                radius = 6.dp.toPx(),
                center = dotCenter
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun MeditationScreen(
    navigator: DestinationsNavigator
) {
    var selectedMinutes by remember { mutableStateOf(5) }
    var isTimerRunning by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val remainingSeconds = MeditationTimerService.timerState.collectAsState().value
    
    // Clean up service when screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            context.stopService(Intent(context, MeditationTimerService::class.java))
        }
    }

    // Start/Stop timer service
    @RequiresApi(Build.VERSION_CODES.O)
    fun toggleTimer() {
        val serviceIntent = Intent(context, MeditationTimerService::class.java).apply {
            if (!isTimerRunning) {
                action = MeditationTimerService.ACTION_START
                putExtra(MeditationTimerService.EXTRA_TIME, selectedMinutes * 60)
            } else {
                action = MeditationTimerService.ACTION_STOP
            }
        }
        
        try {
            if (!isTimerRunning) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                }
            } else {
                context.stopService(serviceIntent)
            }
            isTimerRunning = !isTimerRunning
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            DashboardScreenTopBar()
        },
        bottomBar = {
            BottomBar(
                currentRoute = "meditate",
                onNavigate = { route ->
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
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isTimerRunning) {
                Text(
                    text = "Set Meditation Duration",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    IconButton(
                        onClick = { 
                            if (selectedMinutes > 1) {
                                selectedMinutes--
                            }
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
                            if (selectedMinutes < 60) {
                                selectedMinutes++
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "Increase time"
                        )
                    }
                }
            }
            
            MeditationTimer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                totalSeconds = selectedMinutes * 60,
                remainingSeconds = remainingSeconds,
                isRunning = isTimerRunning
            )
            
            Button(
                onClick = { toggleTimer() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTimerRunning) 
                        MaterialTheme.colorScheme.error
                    else 
                        MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(16.dp)
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
        }
    }
} 