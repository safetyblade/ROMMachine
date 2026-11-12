package com.swordfish.lemuroid.app.shared.game.viewmodel

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PivotArcHeldInputGateTest {
    private val events = mutableListOf<Pair<Int, Int>>()
    private val gate = PivotArcHeldInputGate { action, key -> events.add(action to key) }
    private val a = KeyEvent.KEYCODE_BUTTON_A
    private val b = KeyEvent.KEYCODE_BUTTON_B

    @Test fun standardModeNeverCapturesOrLatches() {
        assertFalse(gate.setSource(b, "touch", true))
        gate.toggleHold(b)
        assertTrue(gate.holds.value.isEmpty())
        assertTrue(events.isEmpty())
    }

    @Test fun holdBRemainsPressedDuringDpadMovementAndOrdinaryBPressRelease() {
        gate.setEnabled(true)
        gate.toggleHold(b)
        assertEquals(listOf(KeyEvent.ACTION_DOWN to b), events)
        assertEquals(setOf(b), gate.holds.value)

        gate.setSource(b, "touch", true)
        gate.setSource(b, "touch", false)
        assertEquals(listOf(KeyEvent.ACTION_DOWN to b), events)

        gate.toggleHold(b)
        assertEquals(
            listOf(KeyEvent.ACTION_DOWN to b, KeyEvent.ACTION_UP to b),
            events,
        )
        assertTrue(gate.holds.value.isEmpty())
    }

    @Test fun bothLatchesCanOperateIndependently() {
        gate.setEnabled(true)
        gate.toggleHold(a)
        gate.toggleHold(b)
        assertEquals(setOf(a, b), gate.holds.value)
        gate.toggleHold(a)
        assertEquals(setOf(b), gate.holds.value)
        assertEquals(
            listOf(
                KeyEvent.ACTION_DOWN to a,
                KeyEvent.ACTION_DOWN to b,
                KeyEvent.ACTION_UP to a,
            ),
            events,
        )
        gate.toggleHold(b)
        assertEquals(KeyEvent.ACTION_UP to b, events.last())
    }

    @Test fun physicalSourceOutlivesLatchAndCannotBeCancelledByToggle() {
        gate.setEnabled(true)
        assertTrue(gate.setSource(b, "physical:1:97", true))
        gate.toggleHold(b)
        gate.toggleHold(b)
        assertEquals(listOf(KeyEvent.ACTION_DOWN to b), events)
        gate.setSource(b, "physical:1:97", false)
        assertEquals(KeyEvent.ACTION_UP to b, events.last())
    }

    @Test fun physicalAndVirtualSourcesNeverReleaseEachOther() {
        gate.setEnabled(true)
        gate.setSource(a, "touch", true)
        gate.setSource(a, "physical:1:96", true)
        gate.setSource(a, "touch", false)
        assertEquals(listOf(KeyEvent.ACTION_DOWN to a), events)
        gate.setSource(a, "physical:1:96", false)
        assertEquals(KeyEvent.ACTION_UP to a, events.last())
    }

    @Test fun lifecycleResetReleasesEveryKeyAndClearsBothIndicators() {
        gate.setEnabled(true)
        gate.toggleHold(a)
        gate.toggleHold(b)
        gate.setSource(a, "touch", true)
        gate.clear()
        assertTrue(gate.holds.value.isEmpty())
        assertEquals(2, events.count { it.first == KeyEvent.ACTION_UP })
        gate.clear()
        assertEquals(4, events.size)
        gate.setEnabled(false)
        assertFalse(gate.isEnabled())
    }

    @Test fun gripChordHasTwoIndependentNativeOwners() {
        val l = KeyEvent.KEYCODE_BUTTON_L1
        val r = KeyEvent.KEYCODE_BUTTON_R1
        gate.setGripEnabled(true)
        gate.setSource(a, "touch", true)
        gate.setGripSlot(1, true, setOf(a, l))
        gate.setGripSlot(2, true, setOf(l, r))
        assertEquals(
            listOf(
                KeyEvent.ACTION_DOWN to a,
                KeyEvent.ACTION_DOWN to l,
                KeyEvent.ACTION_DOWN to r,
            ),
            events,
        )
        gate.setGripSlot(1, false)
        assertEquals(listOf(KeyEvent.ACTION_DOWN to a, KeyEvent.ACTION_DOWN to l, KeyEvent.ACTION_DOWN to r), events)
        gate.setSource(a, "touch", false)
        assertEquals(KeyEvent.ACTION_UP to a, events.last())
        gate.setGripSlot(2, false)
        assertEquals(KeyEvent.ACTION_UP to r, events.last())
        assertEquals(3, events.count { it.first == KeyEvent.ACTION_UP })
    }

    @Test fun gripAndPivotHoldRetainTheirOwnBPresses() {
        val r = KeyEvent.KEYCODE_BUTTON_R1
        gate.setEnabled(true)
        gate.setGripEnabled(true)
        gate.toggleHold(b)
        gate.setGripSlot(1, true, setOf(b, r))
        gate.toggleHold(b)
        assertEquals(0, events.count { it == KeyEvent.ACTION_UP to b })
        gate.setGripSlot(1, false)
        assertEquals(1, events.count { it == KeyEvent.ACTION_UP to b })
        assertEquals(1, events.count { it == KeyEvent.ACTION_UP to r })
    }

    @Test fun hiddenSlotOrBackgroundReleaseDoesNotLeaveGhostInputs() {
        val l = KeyEvent.KEYCODE_BUTTON_L1
        gate.setGripEnabled(true)
        gate.setGripSlot(4, true, setOf(l))
        gate.releaseGripSlots()
        assertEquals(listOf(KeyEvent.ACTION_DOWN to l, KeyEvent.ACTION_UP to l), events)
        gate.setGripSlot(1, true, setOf(a, b))
        gate.clear()
        assertEquals(2, events.count { it.first == KeyEvent.ACTION_DOWN && it.second in setOf(a, b) })
        assertEquals(2, events.count { it.first == KeyEvent.ACTION_UP && it.second in setOf(a, b) })
    }

    @Test fun switchingOffPivotArcReleasesAndPreventsRelatching() {
        gate.setEnabled(true)
        gate.toggleHold(b)
        gate.setEnabled(false)
        gate.toggleHold(b)
        assertEquals(
            listOf(KeyEvent.ACTION_DOWN to b, KeyEvent.ACTION_UP to b),
            events,
        )
        assertTrue(gate.holds.value.isEmpty())
    }
}
