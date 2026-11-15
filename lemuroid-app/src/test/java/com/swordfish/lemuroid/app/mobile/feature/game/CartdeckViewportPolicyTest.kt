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

    @Test fun handheldAperturesUseActualConsolePixelsOnlyWhenTheyCanFit() {
        assertEquals("10:9", CartdeckViewportPolicy.portraitApertureRatio(SystemID.GB, 800f))
        assertEquals("10:9", CartdeckViewportPolicy.portraitApertureRatio(SystemID.GBC, 720f))
        assertEquals("4:3", CartdeckViewportPolicy.portraitApertureRatio(SystemID.GB, 640f))
        assertEquals("3:2", CartdeckViewportPolicy.portraitApertureRatio(SystemID.GBA, 640f))
        assertEquals("30:17", CartdeckViewportPolicy.portraitApertureRatio(SystemID.PSP, 640f))
        assertEquals("4:3", CartdeckViewportPolicy.portraitApertureRatio(SystemID.PSX, 800f))
        assertEquals("4:3", CartdeckViewportPolicy.portraitApertureRatio(SystemID.NDS, 800f))
        assertEquals("4:3", CartdeckViewportPolicy.portraitApertureRatio(SystemID.GB, Float.NaN))
    }

    @Test fun topPixelGuardShrinksViewportButNeverCropsSourcePixels() {
        val unguarded =
            CartdeckViewportPolicy.normalizedAperture(400f, 800f, 25f, 85f, 375f, 410f)
        val guarded =
            CartdeckViewportPolicy.normalizedAperture(400f, 800f, 25f, 85f, 375f, 410f, 1f)
        assertEquals(85f / 800f, unguarded.top, 0.000001f)
        assertEquals(86f / 800f, guarded.top, 0.000001f)
        assertEquals(26f / 400f, guarded.left, 0.000001f)
        assertEquals(374f / 400f, guarded.right, 0.000001f)
        assertEquals(409f / 800f, guarded.bottom, 0.000001f)
        assertTrue(guarded.left < guarded.right)
        assertTrue(guarded.top < guarded.bottom)
    }

    @Test fun normalizedViewportClampsToSafeRootWithoutInvalidDimensions() {
        val clipped = CartdeckViewportPolicy.normalizedAperture(300f, 400f, -30f, -20f, 350f, 450f, 1f)
        assertTrue(clipped.left >= 0f)
        assertTrue(clipped.top >= 0f)
        assertTrue(clipped.right <= 1f)
        assertTrue(clipped.bottom <= 1f)
        assertEquals(
            CartdeckViewportPolicy.NormalizedRect(0f, 0f, 1f, 1f),
            CartdeckViewportPolicy.normalizedAperture(0f, 400f, 1f, 1f, 2f, 2f),
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
