package com.gorai.myedenfocus.presentation.session

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowCircleRight
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gorai.myedenfocus.domain.model.Subject
import com.gorai.myedenfocus.domain.model.Task
import com.gorai.myedenfocus.presentation.components.DeleteDialog
import com.gorai.myedenfocus.presentation.components.MeditationReminderDialog
import com.gorai.myedenfocus.presentation.components.SubjectListBottomSheet
import com.gorai.myedenfocus.presentation.components.studySessionsList
import com.gorai.myedenfocus.presentation.destinations.MeditationScreenDestination
import com.gorai.myedenfocus.presentation.theme.Red
import com.gorai.myedenfocus.util.Constants.ACTION_SERVICE_CANCEL
import com.gorai.myedenfocus.util.Constants.ACTION_SERVICE_START
import com.gorai.myedenfocus.util.Constants.ACTION_SERVICE_STOP
import com.gorai.myedenfocus.util.NavAnimation
import com.gorai.myedenfocus.util.Priority
import com.gorai.myedenfocus.util.ServiceHelper
import com.gorai.myedenfocus.util.SnackbarEvent
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

data class SessionScreenNavArgs(
    val preSelectedTopicId: Int? = null,
    val preSelectedSubjectId: Int? = null
)

@Destination(
    deepLinks = [
        DeepLink(
            action = Intent.ACTION_VIEW,
            uriPattern = "myedenfocus://dashboard/session"
        )
    ],
    style = NavAnimation::class
)

