package com.swordfish.lemuroid.app.mobile.feature.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeRendererAuditTest {
    @Test
    fun viewportRequiresBothRequestedAndObservedSurfaceDimensions() {
        val missing = NativeRendererAudit.Geometry()
        assertEquals("NO_REQUESTED_APERTURE", NativeRendererAudit.surfaceStatus(missing))
        assertEquals(
            "NO_OBSERVED_SURFACE",
            NativeRendererAudit.surfaceStatus(
                NativeRendererAudit.Geometry(requestedWidth = 1600, requestedHeight = 900),
            ),
        )
    }

    @Test
    fun nativeSurfaceCallbacksMustMatchRequestedSizeWithinOnePixel() {
        val tracker = NativeRendererAudit.Tracker(NativeViewportPolicy.Host.XBOX360)
        tracker.request(1280, 720)
        tracker.observe(1280, 720)
        assertTrue(tracker.summary().contains("surface_state=SURFACE_DIMENSIONS_MATCH"))
        tracker.observe(1281, 719)
        assertTrue(tracker.summary().contains("surface_state=SURFACE_DIMENSIONS_MATCH"))
        tracker.observe(1000, 720)
        assertTrue(tracker.summary().contains("surface_state=PENDING_OR_MISMATCHED_RESIZE"))
        tracker.observe(0, 0)
        assertTrue(tracker.summary().contains("surface_state=NO_OBSERVED_SURFACE"))
    }

    @Test
    fun cemuReportsNativeImageAspectSeparatelyFromTheAndroidSurface() {
        val tracker = NativeRendererAudit.Tracker(NativeViewportPolicy.Host.CEMU)
        tracker.request(1920, 1080)
        tracker.observe(1920, 1080)
        tracker.nativeImage(1280, 960)
        val summary = tracker.summary()
        assertTrue(summary.contains("native_image=1280x960"))
        assertTrue(summary.contains("expected_aspect_fit=1440x1080"))
        assertTrue(summary.contains("surface_state=SURFACE_DIMENSIONS_MATCH"))
        assertTrue(summary.contains("pixel_presentation=UNVERIFIED"))
        assertFalse(summary.contains("game_path"))
    }

    @Test
    fun unknownNativeFrameDoesNotInheritTheDefaultFourThreeOrSixteenNineRatio() {
        val tracker = NativeRendererAudit.Tracker(NativeViewportPolicy.Host.ARMSX2)
        tracker.request(1440, 1080)
        tracker.observe(1440, 1080)
        val summary = tracker.summary()
        assertTrue(summary.contains("native_image=NOT_REPORTED"))
        assertTrue(summary.contains("expected_aspect_fit=UNKNOWN"))
        assertTrue(summary.contains("pixel_presentation=UNVERIFIED"))
    }

    @Test
    fun invalidImageReportCannotImplyConfirmedRenderedDimensions() {
        val tracker = NativeRendererAudit.Tracker(NativeViewportPolicy.Host.CEMU)
        tracker.request(1920, 1080)
        tracker.observe(1920, 1080)
        tracker.nativeImage(0, 1080)
        assertTrue(tracker.summary().contains("native_image=NOT_REPORTED"))
        tracker.nativeImage(1920, 1080)
        assertTrue(tracker.summary().contains("native_image=1920x1080"))
    }

    @Test
    fun geometryEvidenceDoesNotChangeTheDualScreenInputContract() {
        assertEquals(
            CartdeckViewportPolicy.PortraitMode.DUAL_SCREEN,
            CartdeckViewportPolicy.portraitMode(com.swordfish.lemuroid.lib.library.SystemID.NDS),
        )
        assertEquals(
            CartdeckViewportPolicy.PortraitMode.DUAL_SCREEN,
            CartdeckViewportPolicy.portraitMode(com.swordfish.lemuroid.lib.library.SystemID.NINTENDO_3DS),
        )
    }
}
