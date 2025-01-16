package com.gorai.myedenfocus.presentation.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gorai.myedenfocus.domain.model.Session
import com.gorai.myedenfocus.domain.model.Subject
import com.gorai.myedenfocus.domain.model.Task
import com.gorai.myedenfocus.presentation.components.AddSubjectDialog
import com.gorai.myedenfocus.presentation.components.BottomBar
import com.gorai.myedenfocus.presentation.components.DeleteDialog
import com.gorai.myedenfocus.presentation.components.Speedometer
import com.gorai.myedenfocus.presentation.components.SubjectCard
import com.gorai.myedenfocus.presentation.components.studySessionsList
import com.gorai.myedenfocus.presentation.components.tasksList
import com.gorai.myedenfocus.presentation.destinations.SessionScreenRouteDestination
import com.gorai.myedenfocus.presentation.destinations.SubjectScreenRouteDestination
import com.gorai.myedenfocus.presentation.destinations.TaskScreenRouteDestination
import com.gorai.myedenfocus.presentation.subject.SubjectScreenNavArgs
import com.gorai.myedenfocus.presentation.task.TaskScreenNavArgs
import com.gorai.myedenfocus.util.NavAnimation
import com.gorai.myedenfocus.util.SnackbarEvent
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import com.gorai.myedenfocus.presentation.destinations.MeditationScreenDestination
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import com.gorai.myedenfocus.presentation.destinations.SettingsScreenDestination
import com.gorai.myedenfocus.presentation.components.CommonTopBar

@RootNavGraph(start = true)
@Destination(
    start = true,
    style = NavAnimation::class
)
@Composable
fun DashBoardScreenRoute(
    navigator: DestinationsNavigator,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val recentSessions by viewModel.recentSessions.collectAsStateWithLifecycle()

    Scaffold(
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
                subjectId?.let {
                    navigator.navigate(SubjectScreenRouteDestination(subjectId = it))
                }
            },
            onTaskCardClick = { taskId ->
                taskId?.let {
                    navigator.navigate(TaskScreenRouteDestination(taskId = it, subjectId = null))
                }
            },
            onStartSessionButtonClick = {
                navigator.navigate(SessionScreenRouteDestination())
            },
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
}

