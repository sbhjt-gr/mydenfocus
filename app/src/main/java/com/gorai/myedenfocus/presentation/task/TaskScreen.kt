package com.gorai.myedenfocus.presentation.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gorai.myedenfocus.presentation.components.DeleteDialog
import com.gorai.myedenfocus.presentation.components.SubjectListBottomSheet
import com.gorai.myedenfocus.presentation.components.TaskCheckBox
import com.gorai.myedenfocus.presentation.components.TaskDatePicker
import com.gorai.myedenfocus.presentation.subjects
import com.gorai.myedenfocus.presentation.theme.Red
import com.gorai.myedenfocus.util.Priority
import com.gorai.myedenfocus.util.changeMillsToDateString
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import java.time.Instant

data class TaskScreenNavArgs(
    val taskId: Int?,
    val subjectId: Int?
)

@Destination(navArgsDelegate = TaskScreenNavArgs::class)
@Composable
fun TaskScreenRoute(
    navigator: DestinationsNavigator
) {
    TaskScreen(
        onBackButtonClick = { navigator.navigateUp() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskScreen(
    onBackButtonClick: () -> Unit
) {
    var isDeleteDialogOpen by rememberSaveable { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }

    var isDatePickerDialogOpen by rememberSaveable { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()

    var isBottomSheetOpen by remember {
        mutableStateOf(false)
    }

    var datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Instant.now().toEpochMilli()
    )

    val scope = rememberCoroutineScope()

    var description by remember { mutableStateOf("") }

    var taskTitleError by rememberSaveable { mutableStateOf<String?>(null) }

    taskTitleError = when {
        title.isBlank() -> "Please enter task title."
        title.length < 4 -> "Task title is too short."
        title.length > 100 -> "Task is too long."
        else -> null
    }

    DeleteDialog(
        isOpen = isDeleteDialogOpen,
        title = "Delete Task?",
        bodyText = "Are you sure you want to delete this task? This action can not be undone.",
        onDismissRequest = { isDeleteDialogOpen = false },
        onConfirmButtonClick = { isDeleteDialogOpen = false }
    )
    TaskDatePicker(
        state = datePickerState,
        isOpen = isDatePickerDialogOpen,
        onDismissRequest = { isDatePickerDialogOpen = false },
        onConfirmButtonClick = { isDatePickerDialogOpen = false },
        onDismissButtonClick = { isDatePickerDialogOpen = false }
    )
    SubjectListBottomSheet(
        sheetState = sheetState,
        isOpen = isBottomSheetOpen,
        subjects = subjects,
        onDismissRequest = {},
        onSubjectClicked = {
            scope.launch {
                sheetState.hide()
            }.invokeOnCompletion {
                if (!sheetState.isVisible) isBottomSheetOpen = false
            }
        }
    )
    Scaffold(
        topBar = {
            TaskScreenTopBar(
                isTaskExist = true,
                isComplete = true,
                checkBoxBorderColor = Red,
                onCheckBoxClick = { /*TODO*/ },
                onDeleteButtonClick = { isDeleteDialogOpen = true },
                onBackButtonClick = { /*TODO*/ }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = title,
                onValueChange = { title = it },
                label = { Text(text = "Title") },
                singleLine = true,
                isError = taskTitleError != null && title.isNotBlank(),
                supportingText = { Text(text = taskTitleError.orEmpty()) }
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = description,
                onValueChange = { description = it },
                label = { Text(text = "Description") }
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Due Date",
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = datePickerState.selectedDateMillis.changeMillsToDateString(),
                    style = MaterialTheme.typography.bodyLarge
                )
                IconButton(onClick = { isDatePickerDialogOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select Due Date"
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Priority",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Priority.entries.forEach { priority ->
                    PriorityButton(
                        modifier = Modifier.weight(1f),
                        label = priority.title,
                        backgroundColor = priority.color,
                        borderColor = if (priority == Priority.MEDIUM) {
                            Color.White
                        } else Color.Transparent,
                        labelColor = if (priority == Priority.MEDIUM) {
                            Color.White
                        } else Color.White.copy(alpha = 0.7f),
                        onClick = { /*TODO*/ }
                    )
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
            Text(
                text = "Related to Subject",
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "English",
                    style = MaterialTheme.typography.bodyLarge
                )
                IconButton(onClick = { isBottomSheetOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Subject"
                    )
                }
            }
            Button(
                onClick = { isBottomSheetOpen = true },
                enabled = taskTitleError == null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
            ) {
                Text(text = "Save")
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
    private fun TaskScreenTopBar(
        isTaskExist: Boolean,
        isComplete: Boolean,
        checkBoxBorderColor: Color,
        onCheckBoxClick: () -> Unit,
        onDeleteButtonClick: () -> Unit,
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
                    text = "Task",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            actions = {
                if (isTaskExist) {
                    TaskCheckBox(
                        isComplete = isComplete,
                        borderColor = checkBoxBorderColor,
                        onCheckBoxClick = onCheckBoxClick
                    )
                    IconButton(onClick = onDeleteButtonClick) {
                        Icon(
                            imageVector = Icons.Default.Delete, contentDescription = "Delete Task"
                        )
                    }
                }
            }
        )
    }
@Composable
private fun PriorityButton(
        modifier: Modifier = Modifier,
        label: String,
        backgroundColor: Color,
        borderColor: Color,
        onClick: () -> Unit,
        labelColor: Color,
        ) {
    Box(
        modifier = modifier
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(5.dp)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = labelColor)
    }
}