@Composable
fun SessionScreenRoute(
    navigator: DestinationsNavigator,
    timerService: StudySessionTimerService,
    preSelectedTopicId: Int? = null,
    preSelectedSubjectId: Int? = null
) {
    val viewModel: SessionViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentTopicId by timerService.topicId
    val currentTimerState by timerService.currentTimerState
    
    LaunchedEffect(preSelectedTopicId, preSelectedSubjectId) {
        if (preSelectedTopicId != null) {
            viewModel.onEvent(SessionEvent.InitializeWithTopic(preSelectedTopicId))
            viewModel.getTaskById(preSelectedTopicId)?.let { task ->
                timerService.setDuration(task.taskDuration)
            }
        }
    }

    // Initialize with running timer's topic
    LaunchedEffect(currentTopicId, currentTimerState) {
        if (currentTimerState != TimerState.IDLE) {
            currentTopicId?.let { runningTopicId ->
                viewModel.onEvent(SessionEvent.InitializeWithTopic(runningTopicId))
                viewModel.getTaskById(runningTopicId)?.let { task ->
                    timerService.setDuration(task.taskDuration)
                }
            }
        }
    }

    SessionScreen(
        state = state,
        snackbarEvent = viewModel.snackbarEventFlow,
        onEvent = viewModel::onEvent,
        onBackButtonClick = { navigator.navigateUp() },
        timerService = timerService,
        navigator = navigator
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionScreen(
    state: SessionState,
    snackbarEvent: SharedFlow<SnackbarEvent>,
    onEvent: (SessionEvent) -> Unit,
    onBackButtonClick: () -> Unit,
    timerService: StudySessionTimerService,
    navigator: DestinationsNavigator
) {
    val hours by timerService.hours
    val minutes by timerService.minutes
    val seconds by timerService.seconds
    val currentTimerState by timerService.currentTimerState

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    var isBottomSheetOpen by remember {
        mutableStateOf(false)
    }
    val scope = rememberCoroutineScope()

    var isDeleteDialogOpen by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val prefs = context.getSharedPreferences("meditation_prefs", Context.MODE_PRIVATE)
    
    val lastMeditationDate = prefs.getString("last_meditation_date", null)
    val today = LocalDate.now().toString()
    val hasMeditatedToday = lastMeditationDate == today

    var showMeditationDialog by remember { mutableStateOf(false) }

    val isAlarmPlaying by StudySessionTimerService.isAlarmPlaying.collectAsState()

    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionMessage by remember { mutableStateOf("") }
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Track DND permission state
    var hasDndPermission by remember { 
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.isNotificationPolicyAccessGranted
            } else true
        )
    }

    // Check permission state changes
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000) // Check every second
            val currentPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.isNotificationPolicyAccessGranted
            } else true
            
            if (currentPermission != hasDndPermission) {
                hasDndPermission = currentPermission
                if (currentPermission) {
                    // Permission was just granted, refresh the screen
                    onEvent(SessionEvent.RefreshScreen)
                }
            }
        }
    }

    // Function to check DND permission
    fun checkDndPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.isNotificationPolicyAccessGranted
        } else true
    }

    // Function to open DND settings
    fun openDndSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    if (showMeditationDialog) {
        MeditationReminderDialog(
            onDismissRequest = { showMeditationDialog = false },
            onMeditateClick = {
                showMeditationDialog = false
                navigator.navigate(MeditationScreenDestination)
            },
            onSkipClick = {
                showMeditationDialog = false
                // Start timer when skipping meditation
                if (state.selectedDuration > 0) {
                    ServiceHelper.triggerForegroundService(
                        context = context,
                        action = ACTION_SERVICE_START,
                        duration = state.selectedDuration,
                        topicId = state.selectedTopicId,
                        subjectId = state.subjectId
                    )
                }
            }
        )
    }

    // Add DND permission dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permission Required") },
            text = { 
                Text(
                    text = "To ensure a distraction-free study session, please allow MyedenFocus to manage Do Not Disturb settings.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        openDndSettings()
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

    LaunchedEffect(key1 = true) {
        snackbarEvent.collectLatest {
                event -> when(event) {
                is SnackbarEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = event.duration
                    )
                }
                SnackbarEvent.NavigateUp -> {}
            }
        }
    }

    LaunchedEffect(key1 = state.subjects) {
        if (state.subjects.isNotEmpty() && state.subjectId == null) {
            // Auto-select first subject if none selected
            val firstSubject = state.subjects.first()
            onEvent(SessionEvent.OnRelatedSubjectChange(firstSubject))
            timerService.subjectId.value = firstSubject.subjectId
        }
    }

    LaunchedEffect(currentTimerState) {
        if (currentTimerState == TimerState.IDLE && state.selectedTopicId != null) {
            val duration = timerService.duration.toLong()
            if (duration > 0) {
                ServiceHelper.triggerForegroundService(
                    context = context,
                    action = ACTION_SERVICE_CANCEL
                )
                onEvent(SessionEvent.CompleteTask(state.selectedTopicId))
            }
        }
    }

    SubjectListBottomSheet(
        sheetState = sheetState,
        isOpen = isBottomSheetOpen,
        subjects = state.subjects,
        onDismissRequest = { isBottomSheetOpen = false },
        onSubjectClicked = { subject ->
            scope.launch {
                sheetState.hide()
            }.invokeOnCompletion {
                if (!sheetState.isVisible) {
                    isBottomSheetOpen = false
                    onEvent(SessionEvent.OnRelatedSubjectChange(subject))
                    timerService.subjectId.value = subject.subjectId
                }
            }
        }
    )
    DeleteDialog(
        isOpen = isDeleteDialogOpen,
        title = "Delete Session?",
        bodyText = "Are you sure you want to delete this session? This action can not be undone.",
        onDismissRequest = { isDeleteDialogOpen = false },
        onConfirmButtonClick = {
            onEvent(SessionEvent.DeleteSession)
            isDeleteDialogOpen = false
        }
    )
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            SessionScreenTopBar(onBackButtonClick = onBackButtonClick)
        },
        floatingActionButton = {
            if (isAlarmPlaying) {
                PulsingStopButton(onClick = { StudySessionTimerService.stopAlarmStatic() })
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!hasMeditatedToday) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable { navigator.navigate(MeditationScreenDestination) }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "You Haven't Meditated Today",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            
                            Text(
                                text = "Take 15 minutes to meditate before studying. Research shows meditation improves focus & concentration, reduces stress & anxiety and enhances memory retention.",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tap to meditate",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowCircleRight,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(start = 4.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            item {
                TimerSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    hours = hours,
                    minutes = minutes,
                    seconds = seconds,
                    totalSeconds = timerService.let {
                        val h = hours.toIntOrNull() ?: 0
                        val m = minutes.toIntOrNull() ?: 0
                        val s = seconds.toIntOrNull() ?: 0
                        (h * 3600) + (m * 60) + s
                    },
                    selectedDurationMinutes = state.selectedDuration
                )
            }
            
            item {
                RelatedToSubjectSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    relatedToSubject = state.relatedToSubject ?: "",
                    selectSubjectButtonClick = { isBottomSheetOpen = true },
                    seconds = seconds,
                    subjects = state.subjects,
                    onSubjectSelected = { subject ->
                        onEvent(SessionEvent.OnRelatedSubjectChange(subject))
                        timerService.subjectId.value = subject.subjectId
                    },
                    onEvent = onEvent
                )
            }
            
            item {
                TopicSelector(
                    topics = state.topics,
                    selectedTopicId = state.selectedTopicId,
                    onTopicSelected = { task -> 
                        onEvent(SessionEvent.OnTopicSelect(task))
                        timerService.setDuration(task.taskDuration)
                    },
                    selectedSubjectId = state.subjectId,
                    timerService = timerService
                )
            }
            
            item {
                ButtonSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    cancelButtonClick = {
                        ServiceHelper.triggerForegroundService(
                            context = context,
                            action = ACTION_SERVICE_CANCEL
                        )
                    },
                    finishButtonClick = {
                        ServiceHelper.triggerForegroundService(
                            context = context,
                            action = ACTION_SERVICE_CANCEL
                        )
                        onEvent(SessionEvent.SaveSession)
                    },
                    timerState = currentTimerState,
                    seconds = seconds,
                    context = context,
                    durationMinutes = state.selectedDuration,
                    hasMeditatedToday = hasMeditatedToday,
                    selectedTopicId = state.selectedTopicId,
                    subjectId = state.subjectId,
                    onEvent = onEvent,
                    navigator = navigator,
                    onStartClick = {
                        if (!checkDndPermission()) {
                            showPermissionDialog = true
                            return@ButtonSection
                        }
                        ServiceHelper.triggerForegroundService(
                            context = context,
                            action = ACTION_SERVICE_START,
                            duration = state.selectedDuration,
                            topicId = state.selectedTopicId,
                            subjectId = state.subjectId
                        )
                    }
                )
            }
            studySessionsList(
                sectionTitle = "Study Sessions",
                emptyListText = "No study sessions recorded yet",
                sessions = state.sessions,
                onDeleteIconClick = { session ->
                    onEvent(SessionEvent.OnDeleteSessionButtonClick(session))
                    isDeleteDialogOpen = true
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionScreenTopBar(
    onBackButtonClick: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBackButtonClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Navigate Back"
                )
            }
        },
        title = {
            Text(
                text = "Study Session",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    )
}

