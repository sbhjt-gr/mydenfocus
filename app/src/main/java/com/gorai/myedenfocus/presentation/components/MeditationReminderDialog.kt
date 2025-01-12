package com.gorai.myedenfocus.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme

@Composable
fun MeditationReminderDialog(
    onDismissRequest: () -> Unit,
    onMeditateClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "Time to Meditate",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "Taking a few minutes to meditate can help improve your focus and study effectiveness. Would you like to meditate before starting your study session?",
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            TextButton(onClick = onMeditateClick) {
                Text("Meditate Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkipClick) {
                Text("Skip")
            }
        }
    )
} 