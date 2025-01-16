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
    var selectedHour by remember { mutableStateOf(reminderTime.split(":")[0].toInt()) }
    var selectedMinute by remember { mutableStateOf(reminderTime.split(":")[1].toInt()) }
    
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
                    state = rememberTimePickerState(
                        initialHour = selectedHour,
                        initialMinute = selectedMinute
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val time = LocalTime.of(selectedHour, selectedMinute)
                    onTimeSelect(time.format(DateTimeFormatter.ofPattern("HH:mm")))
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