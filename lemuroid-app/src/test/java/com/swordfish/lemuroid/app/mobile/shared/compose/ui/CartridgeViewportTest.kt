package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class CartridgeViewportTest {
    @Test fun measuredContentBoundsContainArtInHandheldTabletAndTv() {
        for ((width, height) in listOf(320f to 450f, 720f to 180f, 800f to 1000f, 1200f to 650f)) {
            for (font in listOf(1f, 1.5f, 2f)) for (favorites in listOf(false, true)) {
                val shelf = CartridgeViewport.shelf(width, height, font, favorites)
                assertTrue(shelf.spineWidth > 0f)
                assertTrue(shelf.spineWidth <= 72f)
                assertTrue(shelf.columns >= 1)
                assertTrue(shelf.spineWidth * shelf.columns + 4f * (shelf.columns - 1) <= width - 24f + 0.01f)
            }
            val art = CartridgeViewport.detailArtWidth(width / 2, height)
            assertTrue(art * 390f / 320f <= height + 0.01f)
            assertTrue(art <= width / 2)
        }
    }

    @Test fun portraitPhoneRestoresFiveColumnsAtDefaultFontScale() {
        for (width in listOf(360f, 390f, 412f, 480f)) {
            org.junit.Assert.assertEquals(5, CartridgeViewport.shelf(width, 800f, 1f, false).columns)
            assertTrue(CartridgeViewport.shelf(width, 800f, 2f, false).columns < 5)
        }
        for (height in listOf(180f, 300f, 500f, 800f)) {
            org.junit.Assert.assertEquals(5, CartridgeViewport.shelf(412f, height, 1f, false).columns)
        }
        assertTrue(CartridgeViewport.shelf(720f, 100f, 1f, false).spineWidth >= 48f)
    }

    @Test fun largeFontsReduceDensityInsteadOfPackingMoreSpines() {
        assertTrue(
            CartridgeViewport.shelf(900f, 600f, 2f, false).columns <
                CartridgeViewport.shelf(900f, 600f, 1f, false).columns,
        )
    }
}
