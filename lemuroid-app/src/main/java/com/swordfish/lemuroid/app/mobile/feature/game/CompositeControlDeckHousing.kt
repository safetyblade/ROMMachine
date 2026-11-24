package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.mobile.feature.shelf.CartdeckLighting
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckEditionTokens
import kotlin.math.abs

/**
 * Non-interactive, open-body material. The solver island is a *clearance*
 * envelope, never an opaque faceplate. Existing individual CARTDECK hardware
 * seats remain visible through its centre, with a short seam to the L foot.
 */
internal data class CompositeMaterialSeam(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
)

internal object CompositeOpenHousingPolicy {
    fun seams(placement: CompositeControlDeckPlacement): List<CompositeMaterialSeam> {
        val upper = placement.island.upper
        val lower = placement.island.lower
        val seams = mutableListOf<CompositeMaterialSeam>()

        // Restrained top and bottom machined lips, not the filled island hull.
        if (upper.width > 32f) {
            seams += CompositeMaterialSeam(upper.left + 12f, upper.top + 3f, upper.right - 12f, upper.top + 3f)
        }
        if (lower.width > 32f) {
            seams += CompositeMaterialSeam(lower.left + 12f, lower.bottom - 3f, lower.right - 12f, lower.bottom - 3f)
        }

        // Joined foot is a short, non-interactive seam, never an artificial
        // full-width bridge across otherwise empty shell or game pixels.
        val foot = placement.dockFootprint.getOrNull(1)
        if (foot != null) {
            val rightSide = placement.dock.side == GripArcSide.RIGHT
            val x1 = if (rightSide) lower.right else lower.left
            val x2 = if (rightSide) foot.left else foot.right
            val y1 = lower.bottom - 8f
            val y2 = foot.top + 8f
            if (abs(x2 - x1) <= 64f && abs(y2 - y1) <= 64f) {
                seams += CompositeMaterialSeam(x1, y1, x2, y2)
            }
        }
        return seams.filter { seam ->
            listOf(seam.startX, seam.endX).all { it >= placement.safeDeck.left && it <= placement.safeDeck.right } &&
                listOf(seam.startY, seam.endY).all {
                    it >= placement.safeDeck.top && it <= placement.safeDeck.bottom &&
                        it > placement.viewport.bottom
                }
        }
    }
}

/** Existing control hit areas and the GripArc V2.2 footprint are unchanged. */
@Composable
internal fun CompositeControlDeckHousing(
    placement: CompositeControlDeckPlacement,
    tokens: CartdeckEditionTokens,
    lighting: CartdeckLighting = CartdeckLighting(),
    highContrast: Boolean = false,
    feedbackStrength: Float = 0f,
) {
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            for (seam in CompositeOpenHousingPolicy.seams(placement)) {
                val from = Offset(seam.startX.dp.toPx(), seam.startY.dp.toPx())
                val to = Offset(seam.endX.dp.toPx(), seam.endY.dp.toPx())
                drawDockSeam(from, to, tokens)
            }
        }
        placement.wordmark?.let { brand ->
            PivotArcWordmark(
                Modifier.offset(brand.left.dp, brand.top.dp).size(brand.width.dp, brand.height.dp),
                ink = tokens.onChassis,
                accent = tokens.accent,
                compact = true,
            )
        }
        GripArcHousing(placement.dock, tokens, lighting, highContrast, feedbackStrength)
    }
}

private fun DrawScope.drawDockSeam(
    from: Offset,
    to: Offset,
    tokens: CartdeckEditionTokens,
) {
    drawLine(tokens.bezel, from, to, strokeWidth = 5.dp.toPx())
    drawLine(tokens.secondary, from, to, strokeWidth = 3.dp.toPx())
    drawLine(tokens.onChassis.copy(alpha = 0.38f), from, to, strokeWidth = 0.75.dp.toPx())
}

/** Tie an adjacent native seat to the dock along a shared horizontal band. */
@Composable
internal fun StandardGripArcDockJoint(
    placement: GripArcCombinedPlacement,
    tokens: CartdeckEditionTokens,
) {
    val dock = placement.dock
    val right = dock.side == GripArcSide.RIGHT
    val arms = GripArcPositionSolver.housingFootprint(dock.housing, dock.scale, dock.side)
    val joints =
        arms.mapNotNull { arm ->
            placement.seats.values.mapNotNull seatCandidate@{ seat ->
                val fromX = if (right) seat.right else seat.left
                val toX = if (right) arm.left else arm.right
                val gap = if (right) toX - fromX else fromX - toX
                val top = maxOf(arm.top, seat.top) + 8f
                val bottom = minOf(arm.bottom, seat.bottom) - 8f
                if (gap !in 0f..28f || top >= bottom) return@seatCandidate null
                val y = (top + bottom) / 2f
                val bridge = PivotArcBounds(minOf(fromX, toX), y - 3f, maxOf(fromX, toX), y + 3f)
                if (placement.controls.values.any { it != seat && it.intersects(bridge) }) return@seatCandidate null
                CompositeMaterialSeam(fromX, y, toX, y)
            }.minByOrNull { abs(it.endX - it.startX) }
        }
    Canvas(Modifier.fillMaxSize()) {
        for (joint in joints) {
            drawDockSeam(
                Offset(joint.startX.dp.toPx(), joint.startY.dp.toPx()),
                Offset(joint.endX.dp.toPx(), joint.endY.dp.toPx()),
                tokens,
            )
        }
    }
}
