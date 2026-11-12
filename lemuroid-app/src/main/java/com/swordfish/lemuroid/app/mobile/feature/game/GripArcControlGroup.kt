package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckEditionTokens
import kotlinx.coroutines.flow.collect

/**
 * Native rendering of PR #126: permanent full L, source-accurate shell contour,
 * four fixed internal slot centres and elongated L/R-like shoulder buttons.
 * The housing has no pointer handlers; inactive slots do not exist in UI.
 */
@Composable
fun GripArcControlGroup(
    placement: GripArcPlacement,
    profile: GripArcProfile,
    tokens: CartdeckEditionTokens,
    onPressed: (Int, Boolean, Set<Int>) -> Unit,
) {
    val scale = placement.scale
    Box(Modifier.fillMaxSize()) {
        GripArcHousing(placement = placement, tokens = tokens)
        for (slot in 1..profile.count) {
            val rect = placement.targets.getValue(slot)
            val size = placement.visuals.getValue(slot)
            val mapping = profile.mappings.getOrNull(slot - 1).orEmpty().toSet()
            val interaction = remember(slot) { MutableInteractionSource() }
            var pressed by remember(slot) { mutableStateOf(false) }

            LaunchedEffect(interaction, mapping) {
                interaction.interactions.collect { event ->
                    when (event) {
                        is PressInteraction.Press -> {
                            pressed = true
                            onPressed(slot, true, mapping)
                        }
                        is PressInteraction.Release, is PressInteraction.Cancel -> {
                            pressed = false
                            onPressed(slot, false, mapping)
                        }
                    }
                }
            }
            DisposableEffect(slot, mapping) {
                onDispose { onPressed(slot, false, emptySet()) }
            }
            Box(
                modifier =
                    Modifier
                        .offset(x = rect.left.dp, y = rect.top.dp)
                        .size(rect.width.dp, rect.height.dp)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Button,
                            onClickLabel = "Press GripArc G$slot",
                        ) { },
                contentAlignment = Alignment.Center,
            ) {
                GripArcShoulderButton(
                    modifier = Modifier.size(size.w.dp, size.h.dp),
                    label = "G$slot",
                    tokens = tokens,
                    pressed = pressed,
                )
            }
        }
    }
}

@Composable
private fun GripArcHousing(
    placement: GripArcPlacement,
    tokens: CartdeckEditionTokens,
) {
    val housing = placement.housing
    val left = placement.side == GripArcSide.LEFT
    val outer = remember {
        Path().apply {
            moveTo(161f, 12f)
            lineTo(245f, 12f)
            quadraticBezierTo(265f, 12f, 265f, 32f)
            lineTo(265f, 277f)
            quadraticBezierTo(265f, 298f, 244f, 298f)
            lineTo(18f, 298f)
            quadraticBezierTo(7f, 298f, 7f, 287f)
            lineTo(7f, 231f)
            quadraticBezierTo(7f, 219f, 20f, 219f)
            lineTo(148f, 219f)
            quadraticBezierTo(157f, 219f, 157f, 208f)
            lineTo(157f, 32f)
            quadraticBezierTo(157f, 12f, 161f, 12f)
            close()
        }
    }
    val inner = remember {
        Path().apply {
            moveTo(173f, 23f)
            lineTo(242f, 23f)
            quadraticBezierTo(254f, 23f, 254f, 37f)
            lineTo(254f, 273f)
            quadraticBezierTo(254f, 285f, 242f, 285f)
            lineTo(20f, 285f)
            lineTo(20f, 231f)
            lineTo(149f, 231f)
            quadraticBezierTo(168f, 231f, 168f, 212f)
            lineTo(168f, 37f)
            quadraticBezierTo(168f, 23f, 173f, 23f)
            close()
        }
    }
    Box(
        modifier =
            Modifier
                .offset(x = housing.left.dp, y = housing.top.dp)
                .size(housing.width.dp, housing.height.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val u = size.width / 272f
            withTransform({
                scale(scaleX = u, scaleY = u, pivot = Offset.Zero)
                if (left) scale(scaleX = -1f, scaleY = 1f, pivot = Offset(136f, 154f))
            }) {
                drawPath(outer, tokens.chassis)
                drawPath(outer, tokens.secondary, style = Stroke(width = 2.2f))
                drawPath(inner, tokens.bezel)
                drawPath(inner, tokens.secondary.copy(alpha = 0.42f), style = Stroke(width = 1.4f))
                drawLine(
                    tokens.accent.copy(alpha = 0.65f),
                    Offset(259f, 53f),
                    Offset(259f, 276f),
                    strokeWidth = 3.5f,
                )
                drawLine(
                    tokens.accent.copy(alpha = 0.65f),
                    Offset(259f, 276f),
                    Offset(35f, 291f),
                    strokeWidth = 2.2f,
                )
                drawLine(
                    tokens.focus.copy(alpha = 0.45f),
                    Offset(173f, 28f),
                    Offset(240f, 28f),
                    strokeWidth = 1.3f,
                )
                drawLine(
                    tokens.onChassis.copy(alpha = 0.26f),
                    Offset(165f, 42f),
                    Offset(165f, 204f),
                    strokeWidth = 1.2f,
                )
                for (p in listOf(Offset(164f, 29f), Offset(245f, 37f), Offset(18f, 277f), Offset(247f, 290f))) {
                    drawCircle(tokens.bezel, radius = 5.5f, center = p)
                    drawCircle(tokens.secondary, radius = 5.5f, center = p, style = Stroke(width = 1.3f))
                    drawLine(
                        tokens.onChassis.copy(alpha = 0.55f),
                        p - Offset(2f, 0f),
                        p + Offset(2f, 0f),
                        strokeWidth = 1f,
                    )
                }
            }
        }
        Box(
            modifier =
                Modifier
                    .offset(x = ((if (left) 65f else 209f) * placement.scale - 38f).dp, y = (42f * scale).dp)
                    .width(76.dp)
                    .height(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "GripArc",
                color = tokens.onChassis,
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun GripArcShoulderButton(
    modifier: Modifier,
    label: String,
    tokens: CartdeckEditionTokens,
    pressed: Boolean,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.height / 2.5f
            drawRoundRect(
                color = tokens.bezel,
                cornerRadius = CornerRadius(r),
            )
            drawRoundRect(
                color = if (pressed) tokens.accent.copy(alpha = 0.62f) else tokens.secondary,
                topLeft = Offset(2.dp.toPx(), (if (pressed) 4 else 2).dp.toPx()),
                size = androidx.compose.ui.geometry.Size(
                    size.width - 4.dp.toPx(),
                    size.height - 6.dp.toPx(),
                ),
                cornerRadius = CornerRadius(r),
            )
            drawRoundRect(
                color = if (pressed) tokens.accent else tokens.onButton.copy(alpha = 0.36f),
                topLeft = Offset(2.dp.toPx(), (if (pressed) 4 else 2).dp.toPx()),
                size = androidx.compose.ui.geometry.Size(
                    size.width - 4.dp.toPx(),
                    size.height - 6.dp.toPx(),
                ),
                cornerRadius = CornerRadius(r),
                style = Stroke(width = 1.1.dp.toPx()),
            )
            drawLine(
                color = tokens.onButton.copy(alpha = if (pressed) 0.28f else 0.52f),
                start = Offset(size.width * 0.24f, size.height * 0.28f),
                end = Offset(size.width * 0.76f, size.height * 0.28f),
                strokeWidth = 1.dp.toPx(),
            )
        }
        Text(label, fontSize = 10.sp, color = tokens.onButton, maxLines = 1)
    }
}
