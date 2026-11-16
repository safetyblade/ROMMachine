package com.swordfish.lemuroid.app.shared.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ControllerInputProbeTest {
    @Test fun simultaneousChordsReleaseAndDisconnectClearHeldInput() {
        val probe = ControllerInputProbe()
        probe.key(96, true)
        probe.key(97, true)
        probe.key(96, true)
        assertEquals(setOf(96, 97), probe.held)
        probe.key(96, false)
        assertEquals(setOf(97), probe.held)
        assertEquals("96 released", probe.lastEvent)
        probe.clear()
        assertTrue(probe.held.isEmpty())
    }

    @Test fun prolongedAndRepeatedPressesRemainVisibleUntilReleaseOrDisconnect() {
        var now = 0L
        val probe = ControllerInputProbe { now }
        probe.key(96, true)
        now = 11_000L
        repeat(6) { probe.key(96, true) }
        assertEquals(setOf(96), probe.heldLongerThan(10_000))
        assertEquals(setOf(96), probe.repeatedKeys)
        probe.key(96, false)
        assertTrue(probe.heldLongerThan(10_000).isEmpty())
        assertTrue(probe.repeatedKeys.isEmpty())
        probe.key(97, true)
        probe.clear()
        assertTrue(probe.heldLongerThan(0).isEmpty())
    }

    @Test fun neutralRangesDiagonalsAndAnalogDigitalTriggersStayIndependent() {
        val probe = ControllerInputProbe()
        listOf(0f, -1f, 1f, 0.02f).forEach { probe.axis(0, it) }
        assertEquals(ControllerInputProbe.Axis(0.02f, -1f, 1f), probe.axes[0])
        probe.axis(15, -1f)
        probe.axis(16, 1f)
        probe.axis(17, 0.5f)
        probe.key(104, true)
        assertEquals(0.5f, probe.axes[17]!!.value)
        assertEquals(setOf(104), probe.held)
        probe.axis(17, Float.NaN)
        assertEquals(0.5f, probe.axes[17]!!.value)
    }
}
