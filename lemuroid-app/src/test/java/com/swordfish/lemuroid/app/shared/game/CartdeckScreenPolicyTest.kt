package com.swordfish.lemuroid.app.shared.game

import com.swordfish.lemuroid.lib.library.SystemID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CartdeckScreenPolicyTest {
    @Test
    fun hardwareAndManualTvModeDoNotDependOnDpWidth() {
        assertEquals(
            CartdeckScreenClass.TELEVISION,
            CartdeckScreenPolicy.screenClass(isTvHardware = true, explicitTvMode = false, smallestWidthDp = 320),
        )
        assertEquals(
            CartdeckScreenClass.TELEVISION,
            CartdeckScreenPolicy.screenClass(isTvHardware = false, explicitTvMode = true, smallestWidthDp = 600),
        )
        assertEquals(
            CartdeckScreenClass.TABLET,
            CartdeckScreenPolicy.screenClass(isTvHardware = false, explicitTvMode = false, smallestWidthDp = 600),
        )
        assertEquals(
            CartdeckScreenClass.HANDHELD,
            CartdeckScreenPolicy.screenClass(isTvHardware = false, explicitTvMode = false, smallestWidthDp = 599),
        )
    }

    @Test
    fun expandedMenuFollowsAvailableWindowNotHardwareClass() {
        assertFalse(CartdeckScreenPolicy.isExpandedMenu(839))
        assertTrue(CartdeckScreenPolicy.isExpandedMenu(840))
    }

    @Test
    fun landscapeAndTvDefaultToFillWhilePortraitHandheldDefaultsNative() {
        assertEquals(
            CartdeckScreenPresentation.FILL,
            CartdeckScreenPolicy.defaultPresentation(CartdeckScreenClass.TELEVISION, landscape = false),
        )
        assertEquals(
            CartdeckScreenPresentation.FILL,
            CartdeckScreenPolicy.defaultPresentation(CartdeckScreenClass.TABLET, landscape = true),
        )
        assertEquals(
            CartdeckScreenPresentation.NATIVE,
            CartdeckScreenPolicy.defaultPresentation(CartdeckScreenClass.HANDHELD, landscape = false),
        )
    }

    @Test
    fun dsIsUnavailableOnlyInTvOrLandscapeWhilePortraitRemainsPlayable() {
        val dual = listOf(SystemID.NDS, SystemID.NINTENDO_3DS)
        for (system in dual) {
            assertEquals(
                CartdeckScreenEligibility.REQUIRES_PORTRAIT_DUAL_SCREEN,
                CartdeckScreenPolicy.eligibility(system, CartdeckScreenClass.TELEVISION, landscape = false),
            )
            assertEquals(
                CartdeckScreenEligibility.REQUIRES_PORTRAIT_DUAL_SCREEN,
                CartdeckScreenPolicy.eligibility(system, CartdeckScreenClass.TABLET, landscape = true),
            )
            assertEquals(
                CartdeckScreenEligibility.AVAILABLE,
                CartdeckScreenPolicy.eligibility(system, CartdeckScreenClass.TABLET, landscape = false),
            )
            assertEquals(
                CartdeckScreenEligibility.AVAILABLE,
                CartdeckScreenPolicy.eligibility(system, CartdeckScreenClass.HANDHELD, landscape = false),
            )
        }
        assertEquals(
            CartdeckScreenEligibility.AVAILABLE,
            CartdeckScreenPolicy.eligibility(SystemID.GAMECUBE, CartdeckScreenClass.TELEVISION, landscape = true),
        )
    }

    @Test
    fun controllerHidesTouchButtonsForEveryScreenClassAndDisconnectRestoresTouch() {
        for (screen in listOf(CartdeckScreenClass.HANDHELD, CartdeckScreenClass.TABLET)) {
            assertTrue(
                CartdeckScreenPolicy.allowTouchOverlay(
                    screenClass = screen,
                    externalControllerConnected = false,
                ),
            )
            assertFalse(
                CartdeckScreenPolicy.allowTouchOverlay(
                    screenClass = screen,
                    externalControllerConnected = true,
                ),
            )
            // The device state updates when an enabled controller disconnects.
            assertTrue(
                CartdeckScreenPolicy.allowTouchOverlay(
                    screenClass = screen,
                    externalControllerConnected = false,
                ),
            )
        }
    }

    @Test
    fun televisionNeverDrawsTouchControlsEvenWhenPadIsUnplugged() {
        assertFalse(
            CartdeckScreenPolicy.allowTouchOverlay(
                CartdeckScreenClass.TELEVISION,
                externalControllerConnected = false,
            ),
        )
        assertFalse(
            CartdeckScreenPolicy.allowTouchOverlay(
                CartdeckScreenClass.TELEVISION,
                externalControllerConnected = true,
            ),
        )
    }
}
