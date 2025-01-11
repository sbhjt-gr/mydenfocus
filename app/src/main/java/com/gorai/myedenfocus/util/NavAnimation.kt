package com.gorai.myedenfocus.util

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.spec.DestinationStyle

private const val ANIMATION_DURATION = 300
private const val SLIDE_OFFSET = 40 // 40dp slide offset like Android Pie

object NavAnimation : DestinationStyle.Animated {
    override fun AnimatedContentTransitionScope<NavBackStackEntry>.enterTransition(): EnterTransition? {
        return slideInVertically(
            animationSpec = tween(ANIMATION_DURATION),
            initialOffsetY = { SLIDE_OFFSET }
        ) + fadeIn(
            animationSpec = tween(ANIMATION_DURATION)
        )
    }

    override fun AnimatedContentTransitionScope<NavBackStackEntry>.exitTransition(): ExitTransition? {
        return fadeOut(
            animationSpec = tween(ANIMATION_DURATION)
        )
    }

    override fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnterTransition(): EnterTransition? {
        return fadeIn(
            animationSpec = tween(ANIMATION_DURATION)
        )
    }

    override fun AnimatedContentTransitionScope<NavBackStackEntry>.popExitTransition(): ExitTransition? {
        return slideOutVertically(
            animationSpec = tween(ANIMATION_DURATION),
            targetOffsetY = { SLIDE_OFFSET }
        ) + fadeOut(
            animationSpec = tween(ANIMATION_DURATION)
        )
    }
} 