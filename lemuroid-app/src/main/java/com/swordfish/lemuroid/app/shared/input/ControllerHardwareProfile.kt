package com.swordfish.lemuroid.app.shared.input

import android.view.InputDevice
import android.view.KeyEvent

/** Pure controller capability vocabulary, separate from per-console native button semantics. */
object ControllerHardwareProfile {
    enum class Family { XBOX, PLAYSTATION, SWITCH, GENERIC }

    fun isGameplayPad(device: InputDevice): Boolean {
        if (device.isVirtual) return false
        val source = device.sources
        val hasGamepadSource = source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD
        val hasJoystickSource = source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
        if (!hasGamepadSource && !hasJoystickSource) return false
        val faces =
            runCatching {
                device.hasKeys(
                    KeyEvent.KEYCODE_BUTTON_A,
                    KeyEvent.KEYCODE_BUTTON_B,
                    KeyEvent.KEYCODE_BUTTON_X,
                    KeyEvent.KEYCODE_BUTTON_Y,
                ).count { it }
            }.getOrDefault(0)
        // A TV remote with just D-pad/OK/Back never qualifies as a gameplay controller.
        return faces >= 2 && (hasGamepadSource || device.motionRanges.isNotEmpty())
    }

    fun family(device: InputDevice): Family {
        val lower = device.name.lowercase()
        return when {
            "xbox" in lower || "xinput" in lower -> Family.XBOX
            "dualshock" in lower || "dualsense" in lower || "playstation" in lower -> Family.PLAYSTATION
            "switch" in lower || "pro controller" in lower || "joy-con" in lower -> Family.SWITCH
            else -> Family.GENERIC
        }
    }

    /** Right sticks vary across Android gamepad drivers: Z/RZ or RX/RY. */
    fun rightStickAxes(device: InputDevice?): Pair<Int, Int> {
        val supported = device?.motionRanges?.map { it.axis }?.toSet().orEmpty()
        return if (android.view.MotionEvent.AXIS_RX in supported && android.view.MotionEvent.AXIS_RY in supported &&
            !(android.view.MotionEvent.AXIS_Z in supported && android.view.MotionEvent.AXIS_RZ in supported)
        ) {
            android.view.MotionEvent.AXIS_RX to android.view.MotionEvent.AXIS_RY
        } else {
            android.view.MotionEvent.AXIS_Z to android.view.MotionEvent.AXIS_RZ
        }
    }
}
