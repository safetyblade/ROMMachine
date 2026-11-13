package com.swordfish.lemuroid.app.shared.game

import com.swordfish.lemuroid.lib.library.SystemID

/** CARTDECK display-family policy. This class does NOT change the native renderer itself. */
internal enum class CartdeckScreenClass {
    HANDHELD,
    TABLET,
    TELEVISION,
}

internal enum class CartdeckScreenPresentation {
    FILL,
    NATIVE,
}

internal enum class CartdeckScreenEligibility {
    AVAILABLE,
    REQUIRES_PORTRAIT_DUAL_SCREEN,
}

internal object CartdeckScreenPolicy {
    const val TABLET_MIN_WIDTH_DP = 600
    const val TABLET_EXPANDED_WIDTH_DP = 840

    /**
     * Hardware TV takes precedence. An explicit TV-mode preference may opt a
     * tablet or external display into the same remote-driven experience.
     * Window width drives responsive UI; it must not accidentally turn a
     * touchscreen tablet into a television merely because it is large.
     */
    fun screenClass(
        isTvHardware: Boolean,
        explicitTvMode: Boolean,
        smallestWidthDp: Int,
    ): CartdeckScreenClass =
        when {
            isTvHardware || explicitTvMode -> CartdeckScreenClass.TELEVISION
            smallestWidthDp >= TABLET_MIN_WIDTH_DP -> CartdeckScreenClass.TABLET
            else -> CartdeckScreenClass.HANDHELD
        }

    /** A wide device may host a medium window; layouts adapt to available width. */
    fun isExpandedMenu(availableWidthDp: Int): Boolean = availableWidthDp >= TABLET_EXPANDED_WIDTH_DP

    fun defaultPresentation(
        screenClass: CartdeckScreenClass,
        landscape: Boolean,
    ): CartdeckScreenPresentation =
        if (screenClass == CartdeckScreenClass.TELEVISION || landscape) {
            CartdeckScreenPresentation.FILL
        } else {
            CartdeckScreenPresentation.NATIVE
        }

    /**
     * Dual-screen DS touch layouts are deliberately NOT adapted to TV or
     * landscape fullscreen. Portrait handheld/tablet DS remains supported.
     */
    fun eligibility(
        system: SystemID,
        screenClass: CartdeckScreenClass,
        landscape: Boolean,
    ): CartdeckScreenEligibility {
        val dualScreen =
            system == SystemID.NDS || system == SystemID.NINTENDO_3DS
        return if (dualScreen && (screenClass == CartdeckScreenClass.TELEVISION || landscape)) {
            CartdeckScreenEligibility.REQUIRES_PORTRAIT_DUAL_SCREEN
        } else {
            CartdeckScreenEligibility.AVAILABLE
        }
    }

    /**
     * TV never draws a touch overlay. On touch-only handheld/tablet landscape
     * the player still needs working on-screen inputs until a real controller
     * is present. This policy is independent of a core's input ID mapping.
     */
    fun allowTouchOverlay(
        screenClass: CartdeckScreenClass,
        landscape: Boolean,
        externalControllerConnected: Boolean,
    ): Boolean =
        when {
            screenClass == CartdeckScreenClass.TELEVISION -> false
            landscape && externalControllerConnected -> false
            else -> true
        }
}
