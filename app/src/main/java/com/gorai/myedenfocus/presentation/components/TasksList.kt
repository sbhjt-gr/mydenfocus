package com.gorai.myedenfocus.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gorai.myedenfocus.R
import com.gorai.myedenfocus.domain.model.Task
import com.gorai.myedenfocus.util.Priority
import com.gorai.myedenfocus.util.changeMillsToDateString

fun LazyListScope.tasksList(
    sectionTitle: String,
    emptyListText: String,
    tasks: List<Task>,
    onTaskCardClick: (Int) -> Unit,
    onCheckBoxClick: (Task) -> Unit
) {
    item {
        Text(
            text = sectionTitle.replace("Task", "Topic"),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
    
    if (tasks.isEmpty()) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    modifier = Modifier
                        .size(140.dp)
                        .padding(bottom = 16.dp),
                    painter = painterResource(R.drawable.img_tasks),
                    contentDescription = emptyListText.replace("task", "topic")
                )
                Text(
                    text = emptyListText.replace("task", "topic"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        items(tasks) { task ->
            TaskCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                task = task,
                onCheckBoxClick = { onCheckBoxClick(task) },
                onClick = { task.taskId?.let { onTaskCardClick(it) } }
            )
        }
    }
}

@Composable
private fun TaskCard(
    modifier: Modifier = Modifier,
    task: Task,
    onCheckBoxClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Priority indicator and Checkbox
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Priority.fromInt(task.priority).color.copy(alpha = 0.1f))
            ) {
                TaskCheckBox(
                    isComplete = task.isComplete,
                    borderColor = Priority.fromInt(task.priority).color,
                    onCheckBoxClick = onCheckBoxClick
                )
            }
            
            // Task Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = task.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.isComplete) TextDecoration.LineThrough else TextDecoration.None
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Due Date
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = task.dueDate.changeMillsToDateString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Duration
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (task.taskDuration > 0) {
                                "${task.taskDuration / 60}h ${task.taskDuration % 60}m"
                            } else {
                                "No duration"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Priority Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Priority.fromInt(task.priority).color.copy(alpha = 0.1f),
                contentColor = Priority.fromInt(task.priority).color
            ) {
                Text(
                    text = Priority.fromInt(task.priority).name,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}