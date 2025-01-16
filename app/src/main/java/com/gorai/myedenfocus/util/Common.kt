package com.gorai.myedenfocus.util

import androidx.compose.material3.SnackbarDuration
import androidx.compose.ui.graphics.Color
import com.gorai.myedenfocus.presentation.theme.Green
import com.gorai.myedenfocus.presentation.theme.Red
import com.gorai.myedenfocus.presentation.theme.Orange
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class Priority(val title: String, val color: Color, val value: Int) {
    LOW("Low", Green, 0),
    MEDIUM("Medium", Red, 1),
    HIGH("High", Orange, 2);

    companion object {
        fun fromInt(value: Int) = values().firstOrNull() {
            it.value == value
        } ?: MEDIUM
    }
}
fun Long?.changeMillsToDateString(): String {
    val date: LocalDate = this?.let {
        Instant.ofEpochMilli(it)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    } ?: LocalDate.now()
    return date.format(DateTimeFormatter.ofPattern("dd MMM, yyyy"))
}

fun Long.toHours(): Float {
    return (this.toFloat() / 3600f).coerceIn(0f, Float.MAX_VALUE)
}

sealed class SnackbarEvent {
    data class ShowSnackbar(
        val message: String,
        val duration: SnackbarDuration = SnackbarDuration.Short
    ) : SnackbarEvent()
    data object NavigateUp : SnackbarEvent()
}

fun Int.pad(): String {
    return this.toString().padStart(length = 2, padChar = '0')
}