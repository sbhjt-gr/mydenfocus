package com.gorai.myedenfocus.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val gradientColors = listOf(
    Color(0xFF6B4DE6), Color(0xFFFF5F6D), Color(0xFF11998E),
    Color(0xFF4158D0), Color(0xFF0093E9), Color(0xFFFF3CAC),
    Color(0xFFFBB034), Color(0xFF8E2DE2), Color(0xFF56CCF2),
    Color(0xFFFF0000), Color(0xFF00FF00), Color(0xFF0000FF)
)

@Composable
fun ColorPickerDialog(
    onDismissRequest: () -> Unit,
    onColorSelected: (Color, Color) -> Unit,
    initialStartColor: Color,
    initialEndColor: Color
) {
    var selectedStartColor by remember { mutableStateOf(initialStartColor) }
    var selectedEndColor by remember { mutableStateOf(initialEndColor) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Choose Colors") },
        text = {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Color preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(selectedStartColor, selectedEndColor)
                            )
                        )
                )

                Text("Start Color", style = MaterialTheme.typography.titleSmall)
                ColorGrid(
                    colors = gradientColors,
                    selectedColor = selectedStartColor,
                    onColorSelected = { selectedStartColor = it }
                )

                Text("End Color", style = MaterialTheme.typography.titleSmall)
                ColorGrid(
                    colors = gradientColors,
                    selectedColor = selectedEndColor,
                    onColorSelected = { selectedEndColor = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onColorSelected(selectedStartColor, selectedEndColor) }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ColorGrid(
    colors: List<Color>,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(colors) { color ->
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (color == selectedColor) 2.dp else 1.dp,
                        color = if (color == selectedColor) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(color) }
            )
        }
    }
} 