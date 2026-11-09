package com.swordfish.touchinput.radial.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme

/**
 * Native transcription of design/cartdeck-v03/controls/dpad-master.svg.
 * One physical cross, two direction bars and one centre hub. No round plate,
 * individual arrow buttons, new pointer handlers or input-ID changes.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun LemuroidCrossForeground(
    allowDiagonals: Boolean,
    directionState: State<Offset>,
) {
    val theme = LocalLemuroidPadTheme.current
    val cross =
        remember {
            Path().apply {
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
        }
    val pressed = directionState.value != Offset.Zero
    Canvas(Modifier.fillMaxSize()) {
        val scaleFactor = size.minDimension / 128f
        val insetX = (size.width - 128f * scaleFactor) / 2f
        val insetY = (size.height - 128f * scaleFactor) / 2f
        withTransform({
            translate(left = insetX, top = insetY)
            scale(scaleX = scaleFactor, scaleY = scaleFactor, pivot = Offset.Zero)
        }) {
            drawPath(cross, theme.foregroundFill(pressed))
            drawPath(
                cross,
                color = if (pressed) theme.palette.accent else theme.palette.trim,
                style = Stroke(width = 2.7f, join = StrokeJoin.Bevel),
            )
            val detail = theme.palette.onButton.copy(alpha = if (pressed) 0.78f else 0.42f)
            drawLine(detail, Offset(56f, 31f), Offset(72f, 31f), strokeWidth = 2.7f)
            drawLine(detail, Offset(56f, 97f), Offset(72f, 97f), strokeWidth = 2.7f)
            drawCircle(
                color = theme.foregroundFill(pressed),
                radius = 7.1f,
                center = Offset(64f, 64f),
            )
            drawCircle(
                color = theme.palette.trim,
                radius = 7.1f,
                center = Offset(64f, 64f),
                style = Stroke(width = 2.7f),
            )
        }
    }
}
