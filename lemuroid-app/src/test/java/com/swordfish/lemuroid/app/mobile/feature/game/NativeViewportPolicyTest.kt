package com.swordfish.lemuroid.app.mobile.feature.game

import com.swordfish.lemuroid.app.shared.game.wiiu.WiiuViewport
import com.swordfish.lemuroid.lib.library.SystemID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeViewportPolicyTest {
    @Test
    fun allFourHostsHaveConservativeAspectProfiles() {
        assertEquals(4f / 3f, NativeViewportPolicy.presentationAspect(NativeViewportPolicy.Host.XBOX), 0.001f)
        assertEquals(16f / 9f, NativeViewportPolicy.presentationAspect(NativeViewportPolicy.Host.XBOX360), 0.001f)
        assertEquals(4f / 3f, NativeViewportPolicy.presentationAspect(NativeViewportPolicy.Host.ARMSX2), 0.001f)
        assertEquals(16f / 9f, NativeViewportPolicy.presentationAspect(NativeViewportPolicy.Host.CEMU), 0.001f)
        assertEquals("4:3", NativeViewportPolicy.portraitApertureRatio(NativeViewportPolicy.Host.XBOX))
        assertEquals("4:3", NativeViewportPolicy.portraitApertureRatio(NativeViewportPolicy.Host.ARMSX2))
        assertEquals("16:9", NativeViewportPolicy.portraitApertureRatio(NativeViewportPolicy.Host.XBOX360))
        assertEquals("16:9", NativeViewportPolicy.portraitApertureRatio(NativeViewportPolicy.Host.CEMU))
    }

    @Test
    fun xboxPortraitFitsInsideMeasuredGlassWithoutStretching() {
        val rect = NativeViewportPolicy.fit(360f, 600f, 4f / 3f, 1f)
        assertEquals(358f, rect.width, 0.001f)
        assertEquals(268.5f, rect.height, 0.01f)
        assertEquals(1f, rect.left, 0.001f)
        assertTrue(rect.top > 1f)
        assertEquals(4f / 3f, rect.width / rect.height, 0.001f)
    }

    @Test
    fun nativeLandscapeUsesFullSafeWidthWithoutSourceCropping() {
        val rect = NativeViewportPolicy.fit(1600f, 900f, 16f / 9f, 1f)
        assertEquals(1596.44f, rect.width, 0.1f)
        assertEquals(898f, rect.height, 0.01f)
        assertEquals(16f / 9f, rect.width / rect.height, 0.001f)
        assertTrue(rect.left >= 1f)
        assertTrue(rect.right <= 1599f)
    }

    @Test
    fun tallPhoneAndTvSafeAreasRemainInsideTheWindow() {
        val tall = NativeViewportPolicy.fit(393f, 852f, 16f / 9f, 1f)
        assertEquals(391f, tall.width, 0.01f)
        assertTrue(tall.top > 300f)
        // The TV overlay has already deducted its 32dp per-side overscan
        // and safeDrawing insets before presenting this measured inner rect.
        val tv = NativeViewportPolicy.fit(1856f, 1016f, 16f / 9f, 1f)
        assertTrue(tv.width <= 1856f)
        assertTrue(tv.height <= 1016f)
        assertTrue(tv.left >= 0f && tv.top >= 0f)
    }

    @Test
    fun invalidDimensionsNeverProduceANativeSurface() {
        val zero = NativeViewportPolicy.FittedRect(0f, 0f, 0f, 0f)
        assertEquals(zero, NativeViewportPolicy.fit(0f, 1080f, 16f / 9f))
        assertEquals(zero, NativeViewportPolicy.fit(Float.NaN, 1080f, 16f / 9f))
        assertEquals(zero, NativeViewportPolicy.fit(1920f, Float.POSITIVE_INFINITY, 16f / 9f))
        assertEquals(zero, NativeViewportPolicy.fit(1920f, 1080f, -1f))
        assertEquals(zero, NativeViewportPolicy.fit(1920f, 1080f, 16f / 9f, Float.NaN))
    }

    @Test
    fun dsLayoutAndWiiuTouchCoordinateContractArePreserved() {
        assertEquals(
            CartdeckViewportPolicy.PortraitMode.DUAL_SCREEN,
            CartdeckViewportPolicy.portraitMode(SystemID.NDS),
        )
        assertEquals(
            CartdeckViewportPolicy.PortraitMode.DUAL_SCREEN,
            CartdeckViewportPolicy.portraitMode(SystemID.NINTENDO_3DS),
        )
        val center = WiiuViewport.pointer(960f, 540f, 1920, 1080, 16f / 9f)
        assertTrue(center.inside)
        assertTrue(center.x in -1..1 && center.y in -1..1)
        // 4:3 games in a 16:9 Cemu surface still reject touches in the bars.
        val outside = WiiuViewport.pointer(30f, 540f, 1920, 1080, 4f / 3f)
        assertFalse(outside.inside)
        assertTrue(WiiuViewport.pointer(960f, 540f, 1920, 1080, 4f / 3f).inside)
    }
}