@Composable
private fun TimerSection(
    modifier: Modifier,
    hours: String,
    minutes: String,
    seconds: String,
    totalSeconds: Int,
    selectedDurationMinutes: Int
) {
    LaunchedEffect(totalSeconds) {
        println("Timer Update - Total: $totalSeconds, Selected: $selectedDurationMinutes")
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        TimerCircle(
            hours = hours,
            minutes = minutes,
            seconds = seconds,
            totalSeconds = totalSeconds,
            selectedDurationMinutes = selectedDurationMinutes
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format("%s:%s:%s", hours, minutes, seconds),
                style = MaterialTheme.typography.displayLarge
            )
        }
    }
}

@Composable
private fun TimerCircle(
    hours: String,
    minutes: String,
    seconds: String,
    totalSeconds: Int,
    selectedDurationMinutes: Int
) {
    val colorScheme = MaterialTheme.colorScheme
    
    val safeProgress = remember { mutableStateOf(0f) }
    safeProgress.value = if (selectedDurationMinutes == 0) {
        0f
    } else {
        (totalSeconds.toFloat() / (selectedDurationMinutes * 60)).coerceIn(0f, 1f)
    }

    val progress by animateFloatAsState(
        targetValue = safeProgress.value,
        animationSpec = tween(500),
        label = "timer_progress"
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = minOf(size.width, size.height) / 2f
        
        drawCircle(
            color = colorScheme.surfaceVariant.copy(alpha = 0.3f),
            radius = radius,
            style = Stroke(width = 2.dp.toPx())
        )
        
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
        
        val dotAngle = (-90f + (360f * progress)) * (PI / 180f)
        val dotRadius = radius * 0.9f
        val dotCenter = Offset(
            x = center.x + (dotRadius * cos(dotAngle)).toFloat(),
            y = center.y + (dotRadius * sin(dotAngle)).toFloat()
        )
        drawCircle(
            color = colorScheme.primary,
            radius = 6.dp.toPx(),
            center = dotCenter
        )
    }
}

