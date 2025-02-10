package com.gorai.myedenfocus.presentation.subject

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gorai.myedenfocus.domain.model.Subject
import com.gorai.myedenfocus.presentation.components.DeleteDialog
import com.gorai.myedenfocus.presentation.components.EditSubjectDialog
import com.gorai.myedenfocus.presentation.components.Speedometer
import com.gorai.myedenfocus.presentation.components.TasksList
import com.gorai.myedenfocus.presentation.components.studySessionsList
import com.gorai.myedenfocus.presentation.destinations.SessionScreenRouteDestination
import com.gorai.myedenfocus.presentation.destinations.TaskScreenRouteDestination
import com.gorai.myedenfocus.presentation.session.TimerState
import com.gorai.myedenfocus.presentation.task.TaskScreenNavArgs
import com.gorai.myedenfocus.util.LocalTimerService
import com.gorai.myedenfocus.util.NavAnimation
import com.gorai.myedenfocus.util.SnackbarEvent
import com.gorai.myedenfocus.util.formatTimeHoursMinutes
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

data class SubjectScreenNavArgs(
    val subjectId: Int
)

@Destination(
    navArgsDelegate = SubjectScreenNavArgs::class,
    style = NavAnimation::class
)
@Composable
fun SubjectScreenRoute(
    navigator: DestinationsNavigator
) {
    val viewModel: SubjectViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val timerService = LocalTimerService.current
    val elapsedTime by timerService.elapsedTimeFlow.collectAsStateWithLifecycle()
    val currentTimerState by timerService.currentTimerState
    val sessionCompleted by timerService.sessionCompleted.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    LaunchedEffect(elapsedTime, currentTimerState) {
        if (currentTimerState == TimerState.STARTED && timerService.subjectId.value == state.currentSubjectId) {
            viewModel.updateElapsedTime(elapsedTime)
        }
    }

    // Handle session completion
    LaunchedEffect(sessionCompleted) {
        if (sessionCompleted) {
            viewModel.refreshData()
            timerService.resetSessionCompleted()
        }
    }

    LaunchedEffect(timerService) {
        timerService?.let { service ->
            viewModel.collectTimerUpdates(service)
        }
    }

    SubjectScreen(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarEvent = viewModel.snackbarEventFlow,
        onBackButtonClick = { navigator.navigateUp() },
        onAddTaskButtonClick = {
            val navArg = TaskScreenNavArgs(taskId = null, subjectId = state.currentSubjectId)
            navigator.navigate(TaskScreenRouteDestination(navArgs = navArg)) },
        onTaskCardClick = { taskId ->
            val navArg = TaskScreenNavArgs(taskId = taskId, subjectId = null)
            navigator.navigate(TaskScreenRouteDestination(navArgs = navArg))
        },
        navigator = navigator
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectScreen(
    state: SubjectState,
    onEvent: (SubjectEvent) -> Unit,
    snackbarEvent: SharedFlow<SnackbarEvent>,
    onBackButtonClick: () -> Unit,
    onAddTaskButtonClick: () -> Unit,
    onTaskCardClick: (Int?) -> Unit,
    navigator: DestinationsNavigator
) {
    val listState = rememberLazyListState()
    val isFABExpanded by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var isDeleteSubjectDialogOpen by rememberSaveable { mutableStateOf(false) }
    var isDeleteSessionDialogOpen by rememberSaveable { mutableStateOf(false) }
    var isEditSubjectDialogOpen by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember {
        SnackbarHostState()
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
            SnackbarEvent.NavigateUp -> {
                    onBackButtonClick()
                }
            }
        }
    }

    LaunchedEffect(key1 = state.studiedHours, key2 = state.goalStudyHours) {
        onEvent(SubjectEvent.UpdateProgress)
    }

    EditSubjectDialog(
        isOpen = isEditSubjectDialogOpen,
        selectedColors = state.subjectCardColors,
        onColorChange = { onEvent(SubjectEvent.OnSubjectCardColorChange(it)) },
        subjectName = state.subjectName,
        dailyGoalHours = state.goalStudyHours,
        onSubjectNameChange = { onEvent(SubjectEvent.OnSubjectNameChange(it)) },
        onDailyGoalHoursChange = { onEvent(SubjectEvent.OnGoalStudyHoursChange(it)) },
        onDismissRequest = { isEditSubjectDialogOpen = false },
        onConfirmButtonClick = {
            onEvent(SubjectEvent.UpdateSubject)
            isEditSubjectDialogOpen = false 
        },
        daysPerWeek = state.subjectDaysPerWeek,
        onDaysPerWeekChange = { onEvent(SubjectEvent.OnSubjectDaysPerWeekChange(it)) }
    )
    DeleteDialog(
        isOpen = isDeleteSubjectDialogOpen,
        title = "Delete Subject?",
        bodyText = "Are you sure you want to delete this subject?",
        onDismissRequest = { isDeleteSubjectDialogOpen = false },
        onConfirmButtonClick = {
            isDeleteSubjectDialogOpen = false
            onEvent(SubjectEvent.DeleteSubject)
        }
    )
    DeleteDialog(
        isOpen = isDeleteSessionDialogOpen,
        title = "Delete Session?",
        bodyText = "Are you sure you want to delete this session?",
        onDismissRequest = { isDeleteSessionDialogOpen = false },
        onConfirmButtonClick = {
            onEvent(SubjectEvent.DeleteSession)
            isDeleteSessionDialogOpen = false
        }
    )

    Log.d("SubjectScreen", "Recent sessions size: ${state.recentSessions.size}")

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState)},
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SubjectScreenTopBar(
                title = state.subjectName,
                onBackButtonClick = onBackButtonClick,
                onDeleteButtonClick = { isDeleteSubjectDialogOpen = true },
                onEditButtonClick = { isEditSubjectDialogOpen = true },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTaskButtonClick,
                text = { Text(text = "Add Topic") },
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = "Add Topic") },
                expanded = isFABExpanded
            )
        }
    ) { paddingValue ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValue)
        ) {
            item {
                CountsCard(
                    studiedHours = state.studiedHours,
                    goalStudyHours = state.goalStudyHours,
                    progress = state.progress
                )
            }
            item {
                SubjectOverviewSection(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    studiedHours = state.studiedHours,
                    goalHours = state.goalStudyHours,
                    progress = state.progress,
                    daysPerWeek = state.subjectDaysPerWeek
                )
            }
            item {
                TasksList(
                    sectionTitle = "Topics to Complete",
                    emptyListText = "No upcoming topics\nPress + button to add new topics",
                    tasks = state.upcomingTasks,
                    onTaskCardClick = onTaskCardClick,
                    onStartSession = { task ->
                        navigator.navigate(
                            SessionScreenRouteDestination(
                                preSelectedTopicId = task.taskId
                            )
                        )
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
            item {
                TasksList(
                    sectionTitle = "Completed Topics",
                    emptyListText = "No completed topics\nClick on the start button to complete topics",
                    tasks = state.completedTasks.sortedByDescending { it.completedAt ?: 0L },
                    onTaskCardClick = onTaskCardClick,
                    onStartSession = null
                )
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
            studySessionsList(
                sectionTitle = "Study Sessions (${state.recentSessions.size})",
                emptyListText = "No study sessions\nStart a study session to begin recording your progress",
                sessions = state.recentSessions,
                onDeleteIconClick = {
                    isDeleteSessionDialogOpen = true
                    onEvent(SubjectEvent.OnDeleteSessionButtonClick(it))
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectScreenTopBar(
    title: String,
    onBackButtonClick: () -> Unit,
    onDeleteButtonClick: () -> Unit,
    onEditButtonClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    LargeTopAppBar(
        navigationIcon = {
            IconButton(onClick = onBackButtonClick) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        actions = {
            IconButton(onClick = onDeleteButtonClick) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Subject")
            }
            IconButton(onClick = onEditButtonClick) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Subject")
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun SubjectOverviewSection(
    modifier: Modifier,
    studiedHours: String,
    goalHours: String,
    progress: Float,
    daysPerWeek: Int = 5
) {
    val studiedHoursFloat = studiedHours.toFloatOrNull() ?: 0f
    val goalHoursFloat = goalHours.toFloatOrNull() ?: 0f
    val weeklyGoalHours = goalHoursFloat * daysPerWeek
    val weeklyStudiedHours = studiedHoursFloat

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Speedometer(
            modifier = Modifier.weight(1f),
            headingText = "Daily Goal",
            value = goalHoursFloat,
            maxValue = 15f,
            displayText = formatTimeHoursMinutes(goalHoursFloat)
        )

        Speedometer(
            modifier = Modifier.weight(1f),
            headingText = "Today's Progress",
            value = studiedHoursFloat,
            maxValue = goalHoursFloat,
            displayText = formatTimeHoursMinutes(studiedHoursFloat)
        )

        Speedometer(
            modifier = Modifier.weight(1f),
            headingText = "Progress This Week",
            value = weeklyStudiedHours,
            maxValue = weeklyGoalHours,
            displayText = formatTimeHoursMinutes(weeklyStudiedHours)
        )
    }
}

@Composable
private fun CountCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    maxValue: String
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "/ $maxValue",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun CountsCard(
    studiedHours: String,
    goalStudyHours: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val studiedHoursFloat = studiedHours.toFloatOrNull() ?: 0f
    val goalHoursFloat = goalStudyHours.toFloatOrNull() ?: 1f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Time Studied",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formatTimeHoursMinutes(studiedHoursFloat),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Goal: ${formatTimeHoursMinutes(goalHoursFloat)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Speedometer(
                modifier = Modifier,
                headingText = "Progress",
                value = studiedHoursFloat,
                maxValue = goalHoursFloat,
                displayText = formatTimeHoursMinutes(studiedHoursFloat)
            )
        }
    }
}

private fun calculateRemainingHours(
    dailyGoal: Float,
    currentSubject: Int?,
    allSubjects: List<Subject>,
    currentGoalHours: Float
): Float {
    val totalOtherSubjectsHours = allSubjects
        .filter { it.subjectId != currentSubject }
        .sumOf { it.goalHours.toDouble() }
        .toFloat()
    
    return (dailyGoal - totalOtherSubjectsHours + currentGoalHours).coerceAtLeast(0f)
}

private fun formatHours(hours: Float): String {
    return if (hours % 1 == 0f) {
        "${hours.toInt()}h"
    } else {
        String.format("%.1fh", hours)
    }
}
