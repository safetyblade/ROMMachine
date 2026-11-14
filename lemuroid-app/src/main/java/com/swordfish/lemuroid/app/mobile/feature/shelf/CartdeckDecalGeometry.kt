package com.swordfish.lemuroid.app.mobile.feature.shelf

import androidx.compose.ui.geometry.Rect
import kotlin.math.sqrt

/** Device-pixel geometry. A rotated decal is confined to its full square safety envelope. */
internal object CartdeckDecalGeometry {
    fun place(
        frame: Rect,
        exclusions: List<Rect>,
        decal: CartdeckPersonalDecal,
        baseSize: Float,
        clearance: Float,
    ): Rect? {
        if (frame.width <= 0f || frame.height <= 0f || baseSize <= 0f) return null
        val visibleSide = minOf(baseSize * decal.scale, frame.width * 0.25f, frame.height * 0.20f)
        val envelopeSide = visibleSide * sqrt(2f)
        if (visibleSide < 12f || envelopeSide + clearance * 2f > frame.width) return null
        val inset = maxOf(clearance, 5f)
        val freeX = frame.width - envelopeSide - inset * 2f
        val freeY = frame.height - envelopeSide - inset * 2f
        if (freeX < 0f || freeY < 0f) return null
        val anchors =
            linkedMapOf(
                CartdeckPersonalDecal.ANCHOR_UPPER_RAIL to (0.5f to 0f),
                CartdeckPersonalDecal.ANCHOR_LOWER_LEFT to (0f to 1f),
                CartdeckPersonalDecal.ANCHOR_LOWER_RIGHT to (1f to 1f),
                CartdeckPersonalDecal.ANCHOR_SHELF_RAIL to (0.5f to 1f),
            )
        val preferred = anchors.getValue(decal.anchor)
        val positions =
            listOf(preferred) +
                anchors.values.filterNot { it == preferred } +
                listOf(0f to 0.5f, 1f to 0.5f, 0.5f to 0f)
        for ((nx, ny) in positions) {
            val left = frame.left + inset + freeX * nx
            val top = frame.top + inset + freeY * ny
            val seat = Rect(left, top, left + envelopeSide, top + envelopeSide)
            val forbidden =
                exclusions.any {
                    seat.left < it.right + clearance && seat.right > it.left - clearance &&
                        seat.top < it.bottom + clearance && seat.bottom > it.top - clearance
                }
            if (!forbidden) return seat
        }
        return null
    }
}
