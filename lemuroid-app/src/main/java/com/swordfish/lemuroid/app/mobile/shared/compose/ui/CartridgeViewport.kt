package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import kotlin.math.floor

/** Inputs are the measured content bounds after Scaffold/system insets, in dp. */
object CartridgeViewport {
    data class Shelf(val columns: Int, val spineWidth: Float)

    fun shelf(
        width: Float,
        height: Float,
        fontScale: Float,
        favorites: Boolean,
    ): Shelf {
        val available = (width - 24f).coerceAtLeast(1f)
        val scale = fontScale.coerceIn(1f, 2f)
        // Width, not the remaining height of an individual page, determines phone density.
        val compactWidth = width < 600f
        val decoration = 38f + if (favorites) 48f else 0f
        val heightLimit = (height - decoration).coerceAtLeast(1f) * 94f / 340f
        val targetSlot =
            if (compactWidth) {
                64f
            } else {
                (heightLimit + 4f).coerceIn(52f, 76f)
            }
        val adaptiveColumns = floor((available + 4f) / (targetSlot * scale)).toInt().coerceAtLeast(1)
        val columns = if (compactWidth) minOf(5, adaptiveColumns) else adaptiveColumns
        val slot = ((available - 4f * (columns - 1)) / columns).coerceAtLeast(1f)
        // A vertical shelf scrolls. Never collapse artwork to a few pixels when chrome is tall.
        return Shelf(columns, minOf(slot, 72f).coerceAtLeast(1f))
    }

    fun detailArtWidth(
        width: Float,
        height: Float,
    ): Float = minOf(width.coerceAtLeast(0f), height.coerceAtLeast(0f) * 320f / 390f, 320f)
}
