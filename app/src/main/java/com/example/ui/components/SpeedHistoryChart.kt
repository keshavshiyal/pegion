package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.download.model.SpeedSample

@Composable
fun SpeedHistoryChart(
    samples: List<SpeedSample>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    if (samples.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Waiting for speed telemetry…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        return
    }

    val maxSpeed = (samples.maxOfOrNull { it.bytesPerSecond } ?: 1L).coerceAtLeast(1024L)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(vertical = 8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val stepX = if (samples.size > 1) width / (samples.size - 1) else width

            // Background grid lines (3 subtle horizontal guides)
            val gridColor = lineColor.copy(alpha = 0.12f)
            for (i in 1..3) {
                val y = height * (i / 4f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val path = Path()
            val fillPath = Path()

            samples.forEachIndexed { index, sample ->
                val x = index * stepX
                val normalizedY = (sample.bytesPerSecond.toFloat() / maxSpeed.toFloat()).coerceIn(0f, 1f)
                val y = height - (normalizedY * (height * 0.85f))

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    val prevSample = samples[index - 1]
                    val prevX = (index - 1) * stepX
                    val prevNormY = (prevSample.bytesPerSecond.toFloat() / maxSpeed.toFloat()).coerceIn(0f, 1f)
                    val prevY = height - (prevNormY * (height * 0.85f))

                    // Smooth bezier curve
                    val controlX1 = prevX + (x - prevX) / 2
                    val controlY1 = prevY
                    val controlX2 = prevX + (x - prevX) / 2
                    val controlY2 = y

                    path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                    fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                }

                if (index == samples.size - 1) {
                    fillPath.lineTo(x, height)
                    fillPath.close()
                }
            }

            // Draw gradient fill
            val gradientBrush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent),
                startY = 0f,
                endY = height
            )
            drawPath(fillPath, brush = gradientBrush)

            // Draw line stroke
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw current point pulse dot
            val lastSample = samples.last()
            val lastX = (samples.size - 1) * stepX
            val lastNormY = (lastSample.bytesPerSecond.toFloat() / maxSpeed.toFloat()).coerceIn(0f, 1f)
            val lastY = height - (lastNormY * (height * 0.85f))

            drawCircle(
                color = lineColor,
                radius = 5.dp.toPx(),
                center = Offset(lastX, lastY)
            )
            drawCircle(
                color = Color.White,
                radius = 2.5.dp.toPx(),
                center = Offset(lastX, lastY)
            )
        }
    }
}
