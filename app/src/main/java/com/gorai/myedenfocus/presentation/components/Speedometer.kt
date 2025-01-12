package com.gorai.myedenfocus.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Speedometer(
    modifier: Modifier = Modifier,
    headingText: String,
    value: Float,
    maxValue: Float,
    displayText: String
) {
    val sweepAngle by animateFloatAsState(
        targetValue = if (value.isNaN() || maxValue == 0f) 
            0f 
        else 
            ((value / maxValue) * 180f).coerceIn(0f, 180f),
        animationSpec = tween(1000),
        label = "speedometer"
    )

    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    val textStyle = MaterialTheme.typography.titleSmall
    val textColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = headingText,
            style = textStyle,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Box(
            modifier = Modifier
                .size(100.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background arc
                drawArc(
                    color = surfaceColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    style = Stroke(width = 12f, cap = StrokeCap.Round),
                    size = Size(size.width, size.height)
                )

                // Progress arc
                drawArc(
                    color = primaryColor,
                    startAngle = 180f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 12f, cap = StrokeCap.Round),
                    size = Size(size.width, size.height)
                )

                // Draw tick marks
                for (i in 0..4) {
                    val angle = PI * (0.5 + i * 0.25)
                    val startRadius = size.width * 0.35f
                    val endRadius = size.width * 0.45f
                    val start = Offset(
                        (center.x + cos(angle) * startRadius).toFloat(),
                        (center.y + sin(angle) * startRadius).toFloat()
                    )
                    val end = Offset(
                        (center.x + cos(angle) * endRadius).toFloat(),
                        (center.y + sin(angle) * endRadius).toFloat()
                    )
                    drawLine(
                        color = outlineColor,
                        start = start,
                        end = end,
                        strokeWidth = 2f
                    )
                }
            }
            
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
} 