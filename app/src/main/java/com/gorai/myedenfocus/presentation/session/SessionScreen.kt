package com.gorai.myedenfocus.presentation.session

import android.content.Context
import android.content.Intent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gorai.myedenfocus.domain.model.Subject
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
import com.gorai.myedenfocus.util.ServiceHelper
import com.gorai.myedenfocus.util.SnackbarEvent
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.time.DurationUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

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
    timerService: StudySessionTimerService
) {
    val viewModel: SessionViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
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

    if (showMeditationDialog) {
        MeditationReminderDialog(
            onDismissRequest = { 
                showMeditationDialog = false 
            },
            onMeditateClick = {
                showMeditationDialog = false
                navigator.navigate(MeditationScreenDestination)
            },
            onSkipClick = {
                showMeditationDialog = false
                // Start the timer after skipping meditation
                if (state.subjectId != null) {
                    ServiceHelper.triggerForegroundService(
                        context = context,
                        action = ACTION_SERVICE_START,
                        duration = state.selectedDuration
                    )
                    timerService.subjectId.value = state.subjectId
                } else {
                    onEvent(SessionEvent.CheckSubjectId)
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
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DurationSelector(
                    selectedMinutes = state.selectedDuration,
                    onDurationSelected = { minutes ->
                        onEvent(SessionEvent.OnDurationSelected(minutes))
                    }
                )
            }
            item {
                TimerSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    hours = timerService?.hours?.value ?: "00",
                    minutes = timerService?.minutes?.value ?: "00",
                    seconds = timerService?.seconds?.value ?: "00",
                    totalSeconds = timerService?.let {
                        val h = it.hours.value.toIntOrNull() ?: 0
                        val m = it.minutes.value.toIntOrNull() ?: 0
                        val s = it.seconds.value.toIntOrNull() ?: 0
                        (h * 3600) + (m * 60) + s
                    } ?: 0,
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
                    }
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
                        val duration = timerService.duration.toLong(DurationUnit.SECONDS)
                        if (duration >= 36) {
                            ServiceHelper.triggerForegroundService(
                                context = context,
                                action = ACTION_SERVICE_CANCEL
                            )
                            onEvent(SessionEvent.SaveSession(duration))
                        }
                    },
                    timerState = currentTimerState,
                    seconds = seconds,
                    context = context,
                    onShowMeditationDialog = { showMeditationDialog = true },
                    durationMinutes = state.selectedDuration,
                    hasMeditatedToday = hasMeditatedToday
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
    onSubjectSelected: (Subject) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Related to Subject",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = selectSubjectButtonClick),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = relatedToSubject.ifEmpty { "Select Subject" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (relatedToSubject.isEmpty()) 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select Subject"
                )
            }
        }
    }
}

@Composable
private fun ButtonSection(
    modifier: Modifier = Modifier,
    cancelButtonClick: () -> Unit,
    finishButtonClick: () -> Unit,
    timerState: TimerState,
    seconds: String,
    context: Context,
    onShowMeditationDialog: () -> Unit,
    durationMinutes: Int,
    hasMeditatedToday: Boolean
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        // Cancel Button
        Button(
            onClick = cancelButtonClick,
            enabled = seconds != "00" && timerState != TimerState.STARTED
        ) {
            Text(
                text = "Cancel",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }

        // Start/Stop Button
        Button(
            onClick = { 
                if (timerState == TimerState.STARTED) {
                    // If timer is running, stop it
                    ServiceHelper.triggerForegroundService(
                        context = context,
                        action = ACTION_SERVICE_STOP
                    )
                } else {
                    // If timer is not running, check meditation and start
                    if (!hasMeditatedToday) {
                        onShowMeditationDialog()
                    } else {
                        // Make sure duration is passed here
                        if (durationMinutes > 0) {
                            ServiceHelper.triggerForegroundService(
                                context = context,
                                action = ACTION_SERVICE_START,
                                duration = durationMinutes
                            )
                            // Add debug logging
                            println("Starting timer with duration: $durationMinutes minutes")
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (timerState == TimerState.STARTED) Red
                else MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                text = when (timerState) {
                    TimerState.STARTED -> "Stop"
                    TimerState.STOPPED -> "Resume"
                    else -> "Start"
                }
            )
        }

        // Finish Button
        Button(
            onClick = finishButtonClick,
            enabled = seconds != "00" && timerState != TimerState.STARTED
        ) {
            Text(
                text = "Finish",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
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
private fun DurationSelector(
    selectedMinutes: Int,
    onDurationSelected: (Int) -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Duration:",
            style = MaterialTheme.typography.titleMedium
        )
        
        Button(
            onClick = { showTimePicker = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Text(
                text = formatDuration(selectedMinutes),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = { hours, minutes ->
                val totalMinutes = (hours * 60) + minutes
                if (totalMinutes in 1..(16 * 60)) {
                    onDurationSelected(totalMinutes)
                }
                showTimePicker = false
            },
            initialHours = selectedMinutes / 60,
            initialMinutes = selectedMinutes % 60
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (hours: Int, minutes: Int) -> Unit,
    initialHours: Int = 0,
    initialMinutes: Int = 0
) {
    var hours by remember { mutableStateOf(initialHours) }
    var minutes by remember { mutableStateOf(initialMinutes) }
    val totalMinutes = (hours * 60) + minutes
    val maxMinutes = 16 * 60  // 16 hours in minutes

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Duration") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours Selector
                    NumberPicker(
                        value = hours,
                        onValueChange = { newHours ->
                            // Only update if total minutes would be <= maxMinutes
                            if ((newHours * 60 + minutes) <= maxMinutes) {
                                hours = newHours
                            }
                        },
                        range = 0..16,  // Allow up to 16 hours
                        label = "Hours"
                    )

                    Text(":", style = MaterialTheme.typography.headlineMedium)

                    // Minutes Selector
                    NumberPicker(
                        value = minutes,
                        onValueChange = { newMinutes ->
                            // Only update if total minutes would be <= maxMinutes
                            if ((hours * 60 + newMinutes) <= maxMinutes) {
                                minutes = newMinutes
                            }
                        },
                        range = 0..59,
                        label = "Minutes"
                    )
                }

                if (totalMinutes == 0) {
                    Text(
                        text = "Please select a duration (max 16 hours)",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(hours, minutes) },
                enabled = totalMinutes in 1..maxMinutes
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = { 
                if (value < range.last) onValueChange(value + 1)
            },
            enabled = value < range.last
        ) {
            Icon(Icons.Default.KeyboardArrowUp, "Increase")
        }

        Text(
            text = value.toString().padStart(2, '0'),
            style = MaterialTheme.typography.headlineMedium
        )

        IconButton(
            onClick = { 
                if (value > range.first) onValueChange(value - 1)
            },
            enabled = value > range.first
        ) {
            Icon(Icons.Default.KeyboardArrowDown, "Decrease")
        }

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 -> "${hours}h ${mins}m"
        else -> "${mins}m"
    }
}
