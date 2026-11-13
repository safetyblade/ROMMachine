package com.swordfish.lemuroid.app.xbox

import com.swordfish.lemuroid.app.shared.game.xbox.FramePixelSummary
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FramePixelSummaryTest {
    @Test fun distinguishesOpaqueWhiteBlackAndVariedFramesWithoutExportingPixels() {
        val white = FramePixelSummary.argb(IntArray(64) { -1 })
        val black = FramePixelSummary.argb(IntArray(64) { 0xff000000.toInt() })
        val mixed = FramePixelSummary.argb(IntArray(64) { if (it % 2 == 0) -1 else 0xff000000.toInt() })
        assertTrue(white.contains("rgb=255..255 white=64 black=0 alpha=255..255"))
        assertTrue(black.contains("rgb=0..0 white=0 black=64 alpha=255..255"))
        assertTrue(mixed.contains("rgb=0..255 white=32 black=32"))
        assertNotEquals(white.substringAfter("fnv="), black.substringAfter("fnv="))
    }

    @Test fun transparentSurfaceIsNotConfusedWithOpaqueWhite() {
        assertTrue(FramePixelSummary.argb(IntArray(64) { 0x00ffffff }).contains("alpha=0..0"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnboundedFrameArrays() {
        FramePixelSummary.argb(IntArray(4097))
    }
}
