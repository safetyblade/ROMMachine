package com.swordfish.lemuroid.app.shared.game.xbox360

import android.view.KeyEvent
import com.swordfish.lemuroid.app.shared.game.xbox.XboxPadState
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class Xbox360PadTest {
    @Test fun nativeOrderBumpersTriggersAndSignedSticks() {
        val pad = XboxPadState()
        pad.key(KeyEvent.KEYCODE_BUTTON_A, true)
        pad.key(KeyEvent.KEYCODE_BUTTON_L1, true)
        pad.key(KeyEvent.KEYCODE_BUTTON_R2, true)
        pad.key(KeyEvent.KEYCODE_BUTTON_SELECT, true)
        pad.direction(1, -1f, -.5f)
        pad.direction(2, 1f, 1f)
        val values = Xbox360Pad.values(pad)
        assertEquals(24, values.size)
        assertEquals(255, values[4])
        assertEquals(255, values[10])
        assertEquals(1, values[8])
        assertEquals(255, values[15])
        assertEquals(0, values[14])
        assertEquals(-32767, values[16])
        assertEquals(-16383, values[19])
        assertEquals(32767, values[21])
        assertEquals(32767, values[22])
        pad.clear()
        assertArrayEquals(IntArray(24), Xbox360Pad.values(pad))
    }

    @Test fun releasingOneInputSourceDoesNotReleaseTheOther() {
        val hardware = XboxPadState()
        val touch = XboxPadState()
        hardware.key(KeyEvent.KEYCODE_BUTTON_B, true)
        touch.key(KeyEvent.KEYCODE_BUTTON_B, true)
        touch.clear()
        assertEquals(255, Xbox360Pad.merged(hardware, touch)[5])
        hardware.clear()
        assertArrayEquals(IntArray(24), Xbox360Pad.merged(hardware, touch))
    }
}
