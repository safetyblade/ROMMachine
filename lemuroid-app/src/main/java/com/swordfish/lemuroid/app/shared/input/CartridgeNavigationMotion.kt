package com.swordfish.lemuroid.app.shared.input

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.abs

/** Translate a deliberate stick/hat displacement into one normal Android focus step. */
internal class CartridgeNavigationMotion {
    private val directions = mutableMapOf<Int, Int>()

    fun clear() = directions.clear()

    fun dispatch(
        event: MotionEvent,
        sendKey: (KeyEvent) -> Unit,
    ): Boolean {
        if (!event.isFromSource(InputDevice.SOURCE_JOYSTICK) || event.actionMasked != MotionEvent.ACTION_MOVE) {
            return false
        }
        val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
        val usingHat = abs(hatX) >= 0.5f || abs(hatY) >= 0.5f
        val x = if (usingHat) hatX else event.getAxisValue(MotionEvent.AXIS_X)
        val y = if (usingHat) hatY else event.getAxisValue(MotionEvent.AXIS_Y)
        val threshold = if (event.deviceId in directions) 0.35f else 0.6f
        val next =
            when {
                maxOf(abs(x), abs(y)) < threshold -> null
                abs(x) >= abs(y) -> if (x > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
                else -> if (y > 0) KeyEvent.KEYCODE_DPAD_DOWN else KeyEvent.KEYCODE_DPAD_UP
            }
        if (next == null) {
            directions.remove(event.deviceId)
        } else if (directions.put(event.deviceId, next) != next) {
            for (action in listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
                sendKey(
                    KeyEvent(
                        event.eventTime, event.eventTime, action, next,
                        0, 0, event.deviceId, 0, 0, InputDevice.SOURCE_DPAD,
                    ),
                )
            }
        }
        return true
    }
}
