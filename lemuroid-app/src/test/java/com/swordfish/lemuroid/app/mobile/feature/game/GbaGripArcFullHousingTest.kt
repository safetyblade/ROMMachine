package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GbaGripArcFullHousingTest {
    private val root = Rect(0f, 0f, 393f, 852f)
    private val video = Rect(8f, 130f, 385f, 340f)

    private fun targets() =
        listOf(
            PivotArcBounds(320f, 500f, 369f, 548f),
            PivotArcBounds(320f, 560f, 369f, 608f),
            PivotArcBounds(320f, 620f, 369f, 668f),
            PivotArcBounds(260f, 700f, 309f, 748f),
        )

    @Test
    fun fullBodyIsOnePanelBelowViewportWithAllFourProgrammableKeys() {
        val panel = GbaGripArcFullBodyGeometry.resolve(root, video, targets())!!
        assertEquals(12f, panel.left, 0.01f)
        assertEquals(356f, panel.top, 0.01f)
        assertEquals(836f, panel.bottom, 0.01f)
        assertTrue(panel.top > video.bottom)
    }

    @Test
    fun invalidOrOverlappingKeysCannotBeHiddenByNewArtwork() {
        val outside = targets().dropLast(1) + PivotArcBounds(360f, 805f, 408f, 853f)
        assertNull(GbaGripArcFullBodyGeometry.resolve(root, video, outside))
        assertNull(GbaGripArcFullBodyGeometry.resolve(root, video, targets().take(3)))
        assertNull(
            GbaGripArcFullBodyGeometry.resolve(
                root,
                Rect(0f, 0f, 393f, 812f),
                targets(),
            ),
        )
    }
}
