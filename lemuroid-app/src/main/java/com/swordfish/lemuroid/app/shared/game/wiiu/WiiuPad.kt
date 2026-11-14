package com.swordfish.lemuroid.app.shared.game.wiiu

import android.view.KeyEvent

/** Pinned Libretro RetroPad ABI; hardware and labelled touch controls remain distinct. */
object WiiuPad {
    val retroToXenia = intArrayOf(4, 6, 8, 9, 1, 3, 0, 2, 5, 7, 10, 11, 14, 15, 12, 13)

    fun touchKey(key: Int): Int =
        when (key) {
            KeyEvent.KEYCODE_BUTTON_A -> KeyEvent.KEYCODE_BUTTON_B
            KeyEvent.KEYCODE_BUTTON_B -> KeyEvent.KEYCODE_BUTTON_A
            KeyEvent.KEYCODE_BUTTON_X -> KeyEvent.KEYCODE_BUTTON_Y
            KeyEvent.KEYCODE_BUTTON_Y -> KeyEvent.KEYCODE_BUTTON_X
            // The retained Classic layout labels its first shoulders ZL/ZR.
            KeyEvent.KEYCODE_BUTTON_L1 -> KeyEvent.KEYCODE_BUTTON_L2
            KeyEvent.KEYCODE_BUTTON_L2 -> KeyEvent.KEYCODE_BUTTON_L1
            KeyEvent.KEYCODE_BUTTON_R1 -> KeyEvent.KEYCODE_BUTTON_R2
            KeyEvent.KEYCODE_BUTTON_R2 -> KeyEvent.KEYCODE_BUTTON_R1
            else -> key
        }

    fun analog(native: IntArray): IntArray {
        require(native.size == 24)
        return intArrayOf(
            native[16] + native[18],
            -(native[17] + native[19]),
            native[20] + native[22],
            -(native[21] + native[23]),
        )
    }
}
