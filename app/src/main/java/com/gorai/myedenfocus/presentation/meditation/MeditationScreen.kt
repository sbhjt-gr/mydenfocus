package com.gorai.myedenfocus.presentation.meditation

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gorai.myedenfocus.domain.model.MeditationSession
import com.gorai.myedenfocus.presentation.components.BottomBar
import com.gorai.myedenfocus.presentation.components.CommonTopBar
import com.gorai.myedenfocus.presentation.destinations.SettingsScreenDestination
import com.gorai.myedenfocus.service.MeditationTimerService
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.gorai.myedenfocus.util.NavigationStyles
import android.Manifest
import android.app.AlarmManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

private val meditationDurations = listOf(1, 5, 10, 15, 17, 20, 30)
private val meditationMusic = listOf(
    "No Music",
    "Wind Chimes Nature",
    "Soothing Chime",
    "Full Brain Drop Down",
    "Focus on Yourself"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreenTopBar() {
    CenterAlignedTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(32.dp)
                ) {
                    Image(
                        painter = painterResource(id = com.gorai.myedenfocus.R.drawable.logo),
                        contentDescription = "App Icon",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MyedenFocus",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun MeditationTimer(
    modifier: Modifier = Modifier,
    totalSeconds: Int,
    remainingSeconds: Int,
    isRunning: Boolean
) {
    LaunchedEffect(remainingSeconds) {
        println("Timer Update - Total: $totalSeconds, Remaining: $remainingSeconds, Running: $isRunning")
    }

    val safeProgress = remember { mutableStateOf(0f) }
    safeProgress.value = if (totalSeconds == 0) {
        0f
    } else {
        (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    }

    val progress by animateFloatAsState(
        targetValue = safeProgress.value,
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
            val safeProgress = progress.coerceIn(0f, 1f)
            val dotAngle = -90f + (360f * safeProgress)
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
    val view = LocalView.current
    
    Box(
        modifier = modifier.height(180.dp),
        contentAlignment = Alignment.Center
    ) {
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
            items(1) { Spacer(modifier = Modifier.height(60.dp)) }
            
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
            
            items(1) { Spacer(modifier = Modifier.height(60.dp)) }
        }

        LaunchedEffect(listState) {
            snapshotFlow { 
                val layoutInfo = listState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                val centerOffset = layoutInfo.viewportEndOffset / 2

                visibleItems.firstOrNull { item ->
                    val center = (item.offset + item.size / 2)
                    val distance = kotlin.math.abs(center - centerOffset)
                    distance < item.size / 2
                }?.index?.minus(1)
            }.collect { centerIndex ->
                if (centerIndex != null && centerIndex in meditationDurations.indices) {
                    val selectedDuration = meditationDurations[centerIndex]
                    onDurationSelected(selectedDuration)
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MusicSelector(
    selectedMusic: String,
    onMusicSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = meditationMusic.indexOf(selectedMusic)
    )
    val view = LocalView.current
    
    Box(
        modifier = modifier.height(180.dp),
        contentAlignment = Alignment.Center
    ) {
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
            items(1) { Spacer(modifier = Modifier.height(60.dp)) }
            
            items(meditationMusic) { music ->
                Box(
                    modifier = Modifier
                        .height(60.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = music,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (music == selectedMusic)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            items(1) { Spacer(modifier = Modifier.height(60.dp)) }
        }

        LaunchedEffect(listState) {
            snapshotFlow { 
                val layoutInfo = listState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                val centerOffset = layoutInfo.viewportEndOffset / 2

                visibleItems.firstOrNull { item ->
                    val center = (item.offset + item.size / 2)
                    val distance = kotlin.math.abs(center - centerOffset)
                    distance < item.size / 2
                }?.index?.minus(1)
            }.collect { centerIndex ->
                if (centerIndex != null && centerIndex in meditationMusic.indices) {
                    val selectedMusicOption = meditationMusic[centerIndex]
                    onMusicSelected(selectedMusicOption)
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
            }
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

@RootNavGraph
@Destination(
    route = "meditation",
    style = NavigationStyles.SlideTransition::class,
    deepLinks = [
        DeepLink(
            uriPattern = "myedenfocus://meditation"
        )
    ]
)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MeditationScreen(
    navigator: DestinationsNavigator,
    viewModel: MeditationViewModel = hiltViewModel()
) {
    var selectedMinutes by remember { mutableStateOf(17) }
    var selectedMusic by remember { mutableStateOf(meditationMusic[0]) }
    var isTimerRunning by rememberSaveable { mutableStateOf(false) }
    var showHeadphoneDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showDndPermissionDialog by remember { mutableStateOf(false) }
    var permissionMessage by remember { mutableStateOf("") }
    val context = LocalContext.current
    val remainingSeconds = MeditationTimerService.timerState.collectAsState().value
    val isAlarmPlaying = MeditationTimerService.isAlarmPlaying.collectAsState().value
    val isPaused = MeditationTimerService.isPaused.collectAsState().value
    val isTimerCompleted = MeditationTimerService.isTimerCompleted.collectAsState().value
    val timerState by MeditationTimerService.timerState.collectAsState()

    var sessionToDelete by remember { mutableStateOf<MeditationSession?>(null) }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // Define toggleTimer function
    val handleTimerToggle: () -> Unit = {
        val serviceIntent = Intent(context, MeditationTimerService::class.java).apply {
            if (!isTimerRunning) {
                action = MeditationTimerService.ACTION_START
                putExtra(MeditationTimerService.EXTRA_TIME, selectedMinutes * 60)
                putExtra("selected_duration", selectedMinutes)
                val musicFileName = when (selectedMusic) {
                    "Wind Chimes Nature" -> "wind_chimes_nature_symphony.mp3"
                    "Soothing Chime" -> "soothing_chime.mp3"
                    "Full Brain Drop Down" -> "full_brain_drop_down.mp3"
                    "Focus on Yourself" -> "focus_on_yourself.mp3"
                    else -> "no_music"
                }
                putExtra("selected_music", musicFileName)
            } else {
                action = MeditationTimerService.ACTION_STOP
            }
        }
        
        try {
            if (!isTimerRunning) {
                context.startForegroundService(serviceIntent)
            } else {
                context.stopService(serviceIntent)
            }
            isTimerRunning = !isTimerRunning
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Permission launcher for alarm permission
    val alarmPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            handleTimerToggle()
        } else {
            permissionMessage = "Alarm permission is required for the meditation timer to work properly."
            showPermissionDialog = true
        }
    }

    // Function to check and request permissions
    fun checkAndRequestPermissions() {
        if (!isTimerRunning) {
            // Check DND permission first
            if (!viewModel.checkDndPermission(context)) {
                showDndPermissionDialog = true
                return
            }

            // Check alarm permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    permissionMessage = "Please allow MyedenFocus to schedule alarms for the meditation timer."
                    showPermissionDialog = true
                    return
                }
            }

            // Check notification permission for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                when {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        handleTimerToggle()
                    }
                    else -> {
                        alarmPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            } else {
                handleTimerToggle()
            }
        } else {
            handleTimerToggle()
        }
    }

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

    // Update the timer completion handler
    LaunchedEffect(isTimerCompleted) {
        if (isTimerCompleted) {
            isTimerRunning = false
            
            // Only add session if timer actually completed (not reset/cancelled)
            if (remainingSeconds == 0) {
                // Get the actual selected duration from preferences
                val prefs = context.getSharedPreferences("meditation_prefs", Context.MODE_PRIVATE)
                val actualDuration = prefs.getInt("last_meditation_duration", selectedMinutes)
                viewModel.addSession(actualDuration)
                
                // Save meditation completion for today
                prefs.edit().putString("last_meditation_date", LocalDate.now().toString()).apply()

                // Show completion activity
                val intent = Intent(context, MeditationCompleteActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(MeditationCompleteActivity.EXTRA_DURATION, actualDuration)
                }
                context.startActivity(intent)
            }
            
            // Reset timer completed flag using the service method
            MeditationTimerService.resetTimerCompleted()
        }
    }

    // Clean up service when screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            // Don't stop service on screen dispose
            // Let it run in background
        }
    }

    val meditationSessions by viewModel.sessions.collectAsState()
    val now = LocalDateTime.now()

    // Group sessions by time range
    val thisWeekSessions = meditationSessions.filter { session ->
        ChronoUnit.DAYS.between(session.timestamp, now) <= 7
    }

    val olderSessions = meditationSessions.filter { session ->
        ChronoUnit.DAYS.between(session.timestamp, now) > 7
    }

    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = {
                Text(
                    text = "Delete Session",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this meditation session? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession(session)
                        sessionToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    val listState = rememberLazyListState()

    // Move LaunchedEffect here, outside of LazyColumn
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            listState.animateScrollToItem(0)
        }
    }

    if (showHeadphoneDialog) {
        AlertDialog(
            onDismissRequest = { showHeadphoneDialog = false },
            title = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Headphones Recommended",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            },
            text = {
                Text(
                    text = "For the best meditation experience, please connect headphones before starting the session.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showHeadphoneDialog = false }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showHeadphoneDialog = false
                        val serviceIntent = Intent(context, MeditationTimerService::class.java).apply {
                            action = MeditationTimerService.ACTION_START
                            putExtra(MeditationTimerService.EXTRA_TIME, selectedMinutes * 60)
                            val musicFileName = when (selectedMusic) {
                                "Wind Chimes Nature" -> "wind_chimes_nature_symphony.mp3"
                                "Soothing Chime" -> "soothing_chime.mp3"
                                "Full Brain Drop Down" -> "full_brain_drop_down.mp3"
                                "Focus on Yourself" -> "focus_on_yourself.mp3"
                                else -> "no_music"
                            }
                            putExtra("selected_music", musicFileName)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        }
                        isTimerRunning = true
                    }
                ) {
                    Text("Start Anyway")
                }
            }
        )
    }

    // Permission dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permission Required") },
            text = { Text(permissionMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                            // Open alarm settings
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Text("Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDndPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showDndPermissionDialog = false },
            title = {
                Text(
                    text = "Permission Required",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = "To ensure a distraction-free meditation session, please allow MyedenFocus to manage Do Not Disturb settings.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDndPermissionDialog = false
                        viewModel.openDndSettings(context)
                    }
                ) {
                    Text("Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDndPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Handle DND when timer starts/stops
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            viewModel.enableDnd(context)
        } else {
            viewModel.disableDnd(context)
        }
    }

    LaunchedEffect(timerState) {
        if (timerState == 0) {
            isTimerRunning = false
            selectedMinutes = 17
            selectedMusic = meditationMusic[0]
        }
    }

    Scaffold(
        topBar = {
            CommonTopBar(
                onSettingsClick = { 
                    navigator.navigate(SettingsScreenDestination())
                }
            )
        },
        bottomBar = {
            BottomBar(
                currentRoute = "meditate",
                onNavigate = { route ->
                    when (route) {
                        "schedule" -> navigator.navigateUp()
                        "chat" -> navigator.navigate("chat") {
                            popUpTo(route = "meditate") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
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
            state = listState,
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

                item {
                    Text(
                        text = "Select Music",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                item {
                    MusicSelector(
                        selectedMusic = selectedMusic,
                        onMusicSelected = { selectedMusic = it },
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
                            onClick = { checkAndRequestPermissions() },
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
            
            item {
                Text(
                    text = "Sessions This Week",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                
                if (thisWeekSessions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            thisWeekSessions.forEachIndexed { index, session ->
                                MeditationSessionItem(
                                    session = session,
                                    onDelete = {
                                        sessionToDelete = session
                                    }
                                )
                                if (index < thisWeekSessions.size - 1) {
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                }
                            }
                        }
                    }
                } else if (meditationSessions.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "You haven't meditated yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                }
            }

            item {
                if (olderSessions.isNotEmpty()) {
                    Text(
                        text = "Older Sessions",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            olderSessions.forEachIndexed { index, session ->
                                MeditationSessionItem(
                                    session = session,
                                    onDelete = {
                                        sessionToDelete = session
                                    }
                                )
                                if (index < olderSessions.size - 1) {
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MeditationSessionItem(
    session: MeditationSession,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = session.timestamp.format(
                    DateTimeFormatter.ofPattern("MMM dd, yyyy")
                ),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = session.timestamp.format(
                    DateTimeFormatter.ofPattern("hh:mm a")
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = "${session.duration} min",
            style = MaterialTheme.typography.bodyLarge
        )
        
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete session",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

// Extension function to check if service is running
fun Context.isServiceRunning(serviceClass: Class<*>): Boolean {
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    // For API 26 and above
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            // Check if service is foreground
            return ServiceUtils.isServiceForeground(this, serviceClass)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
    
    // Fallback for older versions
    @Suppress("DEPRECATION")
    return activityManager.getRunningServices(Integer.MAX_VALUE)
        .any { it.service.className == serviceClass.name }
}

// Update ServiceUtils object
object ServiceUtils {
    fun isServiceForeground(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return service.foreground
            }
        }
        return false
    }
} 