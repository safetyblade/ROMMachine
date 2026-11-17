package com.swordfish.lemuroid.app.shared.game.pc

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.swordfish.libretrodroid.GLRetroView

/** PC-only input protocol carried over the queued host input APIs. */
internal class PcHardwareInput {
    private var lastX: Float? = null
    private var lastY: Float? = null
    private var buttons = 0

    fun key(
        event: KeyEvent,
        view: GLRetroView,
    ): Boolean {
        if (!event.isFromSource(InputDevice.SOURCE_KEYBOARD) ||
            event.isFromSource(InputDevice.SOURCE_GAMEPAD)
        ) {
            return false
        }
        val code = retroKey(event.keyCode) ?: return false
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0) view.sendKeyEvent(KeyEvent.ACTION_DOWN, KEY_BASE + code)
            }
            KeyEvent.ACTION_UP -> view.sendKeyEvent(KeyEvent.ACTION_UP, KEY_BASE + code)
            else -> return false
        }
        return true
    }

    fun mouse(
        event: MotionEvent,
        view: GLRetroView,
    ): Boolean {
        if (!event.isFromSource(InputDevice.SOURCE_MOUSE)) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_HOVER_ENTER -> {
                lastX = event.x
                lastY = event.y
            }
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_HOVER_MOVE -> {
                val dx = lastX?.let { event.x - it } ?: 0f
                val dy = lastY?.let { event.y - it } ?: 0f
                if (dx.isFinite() && dy.isFinite()) view.sendMotionEvent(MOUSE_SOURCE, dx, dy)
                lastX = event.x
                lastY = event.y
            }
            MotionEvent.ACTION_HOVER_EXIT -> {
                lastX = null
                lastY = null
            }
            MotionEvent.ACTION_CANCEL -> {
                release(view)
                return true
            }
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP,
            MotionEvent.ACTION_BUTTON_PRESS, MotionEvent.ACTION_BUTTON_RELEASE,
            -> {
                lastX = event.x
                lastY = event.y
            }
            else -> return false
        }
        val state = event.buttonState
        for ((index, mask) in MOUSE_BUTTONS.withIndex()) {
            if ((state and mask) != (buttons and mask)) {
                val action = if ((state and mask) != 0) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP
                view.sendKeyEvent(action, MOUSE_BUTTON_BASE + index)
            }
        }
        buttons = state
        return true
    }

    fun release(view: GLRetroView?) {
        view?.sendKeyEvent(KeyEvent.ACTION_DOWN, CLEAR)
        buttons = 0
        lastX = null
        lastY = null
    }

    companion object {
        // The stock console host does not implement the PC native input protocol.
        // Enable only in a separately verified PC runtime, never in the console APK.
        const val NATIVE_PROTOCOL_AVAILABLE = false
        private const val KEY_BASE = 10000
        private const val MOUSE_BUTTON_BASE = 10600
        private const val CLEAR = 10610
        private const val MOUSE_SOURCE = 5
        private val MOUSE_BUTTONS =
            intArrayOf(MotionEvent.BUTTON_PRIMARY, MotionEvent.BUTTON_SECONDARY, MotionEvent.BUTTON_TERTIARY)

        // Libretro RETROK values, not Android gamepad IDs.
        internal fun retroKey(code: Int): Int? =
            when (code) {
                in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z -> 'a'.code + code - KeyEvent.KEYCODE_A
                in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> '0'.code + code - KeyEvent.KEYCODE_0
                in KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12 -> 282 + code - KeyEvent.KEYCODE_F1
                in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> 256 + code - KeyEvent.KEYCODE_NUMPAD_0
                KeyEvent.KEYCODE_DEL -> 8
                KeyEvent.KEYCODE_TAB -> 9
                KeyEvent.KEYCODE_ENTER -> 13
                KeyEvent.KEYCODE_ESCAPE -> 27
                KeyEvent.KEYCODE_SPACE -> 32
                KeyEvent.KEYCODE_APOSTROPHE -> 39
                KeyEvent.KEYCODE_COMMA -> 44
                KeyEvent.KEYCODE_MINUS -> 45
                KeyEvent.KEYCODE_PERIOD -> 46
                KeyEvent.KEYCODE_SLASH -> 47
                KeyEvent.KEYCODE_SEMICOLON -> 59
                KeyEvent.KEYCODE_EQUALS -> 61
                KeyEvent.KEYCODE_LEFT_BRACKET -> 91
                KeyEvent.KEYCODE_BACKSLASH -> 92
                KeyEvent.KEYCODE_RIGHT_BRACKET -> 93
                KeyEvent.KEYCODE_GRAVE -> 96
                KeyEvent.KEYCODE_FORWARD_DEL -> 127
                KeyEvent.KEYCODE_DPAD_UP -> 273
                KeyEvent.KEYCODE_DPAD_DOWN -> 274
                KeyEvent.KEYCODE_DPAD_RIGHT -> 275
                KeyEvent.KEYCODE_DPAD_LEFT -> 276
                KeyEvent.KEYCODE_INSERT -> 277
                KeyEvent.KEYCODE_MOVE_HOME -> 278
                KeyEvent.KEYCODE_MOVE_END -> 279
                KeyEvent.KEYCODE_PAGE_UP -> 280
                KeyEvent.KEYCODE_PAGE_DOWN -> 281
                KeyEvent.KEYCODE_NUM_LOCK -> 300
                KeyEvent.KEYCODE_CAPS_LOCK -> 301
                KeyEvent.KEYCODE_SCROLL_LOCK -> 302
                KeyEvent.KEYCODE_SHIFT_RIGHT -> 303
                KeyEvent.KEYCODE_SHIFT_LEFT -> 304
                KeyEvent.KEYCODE_CTRL_RIGHT -> 305
                KeyEvent.KEYCODE_CTRL_LEFT -> 306
                KeyEvent.KEYCODE_ALT_RIGHT -> 307
                KeyEvent.KEYCODE_ALT_LEFT -> 308
                KeyEvent.KEYCODE_NUMPAD_DOT -> 266
                KeyEvent.KEYCODE_NUMPAD_DIVIDE -> 267
                KeyEvent.KEYCODE_NUMPAD_MULTIPLY -> 268
                KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> 269
                KeyEvent.KEYCODE_NUMPAD_ADD -> 270
                KeyEvent.KEYCODE_NUMPAD_ENTER -> 271
                else -> null
            }
    }
}
