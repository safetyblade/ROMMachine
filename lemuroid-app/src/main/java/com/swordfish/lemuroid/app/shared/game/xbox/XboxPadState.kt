package com.swordfish.lemuroid.app.shared.game.xbox

import android.view.InputDevice
import android.view.KeyEvent
import com.swordfish.lemuroid.app.shared.input.ControllerHardwareProfile
import android.view.MotionEvent
import kotlin.math.abs

/** Native NcPadState: digital D-pad/utilities, six analog face buttons, two triggers. */
class XboxPadState {
    private val keys = mutableSetOf<Int>()
    private var hat = 0
    private var leftTrigger = 0
    private var rightTrigger = 0
    var lx = 0
        private set
    var ly = 0
        private set
    var rx = 0
        private set
    var ry = 0
        private set
    val buttons: Int
        get() = keys.fold(hat) { mask, key -> mask or (digital[key] ?: 0) }
    val analog: Long
        get() {
            var result = 0L
            keys.forEach { key -> face[key]?.let { result = result or (255L shl (8 * it)) } }
            val lt = if (KeyEvent.KEYCODE_BUTTON_L2 in keys) 255 else leftTrigger
            val rt = if (KeyEvent.KEYCODE_BUTTON_R2 in keys) 255 else rightTrigger
            return result or (lt.toLong() shl 48) or (rt.toLong() shl 56)
        }

    fun key(
        code: Int,
        down: Boolean,
    ): Boolean {
        val trigger = code == KeyEvent.KEYCODE_BUTTON_L2 || code == KeyEvent.KEYCODE_BUTTON_R2
        if (code !in digital && code !in face && !trigger) return false
        if (down) keys.add(code) else keys.remove(code)
        return true
    }

    fun motion(
        axis: (Int) -> Float,
        device: InputDevice? = null,
    ) {
        val (rightX, rightY) = ControllerHardwareProfile.rightStickAxes(device)
        lx = stick(axis(MotionEvent.AXIS_X))
        ly = -stick(axis(MotionEvent.AXIS_Y))
        rx = stick(axis(rightX))
        ry = -stick(axis(rightY))
        leftTrigger = trigger(axis(MotionEvent.AXIS_LTRIGGER), axis(MotionEvent.AXIS_BRAKE))
        rightTrigger = trigger(axis(MotionEvent.AXIS_RTRIGGER), axis(MotionEvent.AXIS_GAS))
        val hx = axis(MotionEvent.AXIS_HAT_X)
        val hy = axis(MotionEvent.AXIS_HAT_Y)
        hat = (
            if (hx < -.5f) {
                4
            } else if (hx > .5f) {
                8
            } else {
                0
            }
        ) or
            (
                if (hy < -.5f) {
                    1
                } else if (hy > .5f) {
                    2
                } else {
                    0
                }
            )
    }

    /** PadKit directions are Cartesian (up positive), matching the native pad integer API. */
    fun direction(
        source: Int,
        x: Float,
        y: Float,
    ) {
        when (source) {
            0 ->
                hat = (
                    if (x < -.5f) {
                        4
                    } else if (x > .5f) {
                        8
                    } else {
                        0
                    }
                ) or
                    (
                        if (y > .5f) {
                            1
                        } else if (y < -.5f) {
                            2
                        } else {
                            0
                        }
                    )
            1 -> {
                lx = stick(x)
                ly = stick(y)
            }
            2 -> {
                rx = stick(x)
                ry = stick(y)
            }
        }
    }

    fun clear() {
        keys.clear()
        hat = 0
        leftTrigger = 0
        rightTrigger = 0
        lx = 0
        ly = 0
        rx = 0
        ry = 0
    }

    private fun trigger(
        first: Float,
        second: Float,
    ) = (maxOf(first, second).coerceIn(0f, 1f) * 255).toInt()

    private fun stick(value: Float) = if (abs(value) < .15f) 0 else (value.coerceIn(-1f, 1f) * 32767).toInt()

    companion object {
        private val digital =
            mapOf(
                KeyEvent.KEYCODE_DPAD_UP to 1, KeyEvent.KEYCODE_DPAD_DOWN to 2,
                KeyEvent.KEYCODE_DPAD_LEFT to 4, KeyEvent.KEYCODE_DPAD_RIGHT to 8,
                KeyEvent.KEYCODE_BUTTON_START to 16, KeyEvent.KEYCODE_BUTTON_SELECT to 32,
                KeyEvent.KEYCODE_BACK to 32, KeyEvent.KEYCODE_BUTTON_THUMBL to 64,
                KeyEvent.KEYCODE_BUTTON_THUMBR to 128,
            )
        private val face =
            mapOf(
                KeyEvent.KEYCODE_BUTTON_A to 0,
                KeyEvent.KEYCODE_BUTTON_B to 1,
                KeyEvent.KEYCODE_BUTTON_X to 2,
                KeyEvent.KEYCODE_BUTTON_Y to 3,
                KeyEvent.KEYCODE_BUTTON_R1 to 4,
                KeyEvent.KEYCODE_BUTTON_L1 to 5,
            )
    }
}
