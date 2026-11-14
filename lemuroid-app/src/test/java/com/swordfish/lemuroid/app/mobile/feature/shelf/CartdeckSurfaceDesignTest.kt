package com.swordfish.lemuroid.app.mobile.feature.shelf

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Defensive proof that decorative art cannot occupy measured game or control pixels. */
class CartdeckSurfaceDesignTest {
    @Test
    fun portraitPatternTilesNeverOverlapVideoControlsOrBrand() {
        val frame = Rect(10f, 10f, 383f, 842f)
        val exclusions =
            listOf(
                Rect(22f, 86f, 371f, 350f),
                Rect(10f, 560f, 190f, 817f),
                Rect(202f, 560f, 375f, 817f),
                Rect(112f, 15f, 281f, 79f),
            )
        val safe = CartdeckSurfaceDesign.safeTiles(frame, exclusions, 24f, 7f)
        assertTrue("Expected residual corner and panel ornament", safe.isNotEmpty())
        assertTrue(safe.all { cell -> cell.left >= frame.left && cell.right <= frame.right })
        assertTrue(safe.all { cell -> cell.top >= frame.top && cell.bottom <= frame.bottom })
        assertTrue(
            safe.none { cell ->
                exclusions.any { excluded ->
                    cell.left < excluded.right + 7f && cell.right > excluded.left - 7f &&
                        cell.top < excluded.bottom + 7f && cell.bottom > excluded.top - 7f
                }
            },
        )
    }

    @Test
    fun landscapeAndTabletTilesAreBoundedAndRemainOutsideGame() {
        for (frame in listOf(Rect(0f, 0f, 832f, 384f), Rect(0f, 0f, 1920f, 1200f))) {
            val viewport = Rect(frame.width * 0.12f, 16f, frame.width * 0.88f, frame.height - 16f)
            val tiles = CartdeckSurfaceDesign.safeTiles(frame, listOf(viewport), 24f, 8f)
            assertTrue("Pattern work is deliberately bounded", tiles.size <= 420)
            assertTrue(
                tiles.none { rect ->
                    rect.left < viewport.right + 8f && rect.right > viewport.left - 8f &&
                        rect.top < viewport.bottom + 8f && rect.bottom > viewport.top - 8f
                },
            )
        }
    }

    @Test
    fun stampRelocatesOrHidesInsteadOfTouchingVideo() {
        val frame = Rect(0f, 0f, 393f, 852f)
        val video = Rect(10f, 80f, 383f, 420f)
        val seat = CartdeckSurfaceDesign.stampSeat(frame, listOf(video), 46f, 6f)
        assertNotNull("Residual shell room supports an original stamp", seat)
        val stamp = seat!!
        assertFalse(
            stamp.left < video.right + 6f && stamp.right > video.left - 6f &&
                stamp.top < video.bottom + 6f && stamp.bottom > video.top - 6f,
        )
        assertEquals(null, CartdeckSurfaceDesign.stampSeat(frame, listOf(frame), 46f, 6f))
        assertTrue(CartdeckSurfaceDesign.validStamp(CartdeckSurfaceDesign.STAMP_SMILEY))
        assertFalse(CartdeckSurfaceDesign.validPattern("untrusted-pattern"))
    }
}
