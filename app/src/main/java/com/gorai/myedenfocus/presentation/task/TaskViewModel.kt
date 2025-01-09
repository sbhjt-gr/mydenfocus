package com.gorai.myedenfocus.presentation.task

import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorai.myedenfocus.domain.model.Task
import com.gorai.myedenfocus.domain.repository.SubjectRepository
import com.gorai.myedenfocus.domain.repository.TaskRepository
import com.gorai.myedenfocus.presentation.navArgs
import com.gorai.myedenfocus.util.Priority
import com.gorai.myedenfocus.util.SnackbarEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val subjectRepository: SubjectRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val navArgs: TaskScreenNavArgs = savedStateHandle.navArgs()
    private val _state = MutableStateFlow(TaskState())
    val state = combine(
        _state,
        subjectRepository.getAllSubjects()
    ) { state, subjects ->
        state.copy(subjects = subjects)
    }.stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = TaskState()
    )

    private val _snackbarEventFlow = MutableSharedFlow<SnackbarEvent>()
    val snackbarEventFlow = _snackbarEventFlow.asSharedFlow()

    init {
        fetchTask()
        fetchSubject()
    }

    fun onEvent(event: TaskEvent) {
        when (event) {
            is TaskEvent.OnTitleChange -> {
                _state.update {
                    it.copy(title = event.title)
                }
            }
            is TaskEvent.OnDescriptionChange -> {
                _state.update {
                    it.copy(description = event.description)
                }
            }
            is TaskEvent.OnDateChange -> {
                _state.update {
                    it.copy(dueDate = event.millis)
                }
            }
            is TaskEvent.OnPriorityChange -> {
                _state.update {
                    it.copy(priority = event.priority)
                }
            }
            is TaskEvent.OnIsCompleteChange -> {
                viewModelScope.launch {
                    try {
                        // Get current state
                        val currentState = _state.value
                        
                        // Toggle completion state
                        _state.update {
                            it.copy(isTaskComplete = !it.isTaskComplete)
                        }
                        
                        // Only update in database if we have a valid task ID
                        currentState.currentTaskId?.let { taskId ->
                            // Get existing task
                            taskRepository.getTaskById(taskId)?.let { existingTask ->
                                // Create updated task with toggled completion
                                val updatedTask = existingTask.copy(
                                    isComplete = !existingTask.isComplete
                                )
                                // Save to database
                                taskRepository.upsertTask(updatedTask)
                                
                                _snackbarEventFlow.emit(
                                    SnackbarEvent.ShowSnackbar(
                                        message = if (updatedTask.isComplete) "Task marked as complete" else "Task marked as incomplete",
                                        duration = SnackbarDuration.Short
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        _snackbarEventFlow.emit(
                            SnackbarEvent.ShowSnackbar(
                                message = "Couldn't update task status: ${e.message}",
                                duration = SnackbarDuration.Long
                            )
                        )
                    }
                }
            }
            is TaskEvent.OnRelatedSubjectSelect -> {
                _state.update {
                    it.copy(
                        relatedToSubject = event.subject.name,
                        subjectId = event.subject.subjectId
                    )
                }
            }
            TaskEvent.SaveTask -> saveTask()
            TaskEvent.DeleteTask -> TODO()
        }
    }
    private fun saveTask() {
        viewModelScope.launch {
            val state = _state.value
            if (state.subjectId == null || state.relatedToSubject == null) {
                _snackbarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        "Please select a subject related to the task",
                        SnackbarDuration.Long
                    )
                )
                return@launch
            }
            try {
                taskRepository.upsertTask(
                    task = Task(
                        title = state.title,
                        description = state.description,
                        dueDate = state.dueDate ?: Instant.now().toEpochMilli(),
                        relatedToSubject = state.relatedToSubject,
                        priority = state.priority.value,
                        isComplete = state.isTaskComplete,
                        taskSubjectId = state.subjectId,
                        taskId = state.currentTaskId
                    )
                )
                _snackbarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        "Task Saved Successfully",
                        SnackbarDuration.Long
                    )
                )
                _snackbarEventFlow.emit(SnackbarEvent.NavigateUp)
            } catch (e: Exception) {
                _snackbarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        "Couldn't save task. ${e.message}",
                        SnackbarDuration.Long
                    )
                )
            }
        }
    }
    private fun fetchTask() {
        viewModelScope.launch {
            navArgs.taskId?.let { id ->
                taskRepository.getTaskById(id)?.let { task ->
                    _state.update { it: TaskState ->
                        it.copy(
                            title = task.title,
                            description = task.description,
                            dueDate = task.dueDate,
                            isTaskComplete = task.isComplete,
                            relatedToSubject = task.relatedToSubject,
                            priority = Priority.fromInt(task.priority),
                            subjectId = task.taskSubjectId,
                            currentTaskId = task.taskId
                        )
                    }
                }
            }
        }
    }
    private fun fetchSubject() {
        viewModelScope.launch {
            navArgs.subjectId?.let { id ->
                subjectRepository.getSubjectById(id)?.let { subject ->
                    _state.update {
                        it.copy(
                            subjectId = subject.subjectId,
                            relatedToSubject = subject.name
                        )
                    }
                }
            }
        }
    }
}