package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CartdeckHardwareSeatPolicyTest {
    private val root = Rect(0f, 0f, 393f, 852f)
    private val video = Rect(22f, 84f, 371f, 345.75f)

    @Test
    fun ornamentCannotEnterTheExistingVideoOpening() {
        val control = Rect(320f, 310f, 365f, 360f)
        assertNull(
            CartdeckHardwareSeatPolicy.at(
                "start",
                control,
                mapOf("start" to control),
                video,
                root,
                6f,
                4f,
            ),
        )
    }

    @Test
    fun aSeparatedStartOrSelectSeatNeverChangesItsFunctionalBounds() {
        val select = Rect(36f, 592f, 65f, 620f)
        val start = Rect(327f, 592f, 356f, 620f)
        val inputs = mapOf("select" to select, "start" to start)
        val selectSeat = CartdeckHardwareSeatPolicy.at("select", select, inputs, video, root, 3f, 4f)!!
        val startSeat = CartdeckHardwareSeatPolicy.at("start", start, inputs, video, root, 3f, 4f)!!
        assertEquals(select, inputs["select"])
        assertEquals(start, inputs["start"])
        assertTrue(selectSeat.left <= select.left && selectSeat.right >= select.right)
        assertTrue(startSeat.left <= start.left && startSeat.right >= start.right)
    }

    @Test
    fun crowdedControlsOmitArtworkRatherThanChangingInput() {
        val start = Rect(300f, 600f, 334f, 640f)
        val menu = Rect(337f, 600f, 370f, 640f)
        assertNull(
            CartdeckHardwareSeatPolicy.at(
                "start",
                start,
                mapOf("start" to start, "menu" to menu),
                video,
                root,
                5f,
                4f,
            ),
        )
    }

    @Test
    fun screenAndPhoneEdgesSuppressUnsafeSeats() {
        val analog = Rect(3f, 760f, 55f, 822f)
        assertNull(
            CartdeckHardwareSeatPolicy.at(
                "analog-left",
                analog,
                mapOf("analog-left" to analog),
                video,
                root,
                7f,
                4f,
            ),
        )
    }
}
