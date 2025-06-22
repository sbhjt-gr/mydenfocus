package com.gorai.myedenfocus.presentation.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add

import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.gorai.myedenfocus.domain.model.Session
import com.gorai.myedenfocus.domain.model.Subject
import com.gorai.myedenfocus.domain.model.Task
import com.gorai.myedenfocus.presentation.components.AddSubjectDialog
import com.gorai.myedenfocus.presentation.components.BottomBar
import com.gorai.myedenfocus.presentation.components.CommonTopBar
import com.gorai.myedenfocus.presentation.components.DeleteDialog
import com.gorai.myedenfocus.presentation.components.SubjectCard
import com.gorai.myedenfocus.presentation.components.TasksList
import com.gorai.myedenfocus.presentation.components.studySessionsList
import com.gorai.myedenfocus.presentation.destinations.MeditationScreenDestination
import com.gorai.myedenfocus.presentation.destinations.SessionScreenRouteDestination
import com.gorai.myedenfocus.presentation.destinations.SettingsScreenDestination
import com.gorai.myedenfocus.presentation.destinations.SubjectScreenRouteDestination

import com.gorai.myedenfocus.presentation.destinations.TaskScreenRouteDestination
import com.gorai.myedenfocus.presentation.subject.SubjectScreenNavArgs
import com.gorai.myedenfocus.presentation.task.TaskScreenNavArgs
import com.gorai.myedenfocus.util.LocalTimerService
import com.gorai.myedenfocus.util.NavAnimation
import com.gorai.myedenfocus.util.SnackbarEvent
import com.gorai.myedenfocus.util.formatHours
import com.gorai.myedenfocus.util.formatHoursFromString
import com.gorai.myedenfocus.util.formatTimeHoursMinutes
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

