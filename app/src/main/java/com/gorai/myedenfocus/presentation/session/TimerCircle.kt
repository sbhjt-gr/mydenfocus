import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TimerCircle(
    hours: String,
    minutes: String,
    seconds: String,
    totalSeconds: Int,
    selectedDurationMinutes: Int
) {
    val colorScheme = MaterialTheme.colorScheme
    val progress = remember(totalSeconds, selectedDurationMinutes) {
        totalSeconds.toFloat() / (selectedDurationMinutes * 60)
    }
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2
        
        // Draw outer circle
        drawCircle(
            color = colorScheme.surfaceVariant.copy(alpha = 0.3f),
            radius = radius,
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Draw tick marks and numbers
        for (i in 0..60) {
            val angle = (2 * PI * i) / 60
            val tickLength = if (i % 5 == 0) 20.dp.toPx() else 10.dp.toPx()
            val startRadius = radius - tickLength
            val endRadius = radius
            
            val start = Offset(
                (center.x + cos(angle) * startRadius).toFloat(),
                (center.y + sin(angle) * startRadius).toFloat()
            )
            val end = Offset(
                (center.x + cos(angle) * endRadius).toFloat(),
                (center.y + sin(angle) * endRadius).toFloat()
            )
            
            val tickColor = if (i % 5 == 0)
                colorScheme.primary
            else
                colorScheme.outline.copy(alpha = 0.5f)
            
            drawLine(
                color = tickColor,
                start = start,
                end = end,
                strokeWidth = if (i % 5 == 0) 2.dp.toPx() else 1.dp.toPx()
            )
        }
        
        // Draw progress arc
        drawArc(
            color = colorScheme.primary,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            style = Stroke(
                width = 12.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
        
        // Draw inner circles
        drawCircle(
            color = colorScheme.surfaceVariant,
            radius = radius * 0.8f,
            style = Stroke(width = 4.dp.toPx())
        )
        
        drawCircle(
            color = colorScheme.surfaceVariant.copy(alpha = 0.3f),
            radius = radius * 0.7f,
            style = Stroke(width = 2.dp.toPx())
        )
        
        // Draw moving dot
        val safeProgress = progress.coerceIn(0f, 1f)
        val dotAngle = -90f + (360f * safeProgress)
        val dotRadius = radius * 0.9f
        val dotCenter = Offset(
            center.x + (dotRadius * cos(Math.toRadians(dotAngle.toDouble()))).toFloat(),
            center.y + (dotRadius * sin(Math.toRadians(dotAngle.toDouble()))).toFloat()
        )
        drawCircle(
            color = colorScheme.primary,
            radius = 6.dp.toPx(),
            center = dotCenter
        )
    }
} 