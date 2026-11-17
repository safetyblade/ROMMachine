package com.swordfish.lemuroid.app.shared.input

import android.view.KeyEvent
import com.swordfish.lemuroid.app.shared.game.xbox.XboxButtonBindingStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ControllerLabelsTest {
    @Test fun everyEditableInputHasAPlayerLabelWithoutAndroidCodeNames() {
        for (code in XboxButtonBindingStore.editableKeys) {
            assertFalse(ControllerLabels.input(code).contains("KEYCODE"))
            assertFalse(ControllerLabels.xboxOutput(code).contains("KEYCODE"))
        }
        assertEquals("D-pad Up", ControllerLabels.input(KeyEvent.KEYCODE_DPAD_UP))
        assertEquals("Left Stick Press", ControllerLabels.xboxOutput(KeyEvent.KEYCODE_BUTTON_THUMBL))
        assertEquals("Black", ControllerLabels.xboxOutput(KeyEvent.KEYCODE_BUTTON_R1))
        assertEquals(
            "Cross",
            ControllerLabels.input(KeyEvent.KEYCODE_BUTTON_A, ControllerHardwareProfile.Family.PLAYSTATION),
        )
        assertEquals("A", ControllerLabels.input(KeyEvent.KEYCODE_BUTTON_B, ControllerHardwareProfile.Family.SWITCH))
    }
}
