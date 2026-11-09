package com.swordfish.touchinput.radial.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.common.compose.textUnit
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme

enum class CartdeckButtonShape {
    FACE,
    SHOULDER,
    PILL,
}

/**
 * Draws the physical button beneath the original PadKit foreground content.
 * The drawn finish has no pointer handlers; PadKit remains the sole input owner.
 */
@Composable
fun LemuroidButtonForeground(
    modifier: Modifier = Modifier,
    pressed: State<Boolean>,
    label: (@Composable BoxWithConstraintsScope.() -> Unit),
    icon: (@Composable BoxWithConstraintsScope.() -> Unit),
    shape: CartdeckButtonShape = CartdeckButtonShape.FACE,
    focused: Boolean = false,
    enabled: Boolean = true,
) {
    val theme = LocalLemuroidPadTheme.current
    BoxWithConstraints(
        modifier = modifier.fillMaxSize().padding(theme.foregroundPadding),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val active = pressed.value
            val ink = theme.palette.onButton
            val edge =
                when {
                    !enabled -> theme.palette.trim.copy(alpha = 0.35f)
                    focused -> theme.palette.focus
                    active -> theme.palette.accent
                    else -> theme.palette.trim
                }
            val surface =
                if (enabled) theme.foregroundFill(active) else theme.palette.button.copy(alpha = 0.38f)
            val down = if (active) 2.dp.toPx() else 0f
            when (shape) {
                CartdeckButtonShape.FACE -> {
                    val radius = size.minDimension * 0.47f
                    val centre = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        color = theme.palette.bezel,
                        radius = radius,
                        center = centre.copy(y = centre.y + 2.dp.toPx()),
                    )
                    val buttonCentre = centre.copy(y = centre.y - 2.dp.toPx() + down)
                    drawCircle(
                        color = surface,
                        radius = radius * 0.82f,
                        center = buttonCentre,
                    )
                    drawCircle(
                        color = edge,
                        radius = radius * 0.82f,
                        center = buttonCentre,
                        style = Stroke(width = 1.8.dp.toPx()),
                    )
                    drawArc(
                        color = ink.copy(alpha = if (active) 0.15f else 0.36f),
                        startAngle = 210f,
                        sweepAngle = 120f,
                        useCenter = false,
                        topLeft = Offset(buttonCentre.x - radius * 0.72f, buttonCentre.y - radius * 0.72f),
                        size = Size(radius * 1.44f, radius * 1.44f),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
                CartdeckButtonShape.SHOULDER,
                CartdeckButtonShape.PILL,
                -> {
                    val widthRatio = if (shape == CartdeckButtonShape.SHOULDER) 0.95f else 0.90f
                    val heightRatio = if (shape == CartdeckButtonShape.SHOULDER) 0.59f else 0.49f
                    val w = size.width * widthRatio
                    val h = size.height * heightRatio
                    val x = (size.width - w) / 2f
                    val y = (size.height - h) / 2f
                    val baseCorner = CornerRadius(h / 2f)
                    drawRoundRect(
                        color = theme.palette.bezel,
                        topLeft = Offset(x, y + 2.dp.toPx()),
                        size = Size(w, h),
                        cornerRadius = baseCorner,
                    )
                    val topY = y + down
                    val inset = 3.dp.toPx()
                    drawRoundRect(
                        color = surface,
                        topLeft = Offset(x + inset, topY + inset),
                        size = Size(w - 2f * inset, h - 2f * inset),
                        cornerRadius = CornerRadius((h - 2f * inset) / 2f),
                    )
                    drawRoundRect(
                        color = edge,
                        topLeft = Offset(x + inset, topY + inset),
                        size = Size(w - 2f * inset, h - 2f * inset),
                        cornerRadius = CornerRadius((h - 2f * inset) / 2f),
                        style = Stroke(width = 1.4.dp.toPx()),
                    )
                    drawLine(
                        color = ink.copy(alpha = if (active) 0.12f else 0.32f),
                        start = Offset(x + w * 0.23f, topY + h * 0.31f),
                        end = Offset(x + w * 0.77f, topY + h * 0.31f),
                        strokeWidth = 1.7.dp.toPx(),
                    )
                }
            }
        }
        icon()
        label()
    }
}

@Composable
fun LemuroidButtonForeground(
    modifier: Modifier = Modifier,
    pressed: State<Boolean>,
    label: String? = null,
    icon: Int? = null,
    iconScale: Float = 0.5f,
    labelScale: Float = 1.0f,
    shape: CartdeckButtonShape = CartdeckButtonShape.FACE,
    focused: Boolean = false,
    enabled: Boolean = true,
) {
    LemuroidButtonForeground(
        modifier = modifier,
        pressed = pressed,
        label = { LemuroidButtonForegroundLabel(label, labelScale, pressed) },
        icon = { LemuroidButtonForegroundIcon(icon, iconScale, pressed) },
        shape = shape,
        focused = focused,
        enabled = enabled,
    )
}

@Composable
private fun BoxWithConstraintsScope.LemuroidButtonForegroundIcon(
    icon: Int?,
    scale: Float,
    pressedState: State<Boolean>,
) {
    if (icon == null) return

    Icon(
        modifier = Modifier.size(maxWidth * scale, maxHeight * scale),
        painter = painterResource(icon),
        contentDescription = "",
        tint = LocalLemuroidPadTheme.current.icons(pressedState.value),
    )
}

@Composable
private fun BoxWithConstraintsScope.LemuroidButtonForegroundLabel(
    label: String?,
    scale: Float,
    pressedState: State<Boolean>,
) {
    if (label == null) return
    val fontSize = minOf(maxHeight * 0.5f * scale, maxWidth / label.length * scale)
    Text(
        modifier = Modifier.wrapContentSize(),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        text = label,
        color = LocalLemuroidPadTheme.current.icons(pressedState.value),
        fontSize = fontSize.textUnit(),
    )
}
