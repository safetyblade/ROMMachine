package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckEditionTokens

/** Pointer-transparent joint material. Both contours come from the input placement result. */
@Composable
internal fun CompositeControlDeckHousing(
    placement: CompositeControlDeckPlacement,
    tokens: CartdeckEditionTokens,
) {
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val upper = placement.island.upper
            val lower = placement.island.lower
            val path =
                Path().apply {
                    moveTo(upper.left.dp.toPx(), upper.top.dp.toPx())
                    lineTo(upper.right.dp.toPx(), upper.top.dp.toPx())
                    lineTo(upper.right.dp.toPx(), upper.bottom.dp.toPx())
                    lineTo(lower.right.dp.toPx(), lower.top.dp.toPx())
                    lineTo(lower.right.dp.toPx(), lower.bottom.dp.toPx())
                    lineTo(lower.left.dp.toPx(), lower.bottom.dp.toPx())
                    lineTo(lower.left.dp.toPx(), lower.top.dp.toPx())
                    lineTo(upper.left.dp.toPx(), upper.bottom.dp.toPx())
                    close()
                }
            drawPath(path, tokens.chassis)
            clipPath(path) {
                // Interior bevel and restrained machined reflection stay inside the validated contour.
                drawPath(path, tokens.bezel, style = Stroke(10.dp.toPx()))
                drawPath(path, tokens.secondary.copy(alpha = 0.7f), style = Stroke(6.dp.toPx()))
                drawLine(
                    tokens.onChassis.copy(alpha = 0.08f),
                    Offset(upper.left.dp.toPx(), upper.top.dp.toPx()),
                    Offset(upper.right.dp.toPx(), upper.bottom.dp.toPx()),
                    strokeWidth = 12.dp.toPx(),
                )
            }
            drawPath(path, tokens.secondary, style = Stroke(PivotIslandShape.OUTLINE_WIDTH_DP.dp.toPx()))
            drawPath(path, tokens.accent.copy(alpha = 0.25f), style = Stroke(1.dp.toPx()))
        }
        placement.wordmark?.let { brand ->
            PivotArcWordmark(
                Modifier.offset(brand.left.dp, brand.top.dp).size(brand.width.dp, brand.height.dp),
                ink = tokens.onChassis,
                accent = tokens.accent,
                compact = true,
            )
        }
        GripArcHousing(placement.dock, tokens)
    }
}
