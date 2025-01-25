package com.gorai.myedenfocus.presentation.components

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gorai.myedenfocus.domain.model.Subject
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubjectDialog(
    isOpen: Boolean,
    title: String = "Add/Update Subject",
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
    var subjectNameError by rememberSaveable { mutableStateOf<String?>(null) }
    var goalHoursError by rememberSaveable { mutableStateOf<String?>(null) }
    var showStartColorPicker by rememberSaveable { mutableStateOf(false) }
    var showEndColorPicker by rememberSaveable { mutableStateOf(false) }
    
    var hoursExpanded by rememberSaveable { mutableStateOf(false) }
    var minutesExpanded by rememberSaveable { mutableStateOf(false) }
    
    val currentHours = dailyGoalHours.toFloatOrNull()?.toInt() ?: 0
    val currentMinutes = ((dailyGoalHours.toFloatOrNull() ?: 0f) % 1 * 60).toInt()

    subjectNameError = when {
        subjectName.isBlank() -> "Subject Name Cannot Be Empty."
        subjectName.length < 2 -> "Subject Name Cannot Be Less Than 2 Characters."
        subjectName.length > 50 -> "Subject Name Cannot Be More Than 50 Characters."
        else -> null
    }

    goalHoursError = when {
        dailyGoalHours.isBlank() -> "Please select study time"
        dailyGoalHours.toFloat() < 0.25f -> "Minimum goal is 15 minutes"
        else -> null
    }

    if (isOpen) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = title) },
            text = {
                Column {
                    // Color Picker Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = "Select Gradient Colors",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Start Color Picker
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Start Color",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(selectedColors.firstOrNull() ?: Color.Gray)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline,
                                            CircleShape
                                        )
                                        .clickable { showStartColorPicker = true }
                                )
                            }
                            
                            // End Color Picker
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "End Color",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(selectedColors.lastOrNull() ?: Color.Gray)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline,
                                            CircleShape
                                        )
                                        .clickable { showEndColorPicker = true }
                                )
                            }
                        }
                        
                        // Preview of selected gradient
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = selectedColors.ifEmpty { listOf(Color.Gray, Color.Gray) }
                                    )
                                )
                        )
                    }
                    
                    // Subject Name TextField
                    OutlinedTextField(
                        value = subjectName,
                        onValueChange = { onSubjectNameChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Subject Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        isError = subjectNameError != null && subjectName.isNotBlank(),
                        supportingText = { Text(text = subjectNameError.orEmpty()) }
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Daily Goal Hours TextField
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Hours TextField
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Hours",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OutlinedTextField(
                                value = if (currentHours == 0) "" else currentHours.toString(),
                                onValueChange = { newValue ->
                                    if (newValue.length <= 2) {
                                        val newHours = newValue.toIntOrNull() ?: 0
                                        if (newHours in 0..15) {
                                            val newValue = newHours + (currentMinutes / 60f)
                                            onDailyGoalHoursChange(newValue.toString())
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    textAlign = TextAlign.Center
                                ),
                                placeholder = { 
                                    Text(
                                        text = "0",
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            )
                        }

                        // Minutes TextField
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Minutes",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            OutlinedTextField(
                                value = if (currentMinutes == 0) "" else currentMinutes.toString(),
                                onValueChange = { newValue ->
                                    if (newValue.length <= 2) {
                                        val newMinutes = newValue.toIntOrNull() ?: 0
                                        if (newMinutes in 0..59) {
                                            val newValue = currentHours + (newMinutes / 60f)
                                            onDailyGoalHoursChange(newValue.toString())
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    textAlign = TextAlign.Center
                                ),
                                placeholder = { 
                                    Text(
                                        text = "0",
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            )
                        }
                    }

                    if (goalHoursError != null) {
                        Text(
                            text = goalHoursError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Days per Week selector
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Days per week:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (daysPerWeek > 1) {
                                        onDaysPerWeekChange(daysPerWeek - 1)
                                    }
                                },
                                modifier = Modifier.size(32.dp),
                                enabled = daysPerWeek > 1
                            ) {
                                Icon(
                                    Icons.Default.Remove,
                                    "Decrease days",
                                    tint = if (daysPerWeek > 1)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outline
                                )
                            }

                    Text(
                                text = "$daysPerWeek",
                                modifier = Modifier.width(40.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium
                            )

                            IconButton(
                                onClick = {
                                    if (daysPerWeek < 7) {
                                        onDaysPerWeekChange(daysPerWeek + 1)
                                    }
                                },
                                modifier = Modifier.size(32.dp),
                                enabled = daysPerWeek < 7
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    "Increase days",
                                    tint = if (daysPerWeek < 7)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = "Cancel")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmButtonClick,
                    enabled = subjectNameError == null && goalHoursError == null
                ) {
                    Text(text = "Save")
                }
            }
        )
    }

    // Color Picker Dialog for Start Color
    if (showStartColorPicker) {
        ColorPickerDialog(
            onDismissRequest = { showStartColorPicker = false },
            onColorSelected = { color ->
                onColorChange(listOf(color, selectedColors.lastOrNull() ?: Color.Gray))
                showStartColorPicker = false
            }
        )
    }

    // Color Picker Dialog for End Color
    if (showEndColorPicker) {
        ColorPickerDialog(
            onDismissRequest = { showEndColorPicker = false },
            onColorSelected = { color ->
                onColorChange(listOf(selectedColors.firstOrNull() ?: Color.Gray, color))
                showEndColorPicker = false
            }
        )
    }
}

@Composable
private fun ColorPickerDialog(
    onDismissRequest: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Pick a color") },
        text = {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                val colors = listOf(
                    Color.Red, Color.Green, Color.Blue,
                    Color.Yellow, Color.Cyan, Color.Magenta,
                    Color.DarkGray, Color.LightGray, Color.Gray,
                    Color(0xFF6B4DE6), Color(0xFFFF5F6D), Color(0xFF11998E),
                    Color(0xFF4158D0), Color(0xFF0093E9), Color(0xFFFF3CAC),
                    Color(0xFFFBB034), Color(0xFF8E2DE2), Color(0xFF56CCF2)
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(colors) { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color = color)
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { onColorSelected(color) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}