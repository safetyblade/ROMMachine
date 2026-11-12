package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
            drawPath(path, tokens.secondary, style = Stroke(PivotIslandShape.OUTLINE_WIDTH_DP.dp.toPx()))
            drawPath(path, tokens.accent.copy(alpha = 0.25f), style = Stroke(1.dp.toPx()))
        }
        GripArcHousing(placement.dock, tokens)
    }
}
