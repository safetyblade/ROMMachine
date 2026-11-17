package com.swordfish.lemuroid.app.shared.game.pc

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PcHardwareInputTest {
    @Test
    fun keyboardUsesRetroKeyDomainWithoutStealingMenuKeys() {
        assertEquals('a'.code, PcHardwareInput.retroKey(KeyEvent.KEYCODE_A))
        assertEquals('z'.code, PcHardwareInput.retroKey(KeyEvent.KEYCODE_Z))
        assertEquals(13, PcHardwareInput.retroKey(KeyEvent.KEYCODE_ENTER))
        assertEquals(27, PcHardwareInput.retroKey(KeyEvent.KEYCODE_ESCAPE))
        assertEquals(293, PcHardwareInput.retroKey(KeyEvent.KEYCODE_F12))
        assertNull(PcHardwareInput.retroKey(KeyEvent.KEYCODE_MENU))
        assertNull(PcHardwareInput.retroKey(KeyEvent.KEYCODE_BUTTON_A))
        assertNull(PcHardwareInput.retroKey(KeyEvent.KEYCODE_BACK))
    }
}
