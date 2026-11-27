package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckEditionTokens

/**
 * Standalone GBA GripArc: one manufactured controller body rather than a
 * separate L-shaped tower. Geometry and artwork are deliberately independent
 * of the PivotArc + GripArc joint, which is the last implementation stage.
 */
internal object GbaGripArcFullBodyGeometry {
    fun resolve(
        root: Rect,
        video: Rect,
        targets: Collection<PivotArcBounds>,
    ): Rect? {
        if (root.width <= 0f || root.height <= 0f || video.bottom >= root.bottom) return null
        val panel =
            Rect(
                root.left + 12f,
                video.bottom + 16f,
                root.right - 12f,
                root.bottom - 16f,
            )
        if (panel.width < 260f || panel.height < 180f || targets.size != 4) return null
        if (targets.any {
                it.left < panel.left || it.right > panel.right ||
                    it.top < panel.top || it.bottom > panel.bottom
            }
        ) {
            return null
        }
        return panel
    }
}

@Composable
internal fun GbaGripArcFullHousing(
    panel: Rect,
    tokens: CartdeckEditionTokens,
) {
    // No pointer handlers: native PadKit owns gameplay; G1-G4 retain their
    // saved bindings, hit rectangles, hold semantics and simultaneous presses.
    val density = LocalDensity.current
    Box(Modifier.fillMaxSize().testTag("gba-griparc-full-hardware")) {
        Canvas(Modifier.fillMaxSize()) {
            val corner = 17.dp.toPx()
            val lip = 7.dp.toPx()
            val undercut = chamferedIsland(panel, corner)
            drawPath(undercut, tokens.bezel.copy(alpha = 0.64f), style = Stroke(6.dp.toPx()))
            drawControllerInset(
                rim = chamferedIsland(panel, corner),
                inset = chamferedIsland(panel.deflate(lip), corner / 2f),
                bounds = panel,
                tokens = tokens,
                edge = 2.dp.toPx(),
                housing = lerp(tokens.chassis, tokens.secondary, 0.24f),
            )
            val bevel = panel.deflate(3.dp.toPx())
            drawPath(
                chamferedIsland(bevel, corner - 2.dp.toPx()),
                lerp(tokens.secondary, tokens.onChassis, 0.29f),
                style = Stroke(1.4.dp.toPx()),
            )
            drawPath(
                chamferedIsland(panel.deflate(lip + 3.dp.toPx()), corner / 2f),
                tokens.bezel.copy(alpha = 0.48f),
                style = Stroke(1.2.dp.toPx()),
            )
            // Same machined rail and fastener language as accepted PivotArc;
            // no opaque plate, overlay or hit target crosses the video aperture.
            val left = panel.left + 22.dp.toPx()
            val right = panel.right - 22.dp.toPx()
            for (y in listOf(panel.top + 9.dp.toPx(), panel.bottom - 9.dp.toPx())) {
                drawLine(
                    color = tokens.accent.copy(alpha = 0.64f),
                    start = Offset(left, y),
                    end = Offset(right, y),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            val sx = 12.dp.toPx()
            val sy = 17.dp.toPx()
            val fasteners =
                listOf(
                    Offset(panel.left + sx, panel.top + sy),
                    Offset(panel.right - sx, panel.top + sy),
                    Offset(panel.left + sx, panel.bottom - sy),
                    Offset(panel.right - sx, panel.bottom - sy),
                )
            for (p in fasteners) {
                drawCircle(tokens.bezel, 3.dp.toPx(), p)
                drawCircle(tokens.onChassis.copy(alpha = 0.45f), 3.dp.toPx(), p, style = Stroke(1.dp.toPx()))
            }
        }
        // GBA-specific full-chassis engraving, not another floating tower.
        // The wordmark is decorative only and cannot become an input owner.
        val engravingX = with(density) { (panel.center.x - 55.dp.toPx()).toDp() }
        val engravingY = with(density) { (panel.top + 19.dp.toPx()).toDp() }
        Text(
            text = "GripArc",
            color = tokens.accent,
            fontSize = 13.sp,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier =
                Modifier
                    .offset(x = engravingX, y = engravingY)
                    .width(110.dp)
                    .testTag("gba-griparc-full-wordmark"),
        )
    }
}

@Composable
internal fun StandaloneGripArcLandscapeMark(
    panel: PivotArcBounds,
    tokens: CartdeckEditionTokens,
) {
    Text(
        text = "GripARC",
        color = tokens.accent,
        fontSize = 11.sp,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.End,
        maxLines = 1,
        modifier = Modifier.offset((panel.right - 96f).dp, (panel.bottom - 17f).dp).width(88.dp),
    )
}
