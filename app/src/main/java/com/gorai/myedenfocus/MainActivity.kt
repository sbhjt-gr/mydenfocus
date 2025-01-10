package com.gorai.myedenfocus

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ActivityCompat
import com.gorai.myedenfocus.domain.model.Session
import com.gorai.myedenfocus.domain.model.Subject
import com.gorai.myedenfocus.domain.model.Task
import com.gorai.myedenfocus.presentation.NavGraphs
import com.gorai.myedenfocus.presentation.destinations.SessionScreenRouteDestination
import com.gorai.myedenfocus.presentation.session.StudySessionTimerService
import com.gorai.myedenfocus.presentation.theme.MyedenFocusTheme
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.navigation.dependency
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var isBound by mutableStateOf(false)
    private lateinit var timerService: StudySessionTimerService
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, service: IBinder?) {
            val binder = service as StudySessionTimerService.StudySessionTimerBinder
            timerService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isBound = false
        }
    }
    override fun onStart() {
        super.onStart()
        Intent(this, StudySessionTimerService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            if (isBound) {
                MyedenFocusTheme {
                    DestinationsNavHost(
                        navGraph = NavGraphs.root,
                        dependenciesContainerBuilder = {
                            dependency(SessionScreenRouteDestination) { timerService }
                        }
                    )
                }
            }
        }
        requestPermission()
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }
    }
    override fun onStop() {
        super.onStop()
        unbindService(connection)
        isBound = false
    }
}


val subjects = listOf(
    Subject(
        name = "Biology",
        goalHours = 10f,
        colors = Subject.subjectCardColors[0].map { it.toArgb() },
        subjectId = 1
    ),
    Subject(
        name = "Chemistry",
        goalHours = 10f,
        colors = Subject.subjectCardColors[1].map { it.toArgb() },
        subjectId = 2
    ),
    Subject(
        name = "Physics",
        goalHours = 10f,
        colors = Subject.subjectCardColors[2].map { it.toArgb() },
        subjectId = 3
    ),
    Subject(
        name = "Maths",
        goalHours = 10f,
        colors = Subject.subjectCardColors[3].map { it.toArgb() },
        subjectId = 4
    ),
    Subject(
        name = "English",
        goalHours = 10f,
        colors = Subject.subjectCardColors[4].map { it.toArgb() },
        subjectId = 5
    )
)

val tasks = listOf(
    Task(
        title = "Prepare Notes",
        description = "Prepare notes for class",
        dueDate = 0L,
        priority = 0,
        relatedToSubject = "Biology",
        isComplete = false,
        taskId = 1,
        taskSubjectId = 1
    ),
    Task(
        title = "Do Homework",
        description = "Prepare notes for class",
        dueDate = 0L,
        priority = 1,
        relatedToSubject = "",
        isComplete = true,
        taskId = 2,
        taskSubjectId = 1
    ),
    Task(
        title = "Go Coaching",
        description = "Prepare notes for class",
        dueDate = 0L,
        priority = 2,
        relatedToSubject = "",
        isComplete = false,
        taskId = 3,
        taskSubjectId = 1
    ),
    Task(
        title = "Assignment",
        description = "Prepare notes for class",
        dueDate = 0L,
        priority = 3,
        relatedToSubject = "",
        isComplete = false,
        taskId = 4,
        taskSubjectId = 1
    ),
    Task(
        title = "Write Poem",
        description = "Prepare notes for class",
        dueDate = 0L,
        priority = 4,
        relatedToSubject = "",
        isComplete = false,
        taskId = 5,
        taskSubjectId = 1
    )
)

val sessions = listOf(
    Session(
        relatedToSubject = "DBMS",
        date = 0L,
        duration = 2,
        sessionSubjectId = 0,
        sessionId = 0
    ),
    Session(
        relatedToSubject = "OOPS",
        date = 0L,
        duration = 2,
        sessionSubjectId = 1,
        sessionId = 0
    ),
    Session(
        relatedToSubject = "OS",
        date = 0L,
        duration = 2,
        sessionSubjectId = 2,
        sessionId = 0
    ),
    Session(
        relatedToSubject = "SE",
        date = 0L,
        duration = 2,
        sessionSubjectId = 3,
        sessionId = 0
    )
)