@Composable
private fun RelatedToSubjectSection(
    modifier: Modifier,
    relatedToSubject: String,
    selectSubjectButtonClick: () -> Unit,
    seconds: String,
    subjects: List<Subject>,
    onSubjectSelected: (Subject) -> Unit,
    onEvent: (SessionEvent) -> Unit
) {
    var showSubjectPicker by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Select Subject:",
            style = MaterialTheme.typography.titleMedium
        )
        
        Button(
            onClick = { showSubjectPicker = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Text(
                text = relatedToSubject.ifEmpty { "Select Subject" },
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    if (showSubjectPicker) {
        AlertDialog(
            onDismissRequest = { showSubjectPicker = false },
            title = { Text("Select Subject") },
            text = {
                if (subjects.isEmpty()) {
                    Box(
            modifier = Modifier
                .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No subjects available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn {
                        items(subjects) { subject ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                                    .clickable {
                                        onSubjectSelected(subject)
                                        // Reset topic selection when subject changes
                                        onEvent(SessionEvent.OnTopicSelect(null))
                                        showSubjectPicker = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                                    text = subject.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "${subject.goalHours}h/day",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubjectPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ButtonSection(
    modifier: Modifier,
    cancelButtonClick: () -> Unit,
    finishButtonClick: () -> Unit,
    timerState: TimerState,
    seconds: String,
    context: Context,
    durationMinutes: Int,
    hasMeditatedToday: Boolean,
    selectedTopicId: Int?,
    subjectId: Int?,
    onEvent: (SessionEvent) -> Unit,
    navigator: DestinationsNavigator,
    onStartClick: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                if (timerState == TimerState.STARTED) {
                    ServiceHelper.triggerForegroundService(
                        context = context,
                        action = ACTION_SERVICE_STOP
                    )
                } else {
                    onStartClick()
                }
            },
            enabled = selectedTopicId != null && durationMinutes > 0 && subjectId != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = when (timerState) {
                    TimerState.STARTED -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.primary
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(
                imageVector = when (timerState) {
                    TimerState.STARTED -> Icons.Default.Pause
                    else -> Icons.Default.PlayArrow
                },
                contentDescription = when (timerState) {
                    TimerState.STARTED -> "Pause Timer"
                    else -> "Start Timer"
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when (timerState) {
                    TimerState.STARTED -> "Pause"
                    TimerState.STOPPED -> "Resume"
                    else -> "Start"
                },
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (timerState != TimerState.IDLE) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        cancelButtonClick()
                        onEvent(SessionEvent.CancelSession)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Red
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Timer"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Button(
                    onClick = {
                        finishButtonClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "Finish Timer"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Finish",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

private fun timerTextAnimation(duration: Int = 600): ContentTransform {
    return slideInVertically(animationSpec = tween(duration)) { fullHeight -> fullHeight } +
            fadeIn(animationSpec = tween(duration)) togetherWith
            slideOutVertically(animationSpec = tween(duration)) { fullHeight -> fullHeight } +
            fadeOut(animationSpec = tween(duration))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicSelector(
    topics: List<Task>,
    selectedTopicId: Int?,
    onTopicSelected: (Task) -> Unit,
    selectedSubjectId: Int?,
    timerService: StudySessionTimerService
) {
    var showTopicPicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Select Topic:",
            style = MaterialTheme.typography.titleMedium
        )
        
        Button(
            onClick = { showTopicPicker = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            enabled = selectedSubjectId != null
        ) {
            Text(
                text = topics.find { it.taskId == selectedTopicId }?.title ?: 
                    if (selectedSubjectId == null) "Select subject first" else "Select Topic",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    if (showTopicPicker) {
        AlertDialog(
            onDismissRequest = { showTopicPicker = false },
            title = { Text("Select Topic") },
            text = {
                val filteredTopics = topics.filter { task -> 
                    !task.isComplete && task.taskSubjectId == selectedSubjectId 
                }
                if (filteredTopics.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No topics available for this subject",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn {
                        items(filteredTopics) { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onTopicSelected(task)
                                        timerService.setDuration(task.taskDuration)
                                        showTopicPicker = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                                Column {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = if (task.taskDuration > 0) {
                                            "${task.taskDuration / 60}h ${task.taskDuration % 60}m"
                                        } else {
                                            "No duration set"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Priority.fromInt(task.priority).color.copy(alpha = 0.1f),
                                    contentColor = Priority.fromInt(task.priority).color
                                ) {
                    Text(
                                        text = Priority.fromInt(task.priority).name,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                }
            }
        },
        confirmButton = {
                TextButton(onClick = { showTopicPicker = false }) {
                Text("Cancel")
            }
        }
    )
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

@Composable
private fun BulletPoint(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    shape = CircleShape
                )
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
