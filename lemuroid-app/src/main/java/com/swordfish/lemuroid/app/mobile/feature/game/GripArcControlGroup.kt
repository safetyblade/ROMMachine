package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swordfish.lemuroid.app.mobile.feature.shelf.CartdeckLighting
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckEditionTokens
import kotlinx.coroutines.flow.collect

// GripArc artboard dimensions keep a visible margin inside the inner L on both hands.
private const val GRIP_ARC_POCKET_WIDTH = 28f
private const val GRIP_ARC_POCKET_HEIGHT = 16f
private const val GRIP_ARC_CAP_WIDTH = 24f
private const val GRIP_ARC_CAP_HEIGHT = 12f

/** Shared production visibility gate, exercised by Compose rendering tests. */
@Composable
internal fun GripArcAccessory(
    placement: GripArcPlacement?,
    profile: GripArcProfile,
    tokens: CartdeckEditionTokens,
    fullscreenWithGamepad: Boolean,
    isLoading: Boolean,
    onPressed: (Int, Boolean, Set<Int>) -> Unit,
    drawHousing: Boolean = true,
    lighting: CartdeckLighting = CartdeckLighting(),
    highContrast: Boolean = false,
    feedbackStrength: Float = 0f,
    inputLabels: Map<Int, String> = emptyMap(),
    typedActions: Boolean = false,
    latchedSlots: Set<Int> = emptySet(),
) {
    if (placement != null && !fullscreenWithGamepad && !isLoading) {
        GripArcControlGroup(
            placement,
            profile,
            tokens,
            onPressed,
            drawHousing,
            lighting,
            highContrast,
            feedbackStrength,
            inputLabels,
            typedActions,
            latchedSlots,
        )
    }
}

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
    lighting: CartdeckLighting = CartdeckLighting(),
    highContrast: Boolean = false,
    feedbackStrength: Float = 0f,
    inputLabels: Map<Int, String> = emptyMap(),
    typedActions: Boolean = false,
    latchedSlots: Set<Int> = emptySet(),
) {
    val scale = placement.scale
    Box(Modifier.fillMaxSize()) {
        if (drawHousing) {
            GripArcHousing(placement, tokens, lighting, highContrast, feedbackStrength, profile.count)
        }
        for (slot in placement.targets.keys.sorted().take(profile.count)) {
            val rect = placement.targets.getValue(slot)
            val mapping = profile.mappings.getOrNull(slot - 1).orEmpty().toSet()
            val label =
                if (typedActions) {
                    if (profile.supportedActionFor(slot, inputLabels.keys) == null) {
                        "Unavailable"
                    } else {
                        profile.labelFor(slot, inputLabels).ifEmpty { "—" }
                    }
                } else {
                    mapping.mapNotNull(inputLabels::get).joinToString("+").ifEmpty { "—" }
                }
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
                        .testTag("griparc-slot-$slot")
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Button,
                            onClickLabel = if (label == "—") "Unassigned button" else "Press $label",
                        ) { },
                contentAlignment = Alignment.Center,
            ) {
                GripArcShoulderButton(
                    modifier =
                        placement.visuals.getValue(slot).let { visual ->
                            Modifier.size(visual.w.dp, visual.h.dp)
                        },
                    label = label,
                    manufactured = typedActions,
                    tokens = tokens,
                    pressed = pressed || slot in latchedSlots,
                )
            }
        }
    }
}

