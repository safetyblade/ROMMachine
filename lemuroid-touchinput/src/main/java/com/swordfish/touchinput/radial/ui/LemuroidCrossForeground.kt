package com.swordfish.touchinput.radial.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme

/**
 * Original CARTDECK dpad-master.svg silhouette (viewBox 128x128).
 * Drawn as one chamfered physical cross. ControlCross in PadKit remains the
 * sole hit-zone and gesture owner, including diagonals and held directions.
 */
@Composable
fun LemuroidCrossForeground(
    allowDiagonals: Boolean,
    directionState: State<Offset>,
) {
    val theme = LocalLemuroidPadTheme.current
    Canvas(Modifier.fillMaxSize()) {
        val side = minOf(size.width, size.height)
        if (side <= 0f) return@Canvas
        val scale = side / 128f
        val outline = Path().apply {
            moveTo(48f, 9f)
            lineTo(80f, 9f)
            lineTo(83f, 12f)
            lineTo(83f, 44f)
            lineTo(116f, 44f)
            lineTo(119f, 47f)
            lineTo(119f, 81f)
            lineTo(116f, 84f)
            lineTo(83f, 84f)
            lineTo(83f, 116f)
            lineTo(80f, 119f)
            lineTo(48f, 119f)
            lineTo(45f, 116f)
            lineTo(45f, 84f)
            lineTo(12f, 84f)
            lineTo(9f, 81f)
            lineTo(9f, 47f)
            lineTo(12f, 44f)
            lineTo(45f, 44f)
            lineTo(45f, 12f)
            close()
        }
        val pressed = directionState.value != Offset.Zero
        withTransform({
            translate((size.width - side) / 2f, (size.height - side) / 2f)
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            drawPath(outline, if (pressed) theme.trim else theme.button)
            drawPath(outline, theme.trim, style = Stroke(width = 2.7f))
            drawLine(theme.trim, Offset(56f, 31f), Offset(72f, 31f), 2.7f)
            drawLine(theme.trim, Offset(56f, 97f), Offset(72f, 97f), 2.7f)
            drawCircle(theme.button, radius = 7.1f, center = Offset(64f, 64f))
            drawCircle(theme.trim, radius = 7.1f, center = Offset(64f, 64f), style = Stroke(width = 2.7f))
            if (pressed) {
                drawCircle(theme.accent.copy(alpha = 0.55f), radius = 4f, center = Offset(64f, 64f))
            }
        }
    }
}
