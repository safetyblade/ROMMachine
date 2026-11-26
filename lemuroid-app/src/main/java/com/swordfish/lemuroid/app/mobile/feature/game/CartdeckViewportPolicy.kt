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

    /**
     * Physical pixel geometry for the manufactured portrait screen aperture.
     * Source game pixels are STILL fitted by the current Libretro renderer;
     * these ratios never instruct that renderer to stretch or crop a frame.
     * Narrow handsets retain 4:3 GB glass when the tall native aperture would
     * compete with the original touch control deck.
     */
    fun portraitApertureRatio(
        system: SystemID,
        availableHeightDp: Float,
        compactSoloPivot: Boolean = false,
    ): String =
        when (system) {
            SystemID.GB, SystemID.GBC ->
                if (!compactSoloPivot && availableHeightDp.isFinite() && availableHeightDp >= 720f) {
                    "10:9"
                } else {
                    STANDARD_RATIO
                }
            SystemID.GBA -> "3:2"
            SystemID.PSP -> "30:17"
            else -> STANDARD_RATIO
        }

    data class NormalizedRect(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
    )

    /**
     * The shell's measured pixel rectangle is the source of truth. A ONE
     * physical-pixel guard fits the whole video into the glass opening rather
     * than clipping a source row; it prevents fractional viewport edges from
     * painting beyond the bezel. Dual-screen input keeps a zero guard.
     */
    fun normalizedAperture(
        parentWidth: Float,
        parentHeight: Float,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        edgeGuardPx: Float = 0f,
    ): NormalizedRect {
        val values = listOf(parentWidth, parentHeight, left, top, right, bottom, edgeGuardPx)
        if (values.any { !it.isFinite() } || parentWidth <= 0f || parentHeight <= 0f ||
            right <= left || bottom <= top
        ) {
            return NormalizedRect(0f, 0f, 1f, 1f)
        }
        val availableGuard = minOf((right - left) / 4f, (bottom - top) / 4f)
        val guard = edgeGuardPx.coerceIn(0f, availableGuard)
        val x1 = ((left + guard) / parentWidth).coerceIn(0f, 1f)
        val y1 = ((top + guard) / parentHeight).coerceIn(0f, 1f)
        val x2 = ((right - guard) / parentWidth).coerceIn(x1, 1f)
        val y2 = ((bottom - guard) / parentHeight).coerceIn(y1, 1f)
        return NormalizedRect(x1, y1, x2, y2)
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
