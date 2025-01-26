package com.gorai.myedenfocus.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gorai.myedenfocus.presentation.components.ColorPickerDialog

@Composable
fun EditSubjectDialog(
    isOpen: Boolean,
    selectedColors: List<Color>,
    onColorChange: (List<Color>) -> Unit,
    subjectName: String,
    dailyGoalHours: String,
    onSubjectNameChange: (String) -> Unit,
    onDailyGoalHoursChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirmButtonClick: () -> Unit,
    daysPerWeek: Int = 5,
    onDaysPerWeekChange: (Int) -> Unit
) {
    if (!isOpen) return

    var subjectNameError by rememberSaveable { mutableStateOf<String?>(null) }
    var goalHoursError by rememberSaveable { mutableStateOf<String?>(null) }
    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    
    val currentHours = dailyGoalHours.toFloatOrNull()?.toInt() ?: 0
    val currentMinutes = ((dailyGoalHours.toFloatOrNull() ?: 0f) % 1 * 60).toInt()

    subjectNameError = when {
        subjectName.isBlank() -> "Subject name is required"
        subjectName.length < 2 -> "Name must be at least 2 characters"
        subjectName.length > 50 -> "Name must be less than 50 characters"
        else -> null
    }

    goalHoursError = when {
        dailyGoalHours.isBlank() -> "Study time is required"
        dailyGoalHours.toFloat() < 0.25f -> "Minimum goal is 15 minutes"
        else -> null
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header
                Text(
                    text = "Edit Subject",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Subject Name
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = onSubjectNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Subject Name") },
                    singleLine = true,
                    isError = subjectNameError != null,
                    supportingText = subjectNameError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Color Selection
                Text(
                    text = "Subject Color",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = selectedColors.ifEmpty { listOf(Color.Gray, Color.Gray) }
                            )
                        )
                        .clickable { showColorPicker = true }
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Study Time
                Text(
                    text = "Daily Study Goal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Hours
                    OutlinedTextField(
                        value = if (currentHours == 0) "" else currentHours.toString(),
                        onValueChange = { newValue ->
                            if (newValue.length <= 2) {
                                val newHours = newValue.toIntOrNull() ?: 0
                                if (newHours in 0..15) {
                                    onDailyGoalHoursChange("${newHours + (currentMinutes / 60f)}")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Hours") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center)
                    )
                    
                    // Minutes
                    OutlinedTextField(
                        value = if (currentMinutes == 0) "" else currentMinutes.toString(),
                        onValueChange = { newValue ->
                            if (newValue.length <= 2) {
                                val newMinutes = newValue.toIntOrNull() ?: 0
                                if (newMinutes in 0..59) {
                                    onDailyGoalHoursChange("${currentHours + (newMinutes / 60f)}")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Minutes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center)
                    )
                }
                if (goalHoursError != null) {
                    Text(
                        text = goalHoursError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                // Days per Week
                Text(
                    text = "Study Days per Week",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = { if (daysPerWeek > 1) onDaysPerWeekChange(daysPerWeek - 1) },
                            enabled = daysPerWeek > 1
                        ) {
                            Icon(Icons.Default.Remove, "Decrease days")
                        }
                        Text(
                            text = "$daysPerWeek days",
                            style = MaterialTheme.typography.titleLarge
                        )
                        FilledIconButton(
                            onClick = { if (daysPerWeek < 7) onDaysPerWeekChange(daysPerWeek + 1) },
                            enabled = daysPerWeek < 7
                        ) {
                            Icon(Icons.Default.Add, "Increase days")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirmButtonClick,
                        enabled = subjectNameError == null && goalHoursError == null
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            onDismissRequest = { showColorPicker = false },
            onColorSelected = { startColor, endColor ->
                onColorChange(listOf(startColor, endColor))
                showColorPicker = false
            },
            initialStartColor = selectedColors.firstOrNull() ?: Color.Gray,
            initialEndColor = selectedColors.lastOrNull() ?: Color.Gray
        )
    }
} 