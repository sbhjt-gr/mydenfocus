package com.gorai.myedenfocus.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gorai.myedenfocus.domain.model.Session
import com.gorai.myedenfocus.domain.model.Subject
import com.gorai.myedenfocus.domain.model.Task
import com.gorai.myedenfocus.presentation.theme.MyedenFocusTheme
import com.ramcosta.composedestinations.DestinationsNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyedenFocusTheme {
                DestinationsNavHost(navGraph = NavGraphs.root)
            }
        }
    }
}


val subjects = listOf(
    Subject(
        name = "Biology",
        goalHours = 10f,
        colors = Subject.subjectCardColors[0],
        subjectId = 1
    ),
    Subject(
        name = "Chemistry",
        goalHours = 10f,
        colors = Subject.subjectCardColors[1],
        subjectId = 2
    ),
    Subject(
        name = "Physics",
        goalHours = 10f,
        colors = Subject.subjectCardColors[2],
        subjectId = 3
    ),
    Subject(
        name = "Maths",
        goalHours = 10f,
        colors = Subject.subjectCardColors[3],
        subjectId = 4
    ),
    Subject(
        name = "English",
        goalHours = 10f,
        colors = Subject.subjectCardColors[4],
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
