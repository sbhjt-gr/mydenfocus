package com.gorai.myedenfocus.presentation.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDatePicker(
     state: DatePickerState,
     isOpen: Boolean,
     confirmButtonText: String = "OK",
     dismissButtonText: String = "Cancel",
     onDismissRequest: () -> Unit,
     onConfirmButtonClick: () -> Unit,
     onDismissButtonClick: () -> Unit
) {
    if (isOpen) {
        DatePickerDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(onClick = onConfirmButtonClick) {
                    Text(text = confirmButtonText)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissButtonClick) {
                    Text(text = dismissButtonText)
                }
            },
            content = {
                DatePicker(
                    state = state,
                    dateValidator = {
                        timestamp ->
                        val selectedDate = Instant
                            .ofEpochMilli(timestamp)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        val currentDate = LocalDate.now(ZoneId.systemDefault())
                        selectedDate >= currentDate
                    }
                )
            }
        )
    }
}