package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PegionTheme

/**
 * Geometric, minimalist carrier pigeon mascot for Pegion brand identity.
 * Features a sleek aerodynamic pigeon silhouette facing right with delivery wings.
 */
@Composable
fun PegionLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    accentTint: Color = MaterialTheme.colorScheme.tertiary
) {
    Box(modifier = modifier.size(size)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // Scale factor to map 100x100 virtual coordinate space
            val sx = w / 100f
            val sy = h / 100f

            // 1. Sleek Main Body & Tail Path (Streamlined geometric pigeon facing right)
            val bodyPath = Path().apply {
                moveTo(20f * sx, 72f * sy) // Tail bottom tip
                lineTo(34f * sx, 66f * sy)
                cubicTo(
                    45f * sx, 68f * sy,
                    55f * sx, 66f * sy,
                    65f * sx, 58f * sy
                ) // Breast curve
                cubicTo(
                    74f * sx, 50f * sy,
                    78f * sx, 42f * sy,
                    80f * sx, 34f * sy
                ) // Neck to head
                // Beak
                lineTo(92f * sx, 38f * sy)
                lineTo(82f * sx, 42f * sy)
                // Throat and chest
                cubicTo(
                    76f * sx, 54f * sy,
                    68f * sx, 64f * sy,
                    54f * sx, 70f * sy
                )
                // Underbelly to tail
                cubicTo(
                    42f * sx, 74f * sy,
                    30f * sx, 76f * sy,
                    16f * sx, 78f * sy
                )
                close()
            }
            drawPath(path = bodyPath, color = tint, style = Fill)

            // 2. Dynamic Soaring Wing Path (Layered geometric upper wing)
            val wingPath = Path().apply {
                moveTo(38f * sx, 56f * sy)
                cubicTo(
                    42f * sx, 40f * sy,
                    52f * sx, 28f * sy,
                    70f * sx, 20f * sy
                ) // Wingtip curve up
                cubicTo(
                    62f * sx, 30f * sy,
                    52f * sx, 44f * sy,
                    46f * sx, 58f * sy
                )
                close()
            }
            drawPath(path = wingPath, color = tint.copy(alpha = 0.85f), style = Fill)

            // 3. Second Feather Wing Accent
            val wingFeatherPath = Path().apply {
                moveTo(44f * sx, 52f * sy)
                cubicTo(
                    50f * sx, 36f * sy,
                    60f * sx, 26f * sy,
                    76f * sx, 22f * sy
                )
                cubicTo(
                    68f * sx, 32f * sy,
                    58f * sx, 44f * sy,
                    52f * sx, 54f * sy
                )
                close()
            }
            drawPath(path = wingFeatherPath, color = tint.copy(alpha = 0.55f), style = Fill)

            // 4. Pigeon Eye (Sharp confident eye)
            drawCircle(
                color = Color.White,
                radius = 2.2f * sx,
                center = Offset(77f * sx, 34f * sy)
            )
            drawCircle(
                color = tint,
                radius = 1.2f * sx,
                center = Offset(77.5f * sx, 34f * sy)
            )

            // 5. Digital Delivery Parcel (Clean geometric envelope/capsule under body)
            val parcelPath = Path().apply {
                moveTo(44f * sx, 67f * sy)
                lineTo(56f * sx, 67f * sy)
                lineTo(56f * sx, 77f * sy)
                lineTo(44f * sx, 77f * sy)
                close()
            }
            drawPath(path = parcelPath, color = accentTint, style = Fill)
            // Parcel fold
            drawLine(
                color = Color.White.copy(alpha = 0.8f),
                start = Offset(44f * sx, 67f * sy),
                end = Offset(50f * sx, 72f * sy),
                strokeWidth = 1.5f * sx
            )
            drawLine(
                color = Color.White.copy(alpha = 0.8f),
                start = Offset(56f * sx, 67f * sy),
                end = Offset(50f * sx, 72f * sy),
                strokeWidth = 1.5f * sx
            )
        }
    }
}

@Preview(name = "Pegion Logo Light", showBackground = true)
@Composable
private fun PegionLogoLightPreview() {
    PegionTheme(dynamicColor = false) {
        PegionLogo(size = 96.dp)
    }
}

@Preview(name = "Pegion Logo Dark", showBackground = true)
@Composable
private fun PegionLogoDarkPreview() {
    PegionTheme(themeMode = com.example.ui.theme.AppThemeMode.DARK, dynamicColor = false) {
        PegionLogo(size = 96.dp)
    }
}
