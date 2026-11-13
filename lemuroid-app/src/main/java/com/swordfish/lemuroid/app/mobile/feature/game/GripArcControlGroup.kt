package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckEditionTokens
import kotlinx.coroutines.flow.collect

/**
 * V2.2 native material: compact permanent L, chamfered contour,
 * four fixed internal slot centres and elongated L/R-like shoulder buttons.
 * The housing has no pointer handlers; inactive slots do not exist in UI.
 */
@Composable
internal fun GripArcControlGroup(
    placement: GripArcPlacement,
    profile: GripArcProfile,
    tokens: CartdeckEditionTokens,
    onPressed: (Int, Boolean, Set<Int>) -> Unit,
    drawHousing: Boolean = true,
) {
    val scale = placement.scale
    Box(Modifier.fillMaxSize()) {
        if (drawHousing) GripArcHousing(placement = placement, tokens = tokens)
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
internal fun GripArcHousing(
    placement: GripArcPlacement,
    tokens: CartdeckEditionTokens,
) {
    val housing = placement.housing
    val left = placement.side == GripArcSide.LEFT
    val outer =
        remember {
            Path().apply {
                moveTo(104f, 4f)
                lineTo(148f, 4f)
                lineTo(156f, 12f)
                lineTo(156f, 148f)
                lineTo(148f, 156f)
                lineTo(12f, 156f)
                lineTo(4f, 148f)
                lineTo(4f, 124f)
                lineTo(12f, 116f)
                lineTo(96f, 116f)
                lineTo(96f, 12f)
                close()
            }
        }
    val inner =
        remember {
            Path().apply {
                moveTo(106f, 10f)
                lineTo(146f, 10f)
                lineTo(150f, 16f)
                lineTo(150f, 145f)
                lineTo(145f, 150f)
                lineTo(14f, 150f)
                lineTo(10f, 145f)
                lineTo(10f, 127f)
                lineTo(15f, 122f)
                lineTo(102f, 122f)
                lineTo(102f, 16f)
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
            val u = size.width / 160f
            withTransform({
                scale(scaleX = u, scaleY = u, pivot = Offset.Zero)
                if (left) scale(scaleX = -1f, scaleY = 1f, pivot = Offset(80f, 80f))
            }) {
                drawPath(outer, tokens.chassis)
                drawPath(outer, tokens.secondary, style = Stroke(width = 2.2f))
                drawPath(inner, tokens.bezel)
                drawPath(inner, tokens.secondary.copy(alpha = 0.42f), style = Stroke(width = 1.4f))
                drawLine(
                    tokens.accent.copy(alpha = 0.65f),
                    Offset(153f, 22f),
                    Offset(153f, 144f),
                    strokeWidth = 3.5f,
                )
                drawLine(
                    tokens.accent.copy(alpha = 0.65f),
                    Offset(153f, 144f),
                    Offset(22f, 153f),
                    strokeWidth = 2.2f,
                )
                drawLine(
                    tokens.focus.copy(alpha = 0.45f),
                    Offset(106f, 15f),
                    Offset(144f, 15f),
                    strokeWidth = 1.3f,
                )
                drawLine(
                    tokens.onChassis.copy(alpha = 0.26f),
                    Offset(99f, 24f),
                    Offset(99f, 108f),
                    strokeWidth = 1.2f,
                )
                for (p in listOf(Offset(102f, 14f), Offset(147f, 14f), Offset(14f, 144f), Offset(147f, 145f))) {
                    drawCircle(tokens.bezel, radius = 2.8f, center = p)
                    drawCircle(tokens.secondary, radius = 2.8f, center = p, style = Stroke(width = 1.3f))
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
                    .offset(x = ((if (left) 36f else 124f) * placement.scale - 24f).dp, y = (4f * placement.scale).dp)
                    .width(48.dp)
                    .height(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "GripArc",
                color = tokens.onChassis,
                fontSize = 8.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
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
                size =
                    androidx.compose.ui.geometry.Size(
                        size.width - 4.dp.toPx(),
                        size.height - 6.dp.toPx(),
                    ),
                cornerRadius = CornerRadius(r),
            )
            drawRoundRect(
                color = if (pressed) tokens.accent else tokens.onButton.copy(alpha = 0.36f),
                topLeft = Offset(2.dp.toPx(), (if (pressed) 4 else 2).dp.toPx()),
                size =
                    androidx.compose.ui.geometry.Size(
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
