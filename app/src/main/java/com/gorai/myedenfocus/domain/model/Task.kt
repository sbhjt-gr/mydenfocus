package com.gorai.myedenfocus.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Task(
    @PrimaryKey(autoGenerate = true)
    val taskId: Int? = null,
    val title: String,
    val description: String,
    val dueDate: Long,
    val priority: Int,
    val relatedToSubject: String,
    val isComplete: Boolean = false,
    val taskSubjectId: Int,
    val taskDuration: Int = 0,
    val completedAt: Long? = null
)
