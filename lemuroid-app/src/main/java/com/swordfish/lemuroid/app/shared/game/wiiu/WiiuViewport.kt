package com.swordfish.lemuroid.app.shared.game.wiiu

import kotlin.math.min

/** Same aspect-fit transform used by the Vulkan blit; bars never become GamePad touches. */
object WiiuViewport {
    data class Pointer(val x: Int, val y: Int, val inside: Boolean)

    fun pointer(
        x: Float,
        y: Float,
        width: Int,
        height: Int,
        aspect: Float,
    ): Pointer {
        if (!x.isFinite() || !y.isFinite() || width <= 0 || height <= 0 || !aspect.isFinite() || aspect <= 0f) {
            return Pointer(0, 0, false)
        }
        val imageWidth = min(width.toFloat(), height * aspect)
        val imageHeight = imageWidth / aspect
        val left = (width - imageWidth) / 2f
        val top = (height - imageHeight) / 2f
        val nx = (x - left) / imageWidth
        val ny = (y - top) / imageHeight
        return Pointer(
            (nx.coerceIn(0f, 1f) * 65534 - 32767).toInt(),
            (ny.coerceIn(0f, 1f) * 65534 - 32767).toInt(),
            nx in 0f..1f && ny in 0f..1f,
        )
    }
}
