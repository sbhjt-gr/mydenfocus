package com.gorai.myedenfocus.util

fun formatTimeHoursMinutes(hours: Float): String {
    val totalMinutes = (hours * 60).toInt()
    val wholeHours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        wholeHours > 0 && minutes > 0 -> "${wholeHours}h ${minutes}m"
        wholeHours > 0 -> "${wholeHours}h"
        minutes > 0 -> "${minutes}m"
        else -> "0h"
    }
}

fun secondsToHours(seconds: Int): Float {
    return seconds / 3600f
}

fun minutesToHours(minutes: Long): Float {
    return minutes / 60f
}

fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return when {
        hours > 0 && remainingMinutes > 0 -> "${hours}h ${remainingMinutes}m"
        hours > 0 -> "${hours}h"
        remainingMinutes > 0 -> "${remainingMinutes}m"
        else -> "0h"
    }
} 