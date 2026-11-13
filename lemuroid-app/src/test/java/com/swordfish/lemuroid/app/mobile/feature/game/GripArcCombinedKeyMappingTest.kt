package com.swordfish.lemuroid.app.mobile.feature.game

import android.view.KeyEvent
import com.swordfish.touchinput.radial.settings.NativeControlIcon
import com.swordfish.touchinput.radial.settings.NativeControlInventory
import com.swordfish.touchinput.radial.settings.TouchControllerID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GripArcCombinedKeyMappingTest {
    @Test fun dreamcastNativeMappingRetainsTriggerPairAndOneStick() {
        val controls =
            NativeControlInventory.forController(
                TouchControllerID.DREAMCAST,
            )!!.controls.associateBy { it.key }
        assertEquals(KeyEvent.KEYCODE_BUTTON_B, controls.getValue("A").keyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_A, controls.getValue("B").keyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_Y, controls.getValue("X").keyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_X, controls.getValue("Y").keyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_L2, controls.getValue("L").keyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_R2, controls.getValue("R").keyCode)
        assertFalse("Select" in controls || "L2" in controls || "R2" in controls || "AnalogR" in controls)
        assertEquals(1, controls.getValue("AnalogL").motionSource)
    }

    @Test fun selectedControllerVariantDeterminesCompleteInventory() {
        val digital = NativeControlInventory.forController(TouchControllerID.PSX)!!.controls.associateBy { it.key }
        val dual =
            NativeControlInventory.forController(
                TouchControllerID.PSX_DUALSHOCK,
            )!!.controls.associateBy { it.key }
        assertFalse("AnalogL" in digital || "AnalogR" in digital)
        assertEquals(digital.keys + setOf("AnalogL", "AnalogR"), dual.keys)
        assertEquals(NativeControlIcon.CIRCLE, dual.getValue("A").icon)
        assertEquals(KeyEvent.KEYCODE_BUTTON_L2, dual.getValue("L2").keyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_THUMBR, dual.getValue("AnalogR").pressCode)
        val gc = NativeControlInventory.forController(TouchControllerID.GAMECUBE)!!.controls.associateBy { it.key }
        assertEquals(KeyEvent.KEYCODE_BUTTON_R2, gc.getValue("Z").keyCode)
        assertTrue(gc.getValue("Z").roundButton)
        assertEquals("C-Stick", gc.getValue("AnalogR").label)
        assertFalse("Select" in gc)
        assertNull(NativeControlInventory.forController(TouchControllerID.WII_NUNCHUK))
    }
}
