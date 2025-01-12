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

@Composable
fun AddSubjectDialog(
    isOpen: Boolean,
    title: String = "Add/Update Subject",
    selectedColors: List<Color>,
    onColorChange: (List<Color>) -> Unit,
    subjectName: String,
    goalHours: String,
    onSubjectNameChange: (String) -> Unit,
    onGoalHoursChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirmButtonClick: () -> Unit
) {
    var subjectNameError by rememberSaveable { mutableStateOf<String?>(null) }
    var goalHoursError by rememberSaveable { mutableStateOf<String?>(null) }
    var showStartColorPicker by rememberSaveable { mutableStateOf(false) }
    var showEndColorPicker by rememberSaveable { mutableStateOf(false) }

    subjectNameError = when {
        subjectName.isBlank() -> "Subject name cannot be empty."
        subjectName.length < 2 -> "Subject name cannot be less than 2 characters."
        subjectName.length > 50 -> "Subject name cannot be more than 50 characters."
        else -> null
    }

    goalHoursError = when {
        goalHours.isBlank() -> "Please enter goal study hours."
        goalHours.toFloatOrNull() == null -> "Please enter a valid number."
        goalHours.toFloat() < 1f -> "Goal study hours cannot be empty."
        goalHours.toFloat() > 1000f -> "Please set a maximum of 1000 hours"
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
                        label = { Text(text = "Subject Name") },
                        singleLine = true,
                        isError = subjectNameError != null && subjectName.isNotBlank(),
                        supportingText = { Text(text = subjectNameError.orEmpty()) }
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Goal Hours TextField
                    OutlinedTextField(
                        value = goalHours,
                        onValueChange = { onGoalHoursChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "Goal Hours") },
                        isError = goalHoursError != null && goalHours.isNotBlank(),
                        supportingText = { Text(text = goalHoursError.orEmpty()) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        )
                    )
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