@RootNavGraph(start = true)
@Destination(
    start = true,
    style = NavAnimation::class,
    route = "schedule"
)
@Composable
fun DashBoardScreenRoute(
    navigator: DestinationsNavigator
) {
    val viewModel: DashboardViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val recentSessions by viewModel.recentSessions.collectAsStateWithLifecycle()
    val timerService = LocalTimerService.current
    val elapsedTime by timerService.elapsedTimeFlow.collectAsStateWithLifecycle()
    val currentTimerState by timerService.currentTimerState
    val sessionCompleted by timerService.sessionCompleted.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val systemUiController = rememberSystemUiController()
    val isDarkTheme = isSystemInDarkTheme()

    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color.Transparent,
            darkIcons = !isDarkTheme
        )
    }

    LaunchedEffect(key1 = true) {
        viewModel.snackbarEventFlow.collectLatest { event ->
            when(event) {
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

    LaunchedEffect(timerService) {
        timerService?.let { service ->
            viewModel.collectTimerUpdates(service)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CommonTopBar(
                onSettingsClick = {
                    navigator.navigate(SettingsScreenDestination())
                }
            )
        }
    ) { padding ->
        DashboardScreen(
            state = state,
            tasks = tasks,
            recentSessions = recentSessions,
            onEvent = viewModel::onEvent,
            snackbarEvent = viewModel.snackbarEventFlow,
            onSubjectCardClick = { subjectId ->
                navigator.navigate(SubjectScreenRouteDestination(SubjectScreenNavArgs(subjectId)))
            },
            onTaskCardClick = { taskId ->
                navigator.navigate(TaskScreenRouteDestination(TaskScreenNavArgs(taskId = taskId, subjectId = null)))
            },
            navigator = navigator,
            snackbarHostState = snackbarHostState
        )
    }
}

@Composable
private fun DashboardScreen(
    state: DashboardState,
    tasks: List<Task>,
    recentSessions: List<Session>,
    onEvent: (DashboardEvent) -> Unit,
    snackbarEvent: SharedFlow<SnackbarEvent>,
    onSubjectCardClick: (Int) -> Unit,
    onTaskCardClick: (Int?) -> Unit,
    navigator: DestinationsNavigator,
    snackbarHostState: SnackbarHostState
) {
    var showDailyGoalDialog by remember { mutableStateOf(false) }
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var isDeleteSubjectDialogOpen by rememberSaveable { mutableStateOf(false) }

    // Show daily goal dialog
    if (showDailyGoalDialog) {
        DailyGoalDialog(
            currentGoal = state.dailyStudyGoal,
            onGoalChange = { onEvent(DashboardEvent.OnDailyStudyGoalChange(it)) },
            onDismiss = { showDailyGoalDialog = false },
            onConfirm = {
                onEvent(DashboardEvent.SaveDailyStudyGoal)
                showDailyGoalDialog = false
            },
            state = state,
            onEvent = onEvent
        )
    }

    if (showAddSubjectDialog) {
        AddSubjectDialog(
            isOpen = true,
            selectedColors = state.subjectCardColors,
            onColorChange = { onEvent(DashboardEvent.OnSubjectCardColorChange(it)) },
            subjectName = state.subjectName,
            dailyGoalHours = state.goalStudyHours,
            onSubjectNameChange = { onEvent(DashboardEvent.OnSubjectNameChange(it)) },
            onDailyGoalHoursChange = { onEvent(DashboardEvent.OnGoalStudyHoursChange(it)) },
            onDismissRequest = { showAddSubjectDialog = false },
            onConfirmButtonClick = {
                onEvent(DashboardEvent.SaveSubject)
                showAddSubjectDialog = false
            },
            daysPerWeek = state.subjectDaysPerWeek,
            onDaysPerWeekChange = { onEvent(DashboardEvent.OnSubjectDaysPerWeekChange(it)) }
        )
    }

    DeleteDialog(
        isOpen = isDeleteSubjectDialogOpen,
        title = "Delete Session?",
        bodyText = "Are you sure you want to delete this session? This action can not be undone.",
        onDismissRequest = { isDeleteSubjectDialogOpen = false },
        onConfirmButtonClick = {
            isDeleteSubjectDialogOpen = false
            onEvent(DashboardEvent.DeleteSession)
        }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            DashboardScreenTopBar(
                onDailyGoalClick = { showDailyGoalDialog = true }
            )
        },
        bottomBar = {
            BottomBar(
                currentRoute = "schedule",
                onNavigate = { route ->
                    when (route) {
                        "meditate" -> navigator.navigate(MeditationScreenDestination) {
                            popUpTo(route = "schedule") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }

                        else -> Unit
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Study Goal",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (state.dailyStudyGoal.isEmpty()) "Not set" 
                                  else state.dailyStudyGoal.formatHoursFromString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { showDailyGoalDialog = true },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(text = if (state.dailyStudyGoal.isEmpty()) "Set Goal" else "Edit Goal")
                    }
                }
            }

            item {
                CountCardsSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    subjectCount = state.totalSubjectCount,
                    studiedHours = state.totalStudiedHours,
                    dailyStudiedHours = state.dailyStudiedHours,
                    dailyGoalHours = state.dailyStudyGoal,
                    studyDaysPerWeek = state.studyDaysPerWeek
                )
            }

            item {
                Button(
                    onClick = {
                        navigator.navigate(
                            SessionScreenRouteDestination(
                                preSelectedTopicId = null,
                                preSelectedSubjectId = null
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start Session",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Study Session",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Subjects",
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = { showAddSubjectDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Subject"
                        )
                    }
                }
            }

            item {
                SubjectCardSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    subjectList = state.subjects,
                    onAddIconClicked = { showAddSubjectDialog = true },
                    onSubjectCardClick = onSubjectCardClick
                )
            }
            item {
                TasksList(
                    sectionTitle = "Incomplete Topics",
                    emptyListText = "No topics are listed",
                    tasks = tasks,
                    onTaskCardClick = onTaskCardClick,
                    onStartSession = { task ->
                        navigator.navigate(
                            SessionScreenRouteDestination(
                                preSelectedTopicId = task.taskId,
                                preSelectedSubjectId = task.taskSubjectId
                            )
                        )
                    }
                )
            }
            studySessionsList(
                sectionTitle = "Recent Study Sessions",
                emptyListText = "No study sessions yet",
                sessions = recentSessions,
                onDeleteIconClick = { session ->
                    isDeleteSubjectDialogOpen = true
                    onEvent(DashboardEvent.OnDeleteSessionButtonClick(session))
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreenTopBar(
    onDailyGoalClick: () -> Unit
) {
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
                    text = "MydenFocus",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CountCardsSection(
    modifier: Modifier,
    subjectCount: Int,
    studiedHours: String,
    dailyStudiedHours: String,
    dailyGoalHours: String,
    studyDaysPerWeek: Int = 5
) {
    val weeklyGoalHours = (dailyGoalHours.toFloatOrNull() ?: 0f) * studyDaysPerWeek
    val studiedHoursFloat = studiedHours.toFloatOrNull() ?: 0f
    val dailyStudiedHoursFloat = dailyStudiedHours.toFloatOrNull() ?: 0f
    val dailyGoalHoursFloat = dailyGoalHours.toFloatOrNull() ?: 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CountCard(
                modifier = Modifier.weight(1f),
                title = "Total Subjects",
                value = subjectCount.toString(),
                maxValue = ""
            )
            Spacer(modifier = Modifier.width(16.dp))
            CountCard(
                modifier = Modifier.weight(1f),
                title = "Today's Progress",
                value = formatTimeHoursMinutes(dailyStudiedHoursFloat),
                maxValue = formatTimeHoursMinutes(dailyGoalHoursFloat)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CountCard(
                modifier = Modifier.weight(1f),
                title = "Weekly Progress",
                value = formatTimeHoursMinutes(studiedHoursFloat),
                maxValue = formatTimeHoursMinutes(weeklyGoalHours)
            )
            Spacer(modifier = Modifier.width(16.dp))
            CountCard(
                modifier = Modifier.weight(1f),
                title = "Study Days",
                value = studyDaysPerWeek.toString(),
                maxValue = "7"
            )
        }
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
                if (maxValue.isNotEmpty()) {
                    Text(
                        text = "/ $maxValue",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectCardSection(
    modifier: Modifier,
    subjectList: List<Subject>,
    emptyListText: String = "No subjects have been added",
    onAddIconClicked: () -> Unit,
    onSubjectCardClick: (Int) -> Unit
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Subjects",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 12.dp)
            )
            IconButton(onClick = onAddIconClicked) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Subject",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (subjectList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(com.gorai.myedenfocus.R.drawable.img_books),
                    contentDescription = emptyListText,
                    modifier = Modifier.size(120.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No subjects have been added",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                subjectList.forEach { subject ->
                    SubjectCard(
                        subjectName = subject.name,
                        gradientColors = subject.colors.map { Color(it) },
                        onClick = { subject.subjectId?.let { onSubjectCardClick(it) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyGoalDialog(
    currentGoal: String,
    onGoalChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    state: DashboardState,
    onEvent: (DashboardEvent) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Daily Study Goal") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Hours selector
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Hours per day:",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val current = currentGoal.toFloatOrNull() ?: 0f
                                if (current >= 1) {
                                    onGoalChange((current - 1f).toString())
                                }
                            },
                            enabled = (currentGoal.toFloatOrNull() ?: 0f) > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease Hours",
                                tint = if ((currentGoal.toFloatOrNull() ?: 0f) > 0)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline
                            )
                        }
                        
                        Text(
                            text = "${(currentGoal.toFloatOrNull() ?: 0f).formatHours()}",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        IconButton(
                            onClick = {
                                val current = currentGoal.toFloatOrNull() ?: 0f
                                if (current < 16) {
                                    onGoalChange((current + 1f).toString())
                                }
                            },
                            enabled = (currentGoal.toFloatOrNull() ?: 0f) < 16
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase Hours",
                                tint = if ((currentGoal.toFloatOrNull() ?: 0f) < 16)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                // Days per week selector
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Days per week:",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val current = state.studyDaysPerWeek
                                if (current > 1) {
                                    onEvent(DashboardEvent.OnStudyDaysPerWeekChange(current - 1))
                                }
                            },
                            enabled = state.studyDaysPerWeek > 1
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease Days",
                                tint = if (state.studyDaysPerWeek > 1)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline
                            )
                        }
                        
                        Text(
                            text = "${state.studyDaysPerWeek}",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        IconButton(
                            onClick = {
                                val current = state.studyDaysPerWeek
                                if (current < 7) {
                                    onEvent(DashboardEvent.OnStudyDaysPerWeekChange(current + 1))
                                }
                            },
                            enabled = state.studyDaysPerWeek < 7
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase Days",
                                tint = if (state.studyDaysPerWeek < 7)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onGoalChange(currentGoal)
                    onConfirm()
                },
                enabled = currentGoal.toFloatOrNull() ?: 0f > 0
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatHours(hours: Float): String {
    return if (hours % 1 == 0f) {
        "${hours.toInt()}h"
    } else {
        String.format("%.1fh", hours)
    }
}