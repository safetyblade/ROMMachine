package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PivotArcModuleGeometryTest {
    private val root = Rect(0f, 0f, 393f, 852f)
    private val video = Rect(22f, 84f, 371f, 344f)
    private val controls =
        mapOf(
            "select" to Rect(28f, 380f, 84f, 428f),
            "start" to Rect(167f, 380f, 223f, 428f),
            "menu" to Rect(304f, 380f, 360f, 428f),
            "dpad" to Rect(170f, 560f, 292f, 682f),
            "shoulder-L" to Rect(85f, 570f, 145f, 630f),
            "face-b" to Rect(100f, 675f, 162f, 737f),
            "face-a" to Rect(160f, 735f, 222f, 797f),
            "shoulder-R" to Rect(250f, 728f, 310f, 788f),
        )

    @Test
    fun mountingPlateFitsRealCoreNotTheThreeUtilityButtons() {
        val result = PivotArcModuleGeometry.resolve(controls, video, root, 11f, 31f)
        assertNotNull(result)
        val plate = result!!.plate
        assertTrue(plate.top > video.bottom)
        assertTrue(plate.top > controls.getValue("start").bottom)
        assertTrue(plate.left < controls.getValue("shoulder-L").left)
        assertTrue(plate.right > controls.getValue("shoulder-R").right)
        assertTrue(plate.bottom > controls.getValue("face-a").bottom)
        assertTrue(plate.bottom < root.bottom)
        assertNotNull(result.wordmark)
        assertTrue(result.wordmark!!.bottom < controls.getValue("dpad").top)
    }

    @Test
    fun fourAdditionalKeysExpandTheSameMountWithoutBeingSilentlyOmitted() {
        val expanded =
            controls +
                mapOf(
                    "face-x" to Rect(54f, 508f, 114f, 568f),
                    "face-y" to Rect(107f, 468f, 167f, 528f),
                    "shoulder-L2" to Rect(297f, 515f, 350f, 568f),
                    "shoulder-R2" to Rect(245f, 465f, 299f, 519f),
                )
        val standard = PivotArcModuleGeometry.resolve(controls, video, root, 11f, 31f)!!
        val extra = PivotArcModuleGeometry.resolve(expanded, video, root, 11f, 31f)!!
        assertTrue(extra.plate.top < standard.plate.top)
        assertTrue(extra.plate.left < standard.plate.left)
        assertTrue(extra.plate.right > standard.plate.right)
        for (key in listOf("face-x", "face-y", "shoulder-L2", "shoulder-R2")) {
            val rect = expanded.getValue(key)
            assertTrue(rect.left > extra.plate.left)
            assertTrue(rect.top > extra.plate.top)
            assertTrue(rect.right < extra.plate.right)
            assertTrue(rect.bottom < extra.plate.bottom)
        }
    }

    @Test
    fun impossibleViewportOrNoControlsNeverBuildsAPlateOverTheVideo() {
        assertNull(PivotArcModuleGeometry.resolve(emptyMap(), video, root, 11f, 31f))
        assertNull(
            PivotArcModuleGeometry.resolve(
                controls,
                Rect(0f, 0f, 393f, 760f),
                root,
                11f,
                31f,
            ),
        )
    }

    @Test
    fun displayGeometryIsIndependentOfCustomPalette() {
        val first = PivotArcModuleGeometry.resolve(controls, video, root, 11f, 31f)
        val second = PivotArcModuleGeometry.resolve(controls.toMap(), video, root, 11f, 31f)
        assertEquals(first, second)
    }
}
