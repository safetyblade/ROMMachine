package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.ui.geometry.Rect

/**
 * Live root-space geometry for noninteractive hardware paint.
 * Never infer game pixels or controls from illustrative artwork coordinates.
 */
internal object CartdeckHardwareGeometry {
    fun inRoot(bounds: Rect?, root: Rect?): Rect? {
        if (bounds == null || root == null || root.width <= 0f || root.height <= 0f) return null
        return Rect(
            bounds.left - root.left,
            bounds.top - root.top,
            bounds.right - root.left,
            bounds.bottom - root.top,
        ).takeIf { it.width > 0f && it.height > 0f }
    }

    fun expanded(rect: Rect, distance: Float): Rect =
        Rect(rect.left - distance, rect.top - distance, rect.right + distance, rect.bottom + distance)

    fun intersects(a: Rect, b: Rect): Boolean =
        a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top

    /** Horizontal segments minus all touching measured controls and protected branding. */
    fun seamSegments(
        left: Float,
        right: Float,
        y: Float,
        exclusions: List<Rect>,
        clearance: Float,
    ): List<Pair<Float, Float>> {
        if (right <= left) return emptyList()
        var available = listOf(left to right)
        for (rect in exclusions) {
            val block = expanded(rect, clearance)
            if (y < block.top || y > block.bottom) continue
            available =
                available.flatMap { (start, end) ->
                    if (block.right <= start || block.left >= end) {
                        listOf(start to end)
                    } else {
                        listOf(
                            start to minOf(end, block.left),
                            maxOf(start, block.right) to end,
                        ).filter { (a, b) -> b - a >= 2f }
                    }
                }
        }
        return available
    }
}
