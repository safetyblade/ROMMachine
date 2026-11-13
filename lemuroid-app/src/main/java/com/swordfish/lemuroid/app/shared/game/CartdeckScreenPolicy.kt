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
     * A connected, enabled GAME controller hides visual touch controls for
     * every device class, including fullscreen portrait. Keep controls when
     * the user is touch-only; TV has no virtual controls even without a pad.
     * A TV remote is navigation, not an enabled game controller.
     * Live enabled-device state drives show/hide and hot-unplug recovery.
     */
    fun allowTouchOverlay(
        screenClass: CartdeckScreenClass,
        externalControllerConnected: Boolean,
    ): Boolean =
        screenClass != CartdeckScreenClass.TELEVISION && !externalControllerConnected
}