@Composable
private fun DashboardScreen(
    state: DashboardState,
    tasks: List<Task>,
    recentSessions: List<Session>,
    onEvent: (DashboardEvent) -> Unit,
    snackbarEvent: SharedFlow<SnackbarEvent>,
    onSubjectCardClick: (Int?) -> Unit,
    onTaskCardClick: (Int?) -> Unit,
    onStartSessionButtonClick: () -> Unit,
    currentRoute: String = "schedule",
    onNavigate: (String) -> Unit = {}
) {
    var isAddSubjectDialogOpen by rememberSaveable {
        mutableStateOf(false)
    }
    var isDeleteSubjectDialogOpen by rememberSaveable {
        mutableStateOf(false)
    }
    val snackbarHostState = remember {
        SnackbarHostState()
    }
    var showDailyGoalDialog by remember { mutableStateOf(false) }
    var showAddSubjectDialog by remember { mutableStateOf(false) }

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

    if (showDailyGoalDialog) {
        DailyGoalDialog(
            currentGoal = state.dailyStudyGoal,
            onGoalChange = { onEvent(DashboardEvent.OnDailyStudyGoalChange(it)) },
            onDismiss = { showDailyGoalDialog = false },
            onConfirm = {
                onEvent(DashboardEvent.SaveDailyStudyGoal)
                showDailyGoalDialog = false
                showAddSubjectDialog = true
            }
        )
    }

    if (showAddSubjectDialog) {
        if (state.dailyStudyGoal.isEmpty()) {
            showDailyGoalDialog = true
            showAddSubjectDialog = false
        } else {
            AddSubjectDialog(
                isOpen = true,
                selectedColors = state.subjectCardColors,
                onColorChange = { onEvent(DashboardEvent.OnSubjectCardColorChange(it)) },
                subjectName = state.subjectName,
                dailyGoalHours = state.goalStudyHours,
                remainingHours = state.dailyStudyGoal.toFloatOrNull()?.minus(
                    state.subjects.sumOf { it.goalHours.toDouble() }.toFloat()
                ) ?: 0f,
                onSubjectNameChange = { onEvent(DashboardEvent.OnSubjectNameChange(it)) },
                onDailyGoalHoursChange = { onEvent(DashboardEvent.OnGoalStudyHoursChange(it)) },
                onDismissRequest = { showAddSubjectDialog = false },
                onConfirmButtonClick = {
                    onEvent(DashboardEvent.SaveSubject)
                    showAddSubjectDialog = false
                }
            )
        }
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
                currentRoute = currentRoute,
                onNavigate = onNavigate
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CountCardsSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    subjectCount = state.totalSubjectCount,
                    studiedHours = state.totalStudiedHours,
                    dailyStudiedHours = state.dailyStudiedHours,
                    dailyGoalHours = state.dailyStudyGoal
                )
            }

            item {
                Button(
                    onClick = onStartSessionButtonClick,
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
                SubjectCardSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    subjectList = state.subjects,
                    onAddIconClicked = { showAddSubjectDialog = true },
                    onSubjectCardClick = onSubjectCardClick
                )
            }
            tasksList(
                sectionTitle = "Task List",
                emptyListText = "No tasks for are listed\nPress + button to add new tasks",
                tasks = tasks,
                onTaskCardClick = onTaskCardClick,
                onCheckBoxClick = { onEvent(DashboardEvent.OnTaskIsCompleteChange(it)) }
            )
            studySessionsList(
                sectionTitle = "Recent Study Sessions",
                emptyListText = "No study sessions\nStart a study session to begin recording your progress",
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
                        painter = painterResource(id = com.gorai.myedenfocus.R.drawable.app_icon),
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
private fun CountCardsSection(
    modifier: Modifier,
    subjectCount: Int,
    studiedHours: String,
    dailyStudiedHours: String,
    dailyGoalHours: String
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Speedometer(
            modifier = Modifier.weight(1f),
            headingText = "Total Subjects",
            value = subjectCount.toFloat(),
            maxValue = 10f,
            displayText = subjectCount.toString()
        )

        Speedometer(
            modifier = Modifier.weight(1f),
            headingText = "Today's Hours",
            value = dailyStudiedHours.toFloatOrNull() ?: 0f,
            maxValue = dailyGoalHours.toFloatOrNull() ?: 1f,
            displayText = "${dailyStudiedHours} h"
        )

        Speedometer(
            modifier = Modifier.weight(1f),
            headingText = "Hours Studied",
            value = studiedHours.toFloatOrNull() ?: 0f,
            maxValue = 100f,
            displayText = "${studiedHours} h"
        )
    }
}

@Composable
private fun SubjectCardSection(
    modifier: Modifier,
    subjectList: List<Subject>,
    emptyListText: String = "No subjects yet.\n Press + button to add new subjects\n",
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
                    text = "No Subjects",
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
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Daily Study Goal") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hours Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hours:",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    
                    IconButton(
                        onClick = {
                            val current = currentGoal.toFloatOrNull() ?: 0f
                            if (current >= 1) {
                                onGoalChange((current - 1f).toString())
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease Hours"
                        )
                    }
                    
                    Text(
                        text = "${currentGoal.toFloatOrNull()?.toInt() ?: 0}",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    IconButton(
                        onClick = {
                            val current = currentGoal.toFloatOrNull() ?: 0f
                            onGoalChange((current + 1f).toString())
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase Hours"
                        )
                    }
                }

                // Minutes Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Minutes:",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    
                    IconButton(
                        onClick = {
                            val current = currentGoal.toFloatOrNull() ?: 0f
                            if (current >= 0.25f) {
                                onGoalChange((current - 0.25f).toString())
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease Minutes"
                        )
                    }
                    
                    Text(
                        text = "${((currentGoal.toFloatOrNull() ?: 0f) % 1 * 60).toInt()}",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    IconButton(
                        onClick = {
                            val current = currentGoal.toFloatOrNull() ?: 0f
                            onGoalChange((current + 0.25f).toString())
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase Minutes"
                        )
                    }
                }

                Text(
                    text = "Total: ${currentGoal.toFloatOrNull() ?: 0}h",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
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