package com.gorai.myedenfocus.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gorai.myedenfocus.domain.model.Theme
import com.gorai.myedenfocus.util.NavAnimation
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.gorai.myedenfocus.presentation.components.ThemeDialog
import com.gorai.myedenfocus.presentation.components.NotificationSettingsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Destination(
    style = NavAnimation::class
)
@Composable
fun SettingsScreen(
    navigator: DestinationsNavigator
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDailyGoalDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Study Goals",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { /* Disabled */ },
                enabled = false
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily Study Goal",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                        Text(
                            text = "Set your daily study target",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                    Text(
                        text = "${state.dailyStudyGoal}h",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                    )
                }
            }

            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showThemeDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Theme",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = when (state.theme) {
                                Theme.SYSTEM -> "System default"
                                Theme.LIGHT -> "Light"
                                Theme.DARK -> "Dark"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Icon(
                        imageVector = when (state.theme) {
                            Theme.SYSTEM -> Icons.Default.DarkMode
                            Theme.LIGHT -> Icons.Default.LightMode
                            Theme.DARK -> Icons.Default.DarkMode
                        },
                        contentDescription = "Theme",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Notifications Section
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showNotificationDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Study Reminders",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Set daily study reminders",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = state.notificationsEnabled,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleNotifications) }
                    )
                }
            }

            // About Section
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Version",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "${com.gorai.myedenfocus.BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }

    // Show Dialogs
    if (showDailyGoalDialog) {
        DailyGoalDialog(
            currentGoal = state.dailyStudyGoal,
            onGoalChange = { viewModel.onEvent(SettingsEvent.OnDailyStudyGoalChange(it)) },
            onDismiss = { showDailyGoalDialog = false },
            onConfirm = {
                viewModel.onEvent(SettingsEvent.SaveDailyStudyGoal)
                showDailyGoalDialog = false
            }
        )
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = state.theme,
            onDismiss = { showThemeDialog = false },
            onThemeSelect = { theme ->
                viewModel.onEvent(SettingsEvent.SetTheme(theme))
                showThemeDialog = false
            }
        )
    }

    if (showNotificationDialog) {
        NotificationSettingsDialog(
            reminderTime = state.reminderTime,
            onDismiss = { showNotificationDialog = false },
            onTimeSelect = { time ->
                viewModel.onEvent(SettingsEvent.SetReminderTime(time))
                showNotificationDialog = false
            }
        )
    }
}

@Composable
private fun DailyGoalDialog(
    currentGoal: String,
    onGoalChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Daily Study Goal") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hours Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hours:",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    
                    IconButton(
                        onClick = {
                            val current = currentGoal.toFloatOrNull() ?: 0f
                            if (current >= 1) {
                                onGoalChange((current - 1f).toString())
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease Hours"
                        )
                    }
                    
                    Text(
                        text = "${currentGoal.toFloatOrNull()?.toInt() ?: 0}",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    IconButton(
                        onClick = {
                            val current = currentGoal.toFloatOrNull() ?: 0f
                            onGoalChange((current + 1f).toString())
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase Hours"
                        )
                    }
                }

                // Minutes Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Minutes:",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    
                    IconButton(
                        onClick = {
                            val current = currentGoal.toFloatOrNull() ?: 0f
                            if (current >= 0.25f) {
                                onGoalChange((current - 0.25f).toString())
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease Minutes"
                        )
                    }
                    
                    Text(
                        text = "${((currentGoal.toFloatOrNull() ?: 0f) % 1 * 60).toInt()}",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    IconButton(
                        onClick = {
                            val current = currentGoal.toFloatOrNull() ?: 0f
                            onGoalChange((current + 0.25f).toString())
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase Minutes"
                        )
                    }
                }

                Text(
                    text = "Total: ${currentGoal.toFloatOrNull() ?: 0}h",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = currentGoal.toFloatOrNull() ?: 0f > 0
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

@Composable
private fun ThemeSelectionDialog(
    currentTheme: Theme,
    onDismiss: () -> Unit,
    onThemeSelect: (Theme) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Theme") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOption(
                    text = "System default",
                    icon = Icons.Default.DarkMode,
                    selected = currentTheme == Theme.SYSTEM,
                    onClick = { onThemeSelect(Theme.SYSTEM) }
                )
                ThemeOption(
                    text = "Light",
                    icon = Icons.Default.LightMode,
                    selected = currentTheme == Theme.LIGHT,
                    onClick = { onThemeSelect(Theme.LIGHT) }
                )
                ThemeOption(
                    text = "Dark",
                    icon = Icons.Default.DarkMode,
                    selected = currentTheme == Theme.DARK,
                    onClick = { onThemeSelect(Theme.DARK) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ThemeOption(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}