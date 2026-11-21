package com.swordfish.lemuroid.app.shared.startup

import org.junit.Assert.assertEquals
import org.junit.Test

class CartdeckIntroFramingTest {
    @Test
    fun framingClampsZoomAndPanBeforeSavingOrRendering() {
        val unsafe = CartdeckIntroFraming(zoom = 8f, offsetX = -99f, offsetY = 99f)
        val safe = unsafe.constrained()
        assertEquals(2.5f, safe.zoom, 0f)
        assertEquals(-0.45f, safe.offsetX, 0f)
        assertEquals(0.45f, safe.offsetY, 0f)
    }

    @Test
    fun nonFiniteAdjustmentsFallBackToNeutral() {
        val safe =
            CartdeckIntroFraming(
                zoom = Float.NaN,
                offsetX = Float.POSITIVE_INFINITY,
                offsetY = Float.NEGATIVE_INFINITY,
            ).constrained()
        assertEquals(1f, safe.zoom, 0f)
        assertEquals(0f, safe.offsetX, 0f)
        assertEquals(0f, safe.offsetY, 0f)
    }
}
