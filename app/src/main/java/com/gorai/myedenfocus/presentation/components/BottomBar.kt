package com.gorai.myedenfocus.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun BottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem(
            name = "Schedule",
            route = "schedule",
            icon = Icons.Default.Home
        ),
        BottomNavItem(
            name = "Meditate",
            route = "meditate",
            icon = Icons.Default.SelfImprovement
        ),
        BottomNavItem(
            name = "Chat",
            route = "chat",
            icon = Icons.Default.Chat
        )
    )

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.name
                    )
                },
                label = { Text(text = item.name) }
            )
        }
    }
}

private data class BottomNavItem(
    val name: String,
    val route: String,
    val icon: ImageVector
) 