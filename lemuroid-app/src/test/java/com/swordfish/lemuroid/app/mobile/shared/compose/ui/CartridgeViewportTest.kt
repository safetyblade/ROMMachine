package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class CartridgeViewportTest {
    @Test fun measuredContentBoundsContainArtInHandheldTabletAndTv() {
        for ((width, height) in listOf(320f to 450f, 720f to 180f, 800f to 1000f, 1200f to 650f)) {
            for (font in listOf(1f, 1.5f, 2f)) for (favorites in listOf(false, true)) {
                val shelf = CartridgeViewport.shelf(width, height, font, favorites)
                val decoration = 38f + if (favorites) 48f else 0f
                assertTrue(shelf.spineWidth * 340f / 94f + decoration <= height + 0.01f)
                assertTrue(shelf.columns >= 1)
                assertTrue(shelf.spineWidth * shelf.columns + 4f * (shelf.columns - 1) <= width - 24f + 0.01f)
            }
            val art = CartridgeViewport.detailArtWidth(width / 2, height)
            assertTrue(art * 390f / 320f <= height + 0.01f)
            assertTrue(art <= width / 2)
        }
    }

    @Test fun largeFontsReduceDensityInsteadOfPackingMoreSpines() {
        assertTrue(
            CartridgeViewport.shelf(900f, 600f, 2f, false).columns <
                CartridgeViewport.shelf(900f, 600f, 1f, false).columns,
        )
    }
}