@Composable
internal fun GripArcHousing(
    placement: GripArcPlacement,
    tokens: CartdeckEditionTokens,
    lighting: CartdeckLighting = CartdeckLighting(),
    highContrast: Boolean = false,
    feedbackStrength: Float = 0f,
    activeSlots: Int = 4,
) {
    val housing = placement.housing
    val left = placement.side == GripArcSide.LEFT
    val insideX = GripArcPositionSolver.HOUSING_ARM_INNER_X
    val insideY = GripArcPositionSolver.HOUSING_ARM_INNER_Y
    val outer =
        remember {
            Path().apply {
                moveTo(insideX + 8f, 4f)
                lineTo(148f, 4f)
                lineTo(156f, 12f)
                lineTo(156f, 148f)
                lineTo(148f, 156f)
                lineTo(12f, 156f)
                lineTo(4f, 148f)
                lineTo(4f, 124f)
                lineTo(12f, insideY)
                lineTo(insideX, insideY)
                lineTo(insideX, 12f)
                close()
            }
        }
    val inner =
        remember {
            Path().apply {
                moveTo(insideX + 10f, 8f)
                lineTo(146f, 10f)
                lineTo(150f, 16f)
                lineTo(150f, 145f)
                lineTo(145f, 150f)
                lineTo(14f, 150f)
                lineTo(10f, 145f)
                lineTo(10f, 127f)
                lineTo(15f, insideY + 3f)
                lineTo(insideX + 3f, insideY + 3f)
                lineTo(insideX + 3f, 16f)
                close()
            }
        }
    Box(
        modifier =
            Modifier
                .offset(x = housing.left.dp, y = housing.top.dp)
                .size(housing.width.dp, housing.height.dp)
                .testTag("griparc-housing"),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val u = size.width / 160f
            withTransform({
                scale(scaleX = u, scaleY = u, pivot = Offset.Zero)
                if (left) scale(scaleX = -1f, scaleY = 1f, pivot = Offset(80f, 80f))
            }) {
                drawControllerInset(outer, inner, Rect(4f, 4f, 156f, 156f), tokens, 2.2f)
                clipPath(inner) {
                    for (slot in 1..activeSlots) {
                        val target = placement.targets.getValue(slot)
                        val localX = (target.centerX - housing.left) / placement.scale
                        val centerX = if (left) 160f - localX else localX
                        val centerY = (target.centerY - housing.top) / placement.scale
                        val seat =
                            Offset(centerX - GRIP_ARC_POCKET_WIDTH / 2f, centerY - GRIP_ARC_POCKET_HEIGHT / 2f)
                        drawRoundRect(
                            color = tokens.bezel,
                            topLeft = seat,
                            size = androidx.compose.ui.geometry.Size(GRIP_ARC_POCKET_WIDTH, GRIP_ARC_POCKET_HEIGHT),
                            cornerRadius = CornerRadius(3f),
                        )
                    }
                }
                drawMaterialTrack(
                    Rect(151f, 22f, 155f, 144f),
                    tokens,
                    lighting,
                    CartdeckLighting.SIDE_RAILS,
                    highContrast,
                    feedbackStrength,
                )
                drawMaterialTrack(
                    Rect(22f, 151f, 149f, 155f),
                    tokens,
                    lighting,
                    CartdeckLighting.SIDE_RAILS,
                    highContrast,
                    feedbackStrength,
                )
                drawLine(
                    tokens.focus.copy(alpha = 0.45f),
                    Offset(106f, 15f),
                    Offset(144f, 15f),
                    strokeWidth = 1.3f,
                )
                drawLine(
                    tokens.onChassis.copy(alpha = 0.26f),
                    Offset(insideX + 3f, 24f),
                    Offset(insideX + 3f, 108f),
                    strokeWidth = 1.2f,
                )
                for (p in listOf(Offset(insideX + 6f, 14f), Offset(147f, 14f), Offset(14f, 144f), Offset(147f, 145f))) {
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
        val first = placement.targets.getValue(1)
        val second = placement.targets.getValue(2)
        val engravingTop = first.centerY + placement.visuals.getValue(1).h / 2f + 2f
        val engravingBottom = second.centerY - placement.visuals.getValue(2).h / 2f - 2f
        val engravingWidth = 36f * placement.scale
        val engravingCenter = (if (left) 32f else 128f) * placement.scale
        Box(
            modifier =
                Modifier
                    .offset(x = (engravingCenter - engravingWidth / 2f).dp, y = (engravingTop - housing.top).dp)
                    .width(engravingWidth.dp)
                    .height((engravingBottom - engravingTop).dp)
                    .clipToBounds()
                    .testTag("griparc-engraving"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "GripArc",
                color = tokens.accent,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
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
    manufactured: Boolean,
) {
    BoxWithConstraints(modifier = modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            if (manufactured) {
                val plate = Rect(1.dp.toPx(), 1.dp.toPx(), size.width - 1.dp.toPx(), size.height - 1.dp.toPx())
                val cap = plate.deflate((if (pressed) 5 else 3).dp.toPx())
                drawControllerInset(
                    chamferedIsland(plate, 6.dp.toPx()),
                    chamferedIsland(cap, 4.dp.toPx()),
                    plate,
                    tokens,
                    1.8.dp.toPx(),
                    if (pressed) tokens.accent.copy(alpha = 0.78f) else tokens.secondary,
                )
                drawPath(
                    chamferedIsland(cap, 4.dp.toPx()),
                    tokens.accent.copy(alpha = if (pressed) 0.92f else 0.43f),
                    style = Stroke(1.15.dp.toPx()),
                )
                return@Canvas
            }
            val r = 3.dp.toPx()
            drawRoundRect(
                color = tokens.bezel,
                cornerRadius = CornerRadius(r),
            )
            drawRoundRect(
                color = if (pressed) tokens.accent.copy(alpha = 0.62f) else tokens.secondary,
                topLeft = Offset(1.dp.toPx(), (if (pressed) 2 else 1).dp.toPx()),
                size =
                    androidx.compose.ui.geometry.Size(
                        size.width - 2.dp.toPx(),
                        size.height - 3.dp.toPx(),
                    ),
                cornerRadius = CornerRadius(r),
            )
            drawRoundRect(
                color = if (pressed) tokens.accent else tokens.onButton.copy(alpha = 0.36f),
                topLeft = Offset(1.dp.toPx(), (if (pressed) 2 else 1).dp.toPx()),
                size =
                    androidx.compose.ui.geometry.Size(
                        size.width - 2.dp.toPx(),
                        size.height - 3.dp.toPx(),
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
        val lettering = if (manufactured) (maxHeight.value * 0.28f).coerceIn(11f, 16f) else 9f
        Text(
            label,
            fontSize = lettering.sp,
            lineHeight = (lettering + if (manufactured) 1f else 0f).sp,
            color = tokens.onButton,
            maxLines = if (manufactured) 2 else 1,
        )
    }
}
