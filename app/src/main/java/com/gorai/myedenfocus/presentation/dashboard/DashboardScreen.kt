package com.gorai.myedenfocus.presentation.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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

@RootNavGraph(start = true)
@Destination(
    start = true,
    style = NavAnimation::class
)
@Composable
fun DashBoardScreenRoute(
    navigator: DestinationsNavigator
) {
    val viewModel: DashboardViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val recentSessions by viewModel.recentSessions.collectAsStateWithLifecycle()

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
                "meditate" -> navigator.navigate(MeditationScreenDestination)
                else -> Unit
            }
        }
    )
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

    AddSubjectDialog(
        isOpen = isAddSubjectDialogOpen,
        onDismissRequest = { isAddSubjectDialogOpen = false },
        onConfirmButtonClick = {
            isAddSubjectDialogOpen = false
            onEvent(DashboardEvent.SaveSubject)
        },
        subjectName = state.subjectName,
        goalHours = state.goalStudyHours,
        onSubjectNameChange = { onEvent(DashboardEvent.OnSubjectNameChange(it)) },
        onGoalHoursChange = { onEvent(DashboardEvent.OnGoalStudyHoursChange(it)) },
        selectedColors = state.subjectCardColors,
        onColorChange = { onEvent(DashboardEvent.OnSubjectCardColorChange(it)) }
    )

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
            DashboardScreenTopBar()
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onStartSessionButtonClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Session"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start Study Session",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
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
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CountCardsSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .padding(bottom = 0.dp),
                    subjectCount = state.totalSubjectCount,
                    studiedHours = state.totalStudiedHours,
                    goalHours = state.totalGoalStudyHours
                )
            }
            item {
                SubjectCardSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp),
                    subjectList = state.subjects,
                    onAddIconClicked = {
                        isAddSubjectDialogOpen = true
                    },
                    onSubjectCardClick = onSubjectCardClick
                )
            }
            tasksList(
                sectionTitle = "Today's Tasks",
                emptyListText = "No tasks for today\nPress + button to add new tasks",
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
private fun CountCardsSection(
    modifier: Modifier,
    subjectCount: Int,
    studiedHours: String,
    goalHours: String
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Subject Count Speedometer
        Speedometer(
            modifier = Modifier.weight(1f),
            headingText = "Subjects",
            value = subjectCount.toFloat(),
            maxValue = 10f, // Assuming max 10 subjects
            displayText = subjectCount.toString()
        )

        // Studied Hours Speedometer
        Speedometer(
            modifier = Modifier.weight(1f),
            headingText = "Studied",
            value = studiedHours.toFloatOrNull() ?: 0f,
            maxValue = goalHours.toFloatOrNull() ?: 100f,
            displayText = "${studiedHours}h"
        )

        // Goal Hours Speedometer
        Speedometer(
            modifier = Modifier.weight(1f),
            headingText = "Goal",
            value = goalHours.toFloatOrNull() ?: 0f,
            maxValue = 100f,
            displayText = "${goalHours}h"
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