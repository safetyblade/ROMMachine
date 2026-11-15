package com.swordfish.lemuroid.app.shared.input

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControllerMenuEdgeTest {
    @Test fun matchedCycleFiresOnceAndRejectsOrphansRepeatsAndDuplicateDowns() {
        val edge = ControllerMenuEdge()
        assertFalse(edge.handle(1, 110, false))
        assertFalse(edge.handle(1, 110, true, downTime = 10))
        assertFalse(edge.handle(1, 110, true, downTime = 10))
        assertFalse(edge.handle(1, 110, true, repeat = 4, downTime = 10))
        assertTrue(edge.handle(1, 110, false, downTime = 10))
        assertFalse(edge.handle(1, 110, false, downTime = 10))
        assertFalse(edge.handle(1, 110, true, downTime = 10))
        assertFalse(edge.handle(1, 110, false, downTime = 10))
        assertFalse(edge.handle(1, 110, true, downTime = 20))
        assertTrue(edge.handle(1, 110, false, downTime = 20))
    }

    @Test fun canceledAndDisconnectedPressCannotOpenMenu() {
        val edge = ControllerMenuEdge()
        edge.handle(1, 110, true, downTime = 1)
        assertFalse(edge.handle(1, 110, false, canceled = true, downTime = 1))
        assertFalse(edge.handle(1, 110, false, downTime = 1))
        edge.handle(2, 110, true, downTime = 2)
        edge.retainDevices(setOf(1))
        assertFalse(edge.handle(2, 110, false, downTime = 2))
        edge.handle(1, 110, true, downTime = 3)
        edge.clear()
        assertFalse(edge.handle(1, 110, false, downTime = 3))
    }

    @Test fun sevenPortsAndTwoMenuKeysNeverCompleteAnotherPlayersGesture() {
        val edge = ControllerMenuEdge()
        (1..7).forEach { edge.handle(it, 110, true, downTime = it.toLong()) }
        assertFalse(edge.handle(1, 82, false, downTime = 1))
        (7 downTo 1).forEach { assertTrue(edge.handle(it, 110, false, downTime = it.toLong())) }
        edge.handle(1, 110, true, downTime = 9)
        assertFalse(edge.handle(1, 110, false, downTime = 10))
        assertTrue(edge.handle(1, 110, false, downTime = 9))
    }
}
