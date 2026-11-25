package com.swordfish.lemuroid.app.shared.game.viewmodel

import android.view.KeyEvent
import com.swordfish.touchinput.radial.settings.TouchControllerID
import org.junit.Assert.assertEquals
import org.junit.Test

class WonderSwanPivotInputTest {
    @Test fun secondCursorAndNativeFaceHoldsReleaseInBothOrientations() {
        for (mode in WonderSwanPivotInput.entries) {
            val messages = mutableListOf<Pair<Int, Int>>()
            val gate = PivotArcHeldInputGate { action, code -> messages += action to code }
            gate.setEnabled(true, mode.faceKeys, mode.cursorKeys)
            val expectedDiagonal =
                if (mode == WonderSwanPivotInput.HORIZONTAL) {
                    listOf(KeyEvent.KEYCODE_BUTTON_R2, KeyEvent.KEYCODE_BUTTON_R1)
                } else {
                    listOf(KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BUTTON_X)
                }
            val expectedA =
                if (mode == WonderSwanPivotInput.HORIZONTAL) KeyEvent.KEYCODE_BUTTON_A else KeyEvent.KEYCODE_BUTTON_L1

            fun update(
                x: Float,
                y: Float,
            ) = mode.dispatch(x, y) { code, pressed ->
                gate.setSource(code, "touch:ws-cursor", pressed)
            }
            update(1f, -1f)
            update(1f, -1f)
            gate.toggleHold(mode.a)
            update(0f, 0f)
            gate.clear()
            assertEquals(
                expectedDiagonal.map { KeyEvent.ACTION_DOWN to it } +
                    (KeyEvent.ACTION_DOWN to expectedA) +
                    expectedDiagonal.map { KeyEvent.ACTION_UP to it } +
                    (KeyEvent.ACTION_UP to expectedA),
                messages,
            )
        }
        assertEquals(
            WonderSwanPivotInput.HORIZONTAL,
            WonderSwanPivotInput.forController(TouchControllerID.WS_LANDSCAPE),
        )
        assertEquals(WonderSwanPivotInput.VERTICAL, WonderSwanPivotInput.forController(TouchControllerID.WS_PORTRAIT))
    }
}
