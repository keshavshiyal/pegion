package com.pegion.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pegion.ui.theme.AppThemeMode
import com.pegion.ui.theme.PegionTheme

/**
 * Single, pure download icon with a modern "vanished / ghost dissolution" effect.
 * Uses a downward fading gradient and dissolving particle trails without bulky circles or badges.
 */
@Composable
fun VanishedDownloadIcon(
    isFiltered: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vanished_motion")
    
    val floatY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_y"
    )

    val vanishPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vanish_pulse"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(100.dp)
            .offset(y = floatY.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(90.dp)) {
            val width = size.width
            val height = size.height

            if (isFiltered) {
                // Vanishing Search Glyph
                drawVanishedSearch(primaryColor, vanishPulse)
            } else {
                // Single Vanished Download Icon with Dissolving Gradient
                drawVanishedDownload(primaryColor, vanishPulse)
            }
        }
    }
}

private fun DrawScope.drawVanishedDownload(primary: Color, pulse: Float) {
    val cx = size.width / 2f
    val cy = size.height / 2f

    // Vertical Vanishing Brush: Full saturation at top, dissolving smoothly into complete transparency at bottom
    val vanishBrush = Brush.verticalGradient(
        colors = listOf(
            primary.copy(alpha = 0.95f * pulse),
            primary.copy(alpha = 0.65f * pulse),
            primary.copy(alpha = 0.25f),
            primary.copy(alpha = 0.04f),
            Color.Transparent
        ),
        startY = cy - 36.dp.toPx(),
        endY = cy + 42.dp.toPx()
    )

    val ghostBorderBrush = Brush.verticalGradient(
        colors = listOf(
            primary.copy(alpha = 0.8f * pulse),
            primary.copy(alpha = 0.35f),
            Color.Transparent
        ),
        startY = cy - 36.dp.toPx(),
        endY = cy + 30.dp.toPx()
    )

    val arrowPath = Path().apply {
        // Central Download Arrow: Sharp, modern geometric arrow
        val arrowTop = cy - 32.dp.toPx()
        val stemWidth = 7.dp.toPx()
        val arrowHeadWidth = 24.dp.toPx()
        val arrowHeadBaseY = cy + 4.dp.toPx()
        val arrowTipY = cy + 18.dp.toPx()

        // Top stem
        moveTo(cx - stemWidth / 2f, arrowTop)
        lineTo(cx + stemWidth / 2f, arrowTop)
        lineTo(cx + stemWidth / 2f, arrowHeadBaseY)
        
        // Arrowhead right wing
        lineTo(cx + arrowHeadWidth / 2f, arrowHeadBaseY)
        // Arrowhead tip
        lineTo(cx, arrowTipY)
        // Arrowhead left wing
        lineTo(cx - arrowHeadWidth / 2f, arrowHeadBaseY)
        // Back to stem
        lineTo(cx - stemWidth / 2f, arrowHeadBaseY)
        close()
    }

    // Draw dissolving filled arrow
    drawPath(path = arrowPath, brush = vanishBrush)

    // Draw crisp top contour stroke that fades away
    drawPath(
        path = arrowPath,
        brush = ghostBorderBrush,
        style = Stroke(
            width = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // Bottom Vanished Carrier Tray (Dissolved horizontal line with open ends)
    val trayY = cy + 28.dp.toPx()
    val trayHalfWidth = 22.dp.toPx()
    val trayCornerHeight = 6.dp.toPx()

    val trayPath = Path().apply {
        moveTo(cx - trayHalfWidth, trayY - trayCornerHeight)
        lineTo(cx - trayHalfWidth, trayY)
        lineTo(cx + trayHalfWidth, trayY)
        lineTo(cx + trayHalfWidth, trayY - trayCornerHeight)
    }

    val trayBrush = Brush.horizontalGradient(
        colors = listOf(
            Color.Transparent,
            primary.copy(alpha = 0.45f * pulse),
            primary.copy(alpha = 0.6f * pulse),
            primary.copy(alpha = 0.45f * pulse),
            Color.Transparent
        ),
        startX = cx - trayHalfWidth - 4.dp.toPx(),
        endX = cx + trayHalfWidth + 4.dp.toPx()
    )

    drawPath(
        path = trayPath,
        brush = trayBrush,
        style = Stroke(
            width = 2.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // Vanishing Dissolved Speed Dots / Particles Below
    val trail1Y = cy + 34.dp.toPx()
    val trail2Y = cy + 39.dp.toPx()

    // Trail Line 1 (dissolving center dash)
    drawLine(
        color = primary.copy(alpha = 0.25f * pulse),
        start = Offset(cx - 10.dp.toPx(), trail1Y),
        end = Offset(cx + 10.dp.toPx(), trail1Y),
        strokeWidth = 1.5.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Trail Line 2 (subtle micro dot)
    drawLine(
        color = primary.copy(alpha = 0.12f * pulse),
        start = Offset(cx - 4.dp.toPx(), trail2Y),
        end = Offset(cx + 4.dp.toPx(), trail2Y),
        strokeWidth = 1.5.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawVanishedSearch(primary: Color, pulse: Float) {
    val cx = size.width / 2f
    val cy = size.height / 2f

    val vanishBrush = Brush.verticalGradient(
        colors = listOf(
            primary.copy(alpha = 0.9f * pulse),
            primary.copy(alpha = 0.35f * pulse),
            Color.Transparent
        ),
        startY = cy - 28.dp.toPx(),
        endY = cy + 32.dp.toPx()
    )

    // Search Lens
    drawCircle(
        brush = vanishBrush,
        radius = 16.dp.toPx(),
        center = Offset(cx - 4.dp.toPx(), cy - 4.dp.toPx()),
        style = Stroke(width = 2.5.dp.toPx())
    )

    // Search Handle (Dissolving downwards)
    drawLine(
        brush = vanishBrush,
        start = Offset(cx + 8.dp.toPx(), cy + 8.dp.toPx()),
        end = Offset(cx + 22.dp.toPx(), cy + 22.dp.toPx()),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Faint Ghost Echo Ring
    drawCircle(
        color = primary.copy(alpha = 0.1f * pulse),
        radius = 24.dp.toPx(),
        center = Offset(cx - 4.dp.toPx(), cy - 4.dp.toPx()),
        style = Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 12f))
        )
    )
}

/**
 * Clean, minimalistic Empty State featuring the vanished single download icon.
 */
@Composable
fun EmptyStateView(
    isFiltered: Boolean,
    onAddClick: () -> Unit = {},
    onClearFilter: () -> Unit = onAddClick,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Single Vanished Download Icon
            VanishedDownloadIcon(isFiltered = isFiltered)

            Spacer(modifier = Modifier.height(16.dp))

            // Headline
            Text(
                text = if (isFiltered) "No Matching Downloads" else "No Downloads Yet",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Friendly Subtitle
            Text(
                text = if (isFiltered) {
                    "No downloads match your query. Try searching for another name or reset filters."
                } else {
                    "Ready to deliver. Paste a download link or tap '+' to start high-speed downloading."
                },
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action Button
            if (isFiltered) {
                OutlinedButton(
                    onClick = onClearFilter,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterAltOff,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Clear Filter",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            } else {
                Button(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New Download",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Preview(name = "Empty State Light", showBackground = true)
@Composable
private fun EmptyStateLightPreview() {
    PegionTheme(dynamicColor = false) {
        EmptyStateView(isFiltered = false)
    }
}

@Preview(name = "Empty State Dark", showBackground = true)
@Composable
private fun EmptyStateDarkPreview() {
    PegionTheme(themeMode = AppThemeMode.DARK, dynamicColor = false) {
        EmptyStateView(isFiltered = false)
    }
}

@Preview(name = "Empty State Filtered Dark", showBackground = true)
@Composable
private fun EmptyStateFilteredDarkPreview() {
    PegionTheme(themeMode = AppThemeMode.DARK, dynamicColor = false) {
        EmptyStateView(isFiltered = true)
    }
}

@Preview(name = "Empty State AMOLED", showBackground = true)
@Composable
private fun EmptyStateAmoledPreview() {
    PegionTheme(themeMode = AppThemeMode.AMOLED, dynamicColor = false) {
        EmptyStateView(isFiltered = false)
    }
}
