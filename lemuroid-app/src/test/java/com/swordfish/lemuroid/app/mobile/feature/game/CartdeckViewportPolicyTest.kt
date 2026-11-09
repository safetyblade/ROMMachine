package com.swordfish.lemuroid.app.mobile.feature.game

import com.swordfish.lemuroid.lib.library.SystemID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartdeckViewportPolicyTest {
    @Test fun conventionalSystemsUseOneFourThreeOpening() {
        val systems =
            listOf(
                SystemID.GB,
                SystemID.GBC,
                SystemID.GBA,
                SystemID.PSX,
                SystemID.N64,
                SystemID.PSP,
                SystemID.NES,
                SystemID.SNES,
                SystemID.GENESIS,
            )
        for (system in systems) {
            assertEquals(
                system.name,
                CartdeckViewportPolicy.PortraitMode.STANDARD,
                CartdeckViewportPolicy.portraitMode(system),
            )
        }
        assertEquals("4:3", CartdeckViewportPolicy.STANDARD_RATIO)
    }

    @Test fun dualScreenSystemsRetainTheWorkingFlexibleAllocation() {
        for (system in listOf(SystemID.NDS, SystemID.NINTENDO_3DS)) {
            assertEquals(
                CartdeckViewportPolicy.PortraitMode.DUAL_SCREEN,
                CartdeckViewportPolicy.portraitMode(system),
            )
        }
    }

    @Test fun standardGlassFitsBothAspectDirections() {
        val widthLimited = CartdeckViewportPolicy.fitStandard(340f, 500f)
        assertEquals(340f, widthLimited.width, 0.01f)
        assertEquals(255f, widthLimited.height, 0.01f)

        val heightLimited = CartdeckViewportPolicy.fitStandard(400f, 210f)
        assertEquals(280f, heightLimited.width, 0.01f)
        assertEquals(210f, heightLimited.height, 0.01f)
        assertTrue(heightLimited.width <= 400f)
    }

    @Test fun invalidSpaceProducesNoSize() {
        for (width in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertEquals(
                CartdeckViewportPolicy.FittedSize(0f, 0f),
                CartdeckViewportPolicy.fitStandard(width, 300f),
            )
        }
        assertEquals(
            CartdeckViewportPolicy.FittedSize(0f, 0f),
            CartdeckViewportPolicy.fitStandard(300f, 0f),
        )
    }

    @Test fun everyConfiguredSystemHasASafeMode() {
        for (system in SystemID.entries) {
            assertTrue(
                CartdeckViewportPolicy.portraitMode(system) in
                    CartdeckViewportPolicy.PortraitMode.entries,
            )
        }
    }
}
