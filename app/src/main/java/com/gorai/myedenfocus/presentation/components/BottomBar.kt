package com.gorai.myedenfocus.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import com.gorai.myedenfocus.presentation.navigation.NavigationViewModel

@Composable
fun BottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    viewModel: NavigationViewModel = hiltViewModel()
) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = viewModel.currentRoute == item.route,
                onClick = { 
                    viewModel.navigateTo(item.route)
                    onNavigate(item.route)
                },
                icon = { 
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(text = item.label) }
            )
        }
    }
}

private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

private val bottomNavItems = listOf(
    BottomNavItem(
        label = "Study Schedule",
        icon = Icons.Default.DateRange,
        route = "schedule"
    ),
    BottomNavItem(
        label = "Meditate",
        icon = Icons.Default.Favorite,
        route = "meditate"
    )
) 