package com.swordfish.lemuroid.app.mobile.feature.game

import com.swordfish.lemuroid.lib.library.SystemID

/**
 * Standard manufactured 4:3 portrait glass, with the v2101 flexible opening
 * preserved for dual-screen content. The native renderer still contains its
 * actual game pixels within the allocated rectangle.
 */
object CartdeckViewportPolicy {
    const val STANDARD_RATIO = "4:3"

    enum class PortraitMode {
        STANDARD,
        DUAL_SCREEN,
    }

    fun portraitMode(system: SystemID): PortraitMode =
        when (system) {
            SystemID.NDS, SystemID.NINTENDO_3DS -> PortraitMode.DUAL_SCREEN
            else -> PortraitMode.STANDARD
        }

    data class FittedSize(val width: Float, val height: Float)

    /** Geometry contract for a standard aperture; never stretches game pixels. */
    fun fitStandard(
        width: Float,
        height: Float,
    ): FittedSize {
        if (!width.isFinite() || !height.isFinite() || width <= 0f || height <= 0f) {
            return FittedSize(0f, 0f)
        }
        val fittedWidth = minOf(width, height * (4f / 3f))
        return FittedSize(fittedWidth, fittedWidth * 3f / 4f)
    }
}
