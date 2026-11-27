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
    if (landscape) {
        StandaloneGripArcLandscapeMark(placement.dock.housing, tokens)
    } else {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
            val housing = placement.dock.housing
            val scale = androidx.compose.ui.platform.LocalDensity.current.density
            GbaGripArcFullHousing(
                Rect(housing.left * scale, housing.top * scale, housing.right * scale, housing.bottom * scale),
                tokens,
            )
        }
    }
    Canvas(Modifier.fillMaxSize()) {
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
