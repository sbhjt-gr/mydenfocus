package com.gorai.myedenfocus.presentation.task
import com.gorai.myedenfocus.domain.model.Subject
import com.gorai.myedenfocus.util.Priority

data class TaskState(
    val title: String = "",
    val description: String = "",
    val dueDate: Long? = null,
    val isTaskComplete: Boolean = false,
    val priority: Priority = Priority.LOW,
    val relatedToSubject: String? = null,
    val subjects: List<Subject> = emptyList(),
    val subjectId: Int? = null,
    val currentTaskId: Int? = null,
    val taskDuration: Int = 0
)