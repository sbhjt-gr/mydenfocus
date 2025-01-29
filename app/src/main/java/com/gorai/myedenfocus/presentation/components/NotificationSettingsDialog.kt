package com.gorai.myedenfocus.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsDialog(
    reminderTime: String,
    onDismiss: () -> Unit,
    onTimeSelect: (String) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = reminderTime.split(":")[0].toInt(),
        initialMinute = reminderTime.split(":")[1].toInt()
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Reminder Time") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Choose when you'd like to receive daily study reminders",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                TimePicker(
                    state = timePickerState,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val time = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                    onTimeSelect(time)
                }
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