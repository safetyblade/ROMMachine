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
        val targetSlot = if (height > width && width in 360f..599f) 64f else 84f
        val adaptiveColumns = floor(available / (targetSlot * scale)).toInt().coerceAtLeast(1)
        val columns = if (height > width && width in 360f..599f) minOf(5, adaptiveColumns) else adaptiveColumns
        val slot = ((available - 4f * (columns - 1)) / columns).coerceAtLeast(1f)
        val decoration = 16f + 22f + if (favorites) 48f else 0f
        val heightLimit = (height - decoration).coerceAtLeast(1f) * 94f / 340f
        return Shelf(columns, minOf(slot, 72f, heightLimit).coerceAtLeast(1f))
    }

    fun detailArtWidth(
        width: Float,
        height: Float,
    ): Float = minOf(width.coerceAtLeast(0f), height.coerceAtLeast(0f) * 320f / 390f, 320f)
}
