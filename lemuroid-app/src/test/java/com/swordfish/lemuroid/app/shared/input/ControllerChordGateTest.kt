package com.swordfish.lemuroid.app.shared.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControllerChordGateTest {
    private val keys = setOf(106, 107)

    @Test fun chordNeverLeaksAndInvokesOnceAcrossRepeatAndRelease() {
        val gate = ControllerChordGate()
        assertTrue(gate.route(1, 106, true, keys).consumed)
        val complete = gate.route(1, 107, true, keys)
        assertTrue(complete.invoke)
        assertTrue(complete.replay.isEmpty())
        assertFalse(gate.route(1, 107, true, keys).invoke)
        assertTrue(gate.route(1, 106, false, keys).replay.isEmpty())
        assertTrue(gate.route(1, 107, false, keys).replay.isEmpty())
        assertFalse(gate.route(1, 107, false, keys).invoke)
    }

    @Test fun independentPressReplaysMatchedPairAndDevicesCannotCompleteEachOthersChord() {
        val gate = ControllerChordGate()
        gate.route(1, 106, true, keys)
        assertFalse(gate.route(2, 107, true, keys).invoke)
        assertEquals(
            listOf(ControllerChordGate.Edge(106, true), ControllerChordGate.Edge(106, false)),
            gate.route(1, 106, false, keys).replay,
        )
        gate.retainDevices(setOf(1))
        assertTrue(gate.route(2, 107, false, keys).replay.isEmpty())
        assertFalse(gate.route(1, 96, true, keys).consumed)
    }

    @Test fun changingConfigurationAndRepeatedBackCannotReplayOrInvokeCanceledPress() {
        val gate = ControllerChordGate()
        assertFalse(gate.route(1, 96, true, emptySet()).consumed)
        gate.route(1, 106, true, keys)
        gate.route(1, 106, false, keys, canceled = true)
        assertTrue(gate.route(1, 106, false, keys).replay.isEmpty())
        assertFalse(gate.route(1, 96, true, setOf(108, 109)).consumed)
    }
}
