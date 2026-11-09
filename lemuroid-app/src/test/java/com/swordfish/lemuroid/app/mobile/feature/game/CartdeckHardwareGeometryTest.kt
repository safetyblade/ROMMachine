package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Noninteractive shell geometry regressions. Actual touch targets are QA measured. */
class CartdeckHardwareGeometryTest {
    @Test
    fun referenceFourByThreeViewportIsNotShrunkToFitRim() {
        val root = Rect(100f, 200f, 493f, 1052f)
        val viewport = Rect(122f, 284f, 471f, 545.75f)
        val local = CartdeckHardwareGeometry.inRoot(viewport, root)!!
        assertEquals(22f, local.left)
        assertEquals(84f, local.top)
        assertEquals(349f, local.width)
        assertEquals(261.75f, local.height)
    }

    @Test
    fun dsVideoIsAnExclusionFromEveryDecorativeLine() {
        val video = Rect(18f, 18f, 375f, 496f)
        assertTrue(
            CartdeckHardwareGeometry.seamSegments(
                10f,
                383f,
                400f,
                listOf(video),
                4f,
            ).isEmpty(),
        )
    }

    @Test
    fun currentSecondaryPadAndCompactCslotBreakTheLowerSeam() {
        val left = Rect(20f, 720f, 190f, 828f)
        val right = Rect(208f, 720f, 373f, 828f)
        val badge = Rect(174f, 788f, 219f, 839f)
        val segments =
            CartdeckHardwareGeometry.seamSegments(
                8f,
                385f,
                813f,
                listOf(left, right, badge),
                4f,
            )
        val excluded = listOf(left, right, badge).map { CartdeckHardwareGeometry.expanded(it, 4f) }
        assertTrue(segments.all { it.first < it.second })
        assertFalse(
            segments.any { (start, end) ->
                excluded.any { it.left < end && it.right > start && 813f >= it.top && 813f <= it.bottom }
            },
        )
    }

    @Test
    fun zeroSizeOrMissingViewportsDoNotProduceFalseApertures() {
        assertEquals(null, CartdeckHardwareGeometry.inRoot(null, Rect(0f, 0f, 393f, 852f)))
        assertEquals(null, CartdeckHardwareGeometry.inRoot(Rect(0f, 0f, 0f, 0f), Rect(0f, 0f, 393f, 852f)))
    }
}
