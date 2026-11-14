package com.swordfish.lemuroid.app.shared.game.viewmodel

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CartdeckMappedInputActivityGateTest {
    @Test
    fun onlyAcceptedDownEdgesTriggerActivityAndReleasesPermitNewPresses() {
        var presses = 0
        val gate = CartdeckMappedInputActivityGate { presses++ }
        val button = KeyEvent.KEYCODE_BUTTON_A

        assertFalse(gate.observe("touch", button, KeyEvent.ACTION_DOWN))
        gate.configure(true)
        assertTrue(gate.observe("touch", button, KeyEvent.ACTION_DOWN))
        assertFalse(gate.observe("touch", button, KeyEvent.ACTION_DOWN))
        assertFalse(gate.observe("touch", button, KeyEvent.ACTION_UP))
        assertTrue(gate.observe("touch", button, KeyEvent.ACTION_DOWN))
        assertEquals(2, presses)
    }

    @Test
    fun touchAndTwoGamepadsKeepIndependentPressedStates() {
        var presses = 0
        val gate = CartdeckMappedInputActivityGate { presses++ }
        gate.configure(true)
        val button = KeyEvent.KEYCODE_BUTTON_B

        assertTrue(gate.observe("touch", button, KeyEvent.ACTION_DOWN))
        assertTrue(gate.observe("physical:4:0", button, KeyEvent.ACTION_DOWN))
        assertTrue(gate.observe("physical:5:1", button, KeyEvent.ACTION_DOWN))
        assertFalse(gate.observe("physical:4:0", button, KeyEvent.ACTION_DOWN))
        assertEquals(3, presses)

        gate.observe("touch", button, KeyEvent.ACTION_UP)
        assertTrue(gate.observe("touch", button, KeyEvent.ACTION_DOWN))
        assertEquals(4, presses)
    }

    @Test
    fun menuBackMalformedActionsAndHeldRepeatsAreExcluded() {
        var presses = 0
        val gate = CartdeckMappedInputActivityGate { presses++ }
        gate.configure(true)
        for (key in listOf(KeyEvent.KEYCODE_BUTTON_MODE, KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_BACK)) {
            assertFalse(gate.observe("touch", key, KeyEvent.ACTION_DOWN))
        }
        assertFalse(gate.observe("", KeyEvent.KEYCODE_BUTTON_A, KeyEvent.ACTION_DOWN))
        assertFalse(gate.observe("touch", -1, KeyEvent.ACTION_DOWN))
        assertFalse(gate.observe("touch", KeyEvent.KEYCODE_BUTTON_A, KeyEvent.ACTION_MULTIPLE))
        assertFalse(gate.observe("touch", KeyEvent.KEYCODE_BUTTON_A, KeyEvent.ACTION_UP))
        assertEquals(0, presses)
    }

    @Test
    fun disablingModeClearsHeldInputAndPreventsGhostPulses() {
        var presses = 0
        val gate = CartdeckMappedInputActivityGate { presses++ }
        gate.configure(true)
        val button = KeyEvent.KEYCODE_BUTTON_X
        assertTrue(gate.observe("touch", button, KeyEvent.ACTION_DOWN))
        gate.configure(false)
        assertFalse(gate.observe("touch", button, KeyEvent.ACTION_DOWN))
        assertEquals(1, presses)

        gate.configure(true)
        assertTrue(gate.observe("touch", button, KeyEvent.ACTION_DOWN))
        assertEquals(2, presses)
    }

    @Test
    fun heldSetIsBoundedEvenIfAControllerNeverReleasesKeys() {
        var presses = 0
        val gate = CartdeckMappedInputActivityGate { presses++ }
        gate.configure(true)
        repeat(CartdeckMappedInputActivityGate.MAX_HELD_BUTTONS) { index ->
            assertTrue(gate.observe("source:$index", KeyEvent.KEYCODE_BUTTON_A, KeyEvent.ACTION_DOWN))
        }
        assertFalse(gate.observe("source:overflow", KeyEvent.KEYCODE_BUTTON_A, KeyEvent.ACTION_DOWN))
        assertEquals(CartdeckMappedInputActivityGate.MAX_HELD_BUTTONS, presses)

        gate.observe("source:0", KeyEvent.KEYCODE_BUTTON_A, KeyEvent.ACTION_UP)
        assertTrue(gate.observe("source:overflow", KeyEvent.KEYCODE_BUTTON_A, KeyEvent.ACTION_DOWN))
        assertEquals(CartdeckMappedInputActivityGate.MAX_HELD_BUTTONS + 1, presses)
    }
}
