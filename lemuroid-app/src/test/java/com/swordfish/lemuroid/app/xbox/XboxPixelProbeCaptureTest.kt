package com.swordfish.lemuroid.app.xbox

import com.swordfish.lemuroid.app.shared.game.xbox.XboxPixelProbeCapture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XboxPixelProbeCaptureTest {
    private fun native(pairs: Int) =
        "pixel_probe=requested; maximum=3\n" +
            (1..pairs).joinToString("\n") {
                "serial=$it guest=640x480 {fnv=1111} swap=1280x720 {fnv=2222}"
            }

    @Test fun surfaceMayFinishBeforeSlowNativePairsWithoutPublishingPartialReport() {
        val capture = XboxPixelProbeCapture()
        assertTrue(capture.surfaceResult(true))
        assertNull(capture.observe(1800, native(1)))
        assertNull(capture.observe(2500, native(2)))
        val result = capture.observe(3200, native(3))!!
        assertEquals(XboxPixelProbeCapture.Status.COMPLETE_SAMPLES, result.status)
        assertEquals(3, result.nativePairs)
        assertNull(capture.observe(3500, native(3)))
    }

    @Test fun nativeMayFinishFirstButMustWaitForSurfaceResult() {
        val capture = XboxPixelProbeCapture()
        assertNull(capture.observe(1000, native(3)))
        capture.surfaceResult(true)
        assertEquals(XboxPixelProbeCapture.Status.COMPLETE_SAMPLES, capture.observe(2000, native(3))!!.status)
    }

    @Test fun latePixelCopyCannotOverwriteSurfaceTimeoutOrCopyReportTwice() {
        val capture = XboxPixelProbeCapture()
        assertTrue(capture.surfaceResult(false))
        assertFalse(capture.surfaceResult(true))
        assertEquals(XboxPixelProbeCapture.Status.INCOMPLETE_SAMPLES, capture.observe(4800, native(3))!!.status)
        assertFalse(capture.surfaceResult(true))
        assertNull(capture.observe(5000, native(3)))
    }

    @Test fun unsupportedTransferAndDeadlineRemainIncompleteEvidenceNotBlankFrameSuccess() {
        val unsupported = XboxPixelProbeCapture()
        unsupported.surfaceResult(true)
        assertEquals(
            XboxPixelProbeCapture.Status.INCOMPLETE_SAMPLES,
            unsupported.observe(2000, "pixel_probe=unsupported swapchain transfer-source usage")!!.status,
        )
        val stalled = XboxPixelProbeCapture()
        assertNull(stalled.observe(5999, native(1)))
        assertEquals(XboxPixelProbeCapture.Status.CAPTURE_TIMEOUT, stalled.observe(6000, native(1))!!.status)
        assertNull(stalled.observe(7000, native(3)))
    }

    @Test fun pauseOrExitCancelsPendingPublication() {
        val capture = XboxPixelProbeCapture()
        capture.cancel()
        assertFalse(capture.surfaceResult(true))
        assertNull(capture.observe(10000, native(3)))
    }
}
