package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckEditionTokens
import com.swordfish.touchinput.radial.settings.NativeControlKind

/** Portrait mounts a full body. Landscape machines the wells directly into the existing shell. */
@Composable
internal fun StandaloneGripArcHardware(
    placement: GripArcCombinedPlacement,
    tokens: CartdeckEditionTokens,
    landscape: Boolean,
) {
    val panel = placement.dock.housing
    val occupied = placement.controls.values + placement.dock.targets.values
    val rail =
        if (landscape) {
            val centralUtilities =
                placement.native.controls.filter { it.kind == NativeControlKind.UTILITY }
                    .map { placement.controls.getValue(it.key) }
                    .filter { target ->
                        target.top >= panel.bottom - 50f &&
                            kotlin.math.abs(target.centerX - panel.centerX) < panel.width / 4f
                    }
            centralUtilities.takeIf { it.isNotEmpty() }?.let { seats ->
                PivotArcBounds(
                    (seats.minOf { it.left } - 6f).coerceAtLeast(panel.left),
                    seats.minOf { it.top },
                    (seats.maxOf { it.right } + 6f).coerceAtMost(panel.right),
                    seats.maxOf { it.bottom },
                )
            }
        } else {
            null
        }
    if (landscape) {
        StandaloneGripArcLandscapeMark(
            panel,
            occupied + listOfNotNull(rail),
            tokens,
        )
    } else {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
            val scale = androidx.compose.ui.platform.LocalDensity.current.density
            val markWidth = 62f
            val x = panel.centerX - markWidth / 2f
            val engraving =
                generateSequence(panel.top + 12f) { it + 4f }
                    .takeWhile { it + 18f <= panel.bottom - 12f }
                    .map { y -> PivotArcBounds(x, y, x + markWidth, y + 18f) }
                    .firstOrNull { candidate -> occupied.none { it.inset(3f).intersects(candidate) } }
            GbaGripArcFullHousing(
                Rect(panel.left * scale, panel.top * scale, panel.right * scale, panel.bottom * scale),
                tokens,
                engraving = engraving,
                standalone = true,
            )
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        if (rail != null) {
            val bounds = Rect(rail.left.dp.toPx(), rail.top.dp.toPx(), rail.right.dp.toPx(), rail.bottom.dp.toPx())
            drawControllerInset(
                chamferedIsland(bounds, 8.dp.toPx()),
                chamferedIsland(bounds.deflate(3.dp.toPx()), 5.dp.toPx()),
                bounds,
                tokens,
                1.5.dp.toPx(),
            )
        }

        fun well(
            target: PivotArcBounds,
            size: PivotArcSize,
            round: Boolean,
            trim: Color,
        ) {
            val center = Offset(target.centerX.dp.toPx(), target.centerY.dp.toPx())
            val x = minOf(target.width / 2f, size.w / 2f + 2f).dp.toPx()
            val y = minOf(target.height / 2f, size.h / 2f + 2f).dp.toPx()
            val edge = 1.7.dp.toPx()
            if (round) {
                drawCircle(tokens.bezel, x, center)
                drawCircle(trim.copy(alpha = 0.65f), x - edge / 2f, center, style = Stroke(edge))
                drawCircle(tokens.onChassis.copy(alpha = 0.27f), x - edge * 2f, center, style = Stroke(edge / 2f))
            } else {
                val rim = Rect(center.x - x, center.y - y, center.x + x, center.y + y)
                drawControllerInset(
                    chamferedIsland(rim, 6.dp.toPx()),
                    chamferedIsland(rim.deflate(edge * 1.4f), 4.dp.toPx()),
                    rim,
                    tokens,
                    edge,
                )
                drawPath(
                    chamferedIsland(rim.deflate(edge), 5.dp.toPx()),
                    trim.copy(alpha = 0.51f),
                    style = Stroke(edge),
                )
            }
        }
        for (control in placement.native.controls) {
            val round = control.kind in setOf(NativeControlKind.FACE, NativeControlKind.DPAD, NativeControlKind.ANALOG)
            well(
                placement.controls.getValue(control.key),
                placement.visuals.getValue(control.key),
                round,
                lerp(tokens.secondary, tokens.onChassis, 0.36f),
            )
        }
        for ((slot, target) in placement.dock.targets) {
            well(target, placement.dock.visuals.getValue(slot), false, tokens.accent)
        }
    }
}
