package com.gorai.myedenfocus.presentation.meditation

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale
import com.gorai.myedenfocus.service.MeditationTimerService.Companion.isTimerCompleted

private val meditationDurations = listOf(1, 5, 10, 15, 17, 20, 30)

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DurationSelector(
    selectedMinutes: Int,
    onDurationSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = meditationDurations.indexOf(selectedMinutes)
    )
    
    Box(
        modifier = modifier
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        // Selection indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        )
        
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Add padding items at top and bottom for infinite scroll feel
            items(2) { Spacer(modifier = Modifier.height(60.dp)) }
            
            items(meditationDurations) { duration ->
                Box(
                    modifier = Modifier
                        .height(60.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$duration min",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (duration == selectedMinutes)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Add padding items at top and bottom for infinite scroll feel
            items(2) { Spacer(modifier = Modifier.height(60.dp)) }
        }
    }
    
    // Update selected duration when scrolling stops
    LaunchedEffect(listState.firstVisibleItemIndex) {
        val selectedIndex = listState.firstVisibleItemIndex - 2 // Adjust for padding items
        if (selectedIndex in meditationDurations.indices) {
            onDurationSelected(meditationDurations[selectedIndex])
        }
    }
}

@Composable
private fun PulsingStopButton(onClick: () -> Unit) {
    val pulseAnim = rememberInfiniteTransition()
    val scale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        )
    )

    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Stop Sound",
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Stop\nAlarm",
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
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
    var selectedMinutes by remember { mutableStateOf(17) }
    var isTimerRunning by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val remainingSeconds = MeditationTimerService.timerState.collectAsState().value
    val isAlarmPlaying = MeditationTimerService.isAlarmPlaying.collectAsState().value
    val isPaused = MeditationTimerService.isPaused.collectAsState().value
    val isTimerCompleted = MeditationTimerService.isTimerCompleted.collectAsState().value
    
    // Handle deep linking in a composable context
    LaunchedEffect(Unit) {
        val activity = context as? Activity
        val intent = activity?.intent
        if (intent?.getBooleanExtra("openMeditation", false) == true) {
            // Clear the extra to prevent repeated handling
            intent.removeExtra("openMeditation")
        }
    }

    // Check if service is running when screen is created
    LaunchedEffect(Unit) {
        if (context.isServiceRunning(MeditationTimerService::class.java)) {
            isTimerRunning = true
        }
    }

    // Play alarm when timer completes
    LaunchedEffect(isTimerCompleted) {
        if (isTimerCompleted) {
            isTimerRunning = false
        }
    }

    // Clean up service when screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            // Don't stop service on screen dispose
            // Let it run in background
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
        },
        floatingActionButton = {
            if (isAlarmPlaying) {
                PulsingStopButton(onClick = { MeditationTimerService.stopAlarmStatic() })
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isTimerRunning) {
                item {
                    Text(
                        text = "Select Meditation Duration",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                item {
                    DurationSelector(
                        selectedMinutes = selectedMinutes,
                        onDurationSelected = { selectedMinutes = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }
            
            item {
                MeditationTimer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    totalSeconds = selectedMinutes * 60,
                    remainingSeconds = remainingSeconds,
                    isRunning = isTimerRunning
                )
            }
            
            item {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTimerRunning && !isPaused) {
                        Button(
                            onClick = {
                                val intent = Intent(context, MeditationTimerService::class.java)
                                    .setAction(MeditationTimerService.ACTION_PAUSE)
                                context.startService(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .height(56.dp)
                                .width(120.dp)
                        ) {
                            Text("Pause", style = MaterialTheme.typography.titleMedium)
                        }
                        
                        Button(
                            onClick = {
                                val intent = Intent(context, MeditationTimerService::class.java)
                                    .setAction(MeditationTimerService.ACTION_RESET)
                                context.startService(intent)
                                isTimerRunning = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .height(56.dp)
                                .width(120.dp)
                        ) {
                            Text("Reset", style = MaterialTheme.typography.titleMedium)
                        }
                    } else if (isPaused) {
                        Button(
                            onClick = {
                                val intent = Intent(context, MeditationTimerService::class.java)
                                    .setAction(MeditationTimerService.ACTION_RESUME)
                                context.startService(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .height(56.dp)
                                .width(120.dp)
                        ) {
                            Text("Resume", style = MaterialTheme.typography.titleMedium)
                        }
                        Button(
                            onClick = {
                                val intent = Intent(context, MeditationTimerService::class.java)
                                    .setAction(MeditationTimerService.ACTION_RESET)
                                context.startService(intent)
                                isTimerRunning = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .height(56.dp)
                                .width(120.dp)
                        ) {
                            Text("Reset", style = MaterialTheme.typography.titleMedium)
                        }
                    } else {
                        Button(
                            onClick = { toggleTimer() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .height(56.dp)
                                .width(120.dp)
                        ) {
                            Text("Start", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

// Extension function to check if service is running
private fun Context.isServiceRunning(serviceClass: Class<*>): Boolean {
    val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return manager.getRunningServices(Integer.MAX_VALUE)
        .any { it.service.className == serviceClass.name }
} 