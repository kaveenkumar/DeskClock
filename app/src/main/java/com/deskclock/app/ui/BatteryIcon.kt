package com.deskclock.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Horizontal battery glyph: outlined body, terminal nub, fill proportional to [percent], and a
 * lightning bolt overlaid while power is connected — solid while actively [charging], faint while
 * merely [plugged] (a charge limiter holding the level). Drawn rather than iconography so it
 * scales with the info row's font size and inherits the theme's text color.
 */
@Composable
fun BatteryIcon(
    percent: Int,
    charging: Boolean,
    plugged: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val strokeWidth = size.height * 0.09f
        val nubWidth = size.width * 0.06f
        val bodyWidth = size.width - nubWidth
        val inset = strokeWidth / 2f

        drawRoundRect(
            color = color,
            topLeft = Offset(inset, inset),
            size = Size(bodyWidth - strokeWidth, size.height - strokeWidth),
            cornerRadius = CornerRadius(size.height * 0.18f),
            style = Stroke(width = strokeWidth),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(bodyWidth + nubWidth * 0.1f, size.height * 0.3f),
            size = Size(nubWidth * 0.9f, size.height * 0.4f),
            cornerRadius = CornerRadius(nubWidth * 0.4f),
        )

        val padding = strokeWidth * 1.7f
        val fillMax = bodyWidth - 2 * padding
        drawRoundRect(
            color = color,
            topLeft = Offset(padding, padding),
            size = Size(fillMax * (percent.coerceIn(0, 100) / 100f), size.height - 2 * padding),
            cornerRadius = CornerRadius(size.height * 0.08f),
        )

        if (plugged) {
            // Bolt drawn in the inverse color so it stays visible over the fill.
            val bolt = Path().apply {
                moveTo(bodyWidth * 0.56f, size.height * 0.08f)
                lineTo(bodyWidth * 0.30f, size.height * 0.58f)
                lineTo(bodyWidth * 0.47f, size.height * 0.58f)
                lineTo(bodyWidth * 0.42f, size.height * 0.92f)
                lineTo(bodyWidth * 0.70f, size.height * 0.40f)
                lineTo(bodyWidth * 0.52f, size.height * 0.40f)
                lineTo(bodyWidth * 0.62f, size.height * 0.08f)
                close()
            }
            drawPath(bolt, color = Color.Black.copy(alpha = if (charging) 0.75f else 0.4f))
        }
    }
}
