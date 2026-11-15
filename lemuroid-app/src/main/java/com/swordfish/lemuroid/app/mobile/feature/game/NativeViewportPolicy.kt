package com.swordfish.lemuroid.app.mobile.feature.game

import kotlin.math.min

/**
 * Native hosts own their renderer and guest pixel-aspect correction.
 * This is only the measured Android surface aperture, not a source-frame crop.
 */
internal object NativeViewportPolicy {
    internal enum class Host {
        XBOX,
        XBOX360,
        ARMSX2,
        CEMU,
    }

    // Conservative initial surface shapes. Each engine must continue to
    // letterbox/fit its own title-specific video within this Android surface.
    fun presentationAspect(host: Host): Float =
        when (host) {
            Host.XBOX, Host.ARMSX2 -> 4f / 3f
            Host.XBOX360, Host.CEMU -> 16f / 9f
        }

    fun portraitApertureRatio(host: Host): String =
        when (host) {
            Host.XBOX, Host.ARMSX2 -> CartdeckViewportPolicy.STANDARD_RATIO
            Host.XBOX360, Host.CEMU -> "16:9"
        }

    data class FittedRect(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
    ) {
        val width: Float get() = right - left
        val height: Float get() = bottom - top
    }

    /**
     * Fit the entire content inside a measured safe container, symmetrically.
     * Never stretch, upscale past available bounds, or crop for a target ratio.
     * One physical pixel of inward clearance prevents rounding onto bezel art.
     */
    fun fit(
        width: Float,
        height: Float,
        aspect: Float,
        edgeGuardPx: Float = 0f,
    ): FittedRect {
        if (!width.isFinite() || !height.isFinite() ||
            !aspect.isFinite() || !edgeGuardPx.isFinite() ||
            width <= 0f || height <= 0f || aspect <= 0f
        ) {
            return FittedRect(0f, 0f, 0f, 0f)
        }
        val guard = edgeGuardPx.coerceIn(0f, min(width, height) / 4f)
        val usableWidth = width - 2f * guard
        val usableHeight = height - 2f * guard
        val contentWidth = min(usableWidth, usableHeight * aspect)
        val contentHeight = contentWidth / aspect
        val left = (width - contentWidth) / 2f
        val top = (height - contentHeight) / 2f
        return FittedRect(left, top, left + contentWidth, top + contentHeight)
    }
}
