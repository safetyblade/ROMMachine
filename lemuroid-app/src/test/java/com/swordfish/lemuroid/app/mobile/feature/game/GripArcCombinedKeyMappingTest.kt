package com.swordfish.lemuroid.app.mobile.feature.game

import android.view.KeyEvent
import com.swordfish.lemuroid.lib.library.SystemID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GripArcCombinedKeyMappingTest {
    @Test fun dreamcastPhysicalButtonsMatchNativeFlycastRetroPad() {
        val system = SystemID.DREAMCAST
        // Physical Dreamcast A/B/X/Y -> Flycast RetroPad B/A/Y/X.
        assertEquals(KeyEvent.KEYCODE_BUTTON_B, gripArcCombinedKeyCode(system, "A"))
        assertEquals(KeyEvent.KEYCODE_BUTTON_A, gripArcCombinedKeyCode(system, "B"))
        assertEquals(KeyEvent.KEYCODE_BUTTON_Y, gripArcCombinedKeyCode(system, "X"))
        assertEquals(KeyEvent.KEYCODE_BUTTON_X, gripArcCombinedKeyCode(system, "Y"))
        assertEquals(KeyEvent.KEYCODE_BUTTON_L2, gripArcCombinedKeyCode(system, "L"))
        assertEquals(KeyEvent.KEYCODE_BUTTON_R2, gripArcCombinedKeyCode(system, "R"))
        assertEquals(KeyEvent.KEYCODE_BUTTON_START, gripArcCombinedKeyCode(system, "Start"))
        assertNull(gripArcCombinedKeyCode(system, "Select"))
        assertNull(gripArcCombinedKeyCode(system, "L2"))
        assertNull(gripArcCombinedKeyCode(system, "R2"))
    }

    @Test fun otherConsoleButtonCodesStayUnchanged() {
        for (system in listOf(SystemID.GAMECUBE, SystemID.PLAYSTATION_2, SystemID.GBA)) {
            assertEquals(KeyEvent.KEYCODE_BUTTON_A, gripArcCombinedKeyCode(system, "A"))
            assertEquals(KeyEvent.KEYCODE_BUTTON_B, gripArcCombinedKeyCode(system, "B"))
            assertEquals(KeyEvent.KEYCODE_BUTTON_X, gripArcCombinedKeyCode(system, "X"))
            assertEquals(KeyEvent.KEYCODE_BUTTON_Y, gripArcCombinedKeyCode(system, "Y"))
            assertEquals(KeyEvent.KEYCODE_BUTTON_L1, gripArcCombinedKeyCode(system, "L"))
            assertEquals(KeyEvent.KEYCODE_BUTTON_R1, gripArcCombinedKeyCode(system, "R"))
        }
        assertEquals(KeyEvent.KEYCODE_BUTTON_R2, gripArcCombinedKeyCode(SystemID.GAMECUBE, "R2"))
        assertEquals(KeyEvent.KEYCODE_BUTTON_SELECT, gripArcCombinedKeyCode(SystemID.PLAYSTATION_2, "Select"))
    }
}
