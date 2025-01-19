package com.gorai.myedenfocus.presentation.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gorai.myedenfocus.R
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.gorai.myedenfocus.presentation.destinations.DashBoardScreenRouteDestination
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val image: Int,
    val backgroundColor: Color = Color(0xFF1A1A1A)
)

val onboardingPages = listOf(
    OnboardingPage(
        title = "Welcome to MyedenFocus",
        description = "Your personal study companion for better focus and productivity",
        image = R.drawable.app_icon,
        backgroundColor = Color(0xFF2196F3)
    ),
    OnboardingPage(
        title = "Smart Study Timer",
        description = "Stay focused with our intelligent Pomodoro timer and track your study sessions",
        image = R.drawable.app_icon,
        backgroundColor = Color(0xFF4CAF50)
    ),
    OnboardingPage(
        title = "Track Progress",
        description = "Monitor your study habits with detailed analytics and insights",
        image = R.drawable.app_icon,
        backgroundColor = Color(0xFF9C27B0)
    ),
    OnboardingPage(
        title = "Enable Notifications",
        description = "Allow notifications to get timer updates and stay on track with your study sessions",
        image = R.drawable.app_icon,
        backgroundColor = Color(0xFFFF9800)
    ),
    OnboardingPage(
        title = "Stay Mindful",
        description = "Take meditation breaks to maintain mental clarity and reduce stress",
        image = R.drawable.app_icon,
        backgroundColor = Color(0xFF9C27B0)
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Destination
@Composable
fun OnboardingScreen(
    navigator: DestinationsNavigator,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState { onboardingPages.size }
    val scope = rememberCoroutineScope()
    val systemUiController = rememberSystemUiController()
    val shouldNavigateNext by viewModel.shouldNavigateNext.collectAsState()

    LaunchedEffect(shouldNavigateNext) {
        if (shouldNavigateNext) {
            if (pagerState.currentPage < onboardingPages.size - 1) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            } else {
                navigator.navigate(DashBoardScreenRouteDestination)
            }
        }
    }

    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color.Transparent,
            darkIcons = false
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.onPermissionResult(isGranted)
        }
    )

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPageChange(pagerState.currentPage)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            OnboardingPage(
                page = onboardingPages[page],
                modifier = Modifier.fillMaxSize(),
                notificationPermissionLauncher = notificationPermissionLauncher
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    Modifier
                        .height(10.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(onboardingPages.size) { iteration ->
                        val width = if (pagerState.currentPage == iteration) 25.dp else 10.dp
                        
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    if (pagerState.currentPage == iteration)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                .width(width)
                                .height(10.dp)
                                .animateContentSize()
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pagerState.currentPage < onboardingPages.size - 1) {
                        TextButton(
                            onClick = {
                                viewModel.onNextClick()
                                navigator.navigate(DashBoardScreenRouteDestination)
                            }
                        ) {
                            Text(
                                "Skip",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        
                        Button(
                            onClick = {
                                if (pagerState.currentPage == 3) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.onPermissionResult(true)
                                    }
                                } else {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(
                                "Next",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.onNextClick()
                                navigator.navigate(DashBoardScreenRouteDestination)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                "Get Started",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingPage(
    page: OnboardingPage,
    modifier: Modifier = Modifier,
    notificationPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>? = null
) {
    Box(
        modifier = modifier
            .background(page.backgroundColor)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Image(
                painter = painterResource(id = page.image),
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .scale(1.2f)
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = page.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                if (page.title == "Enable Notifications") {
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .height(56.dp)
                            .fillMaxWidth(0.8f)
                    ) {
                        Text(
                            "Allow Notifications",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
} 