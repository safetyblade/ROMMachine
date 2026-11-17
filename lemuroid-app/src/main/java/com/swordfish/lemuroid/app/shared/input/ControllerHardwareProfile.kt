package com.swordfish.lemuroid.app.shared.input

import android.view.InputDevice
import android.view.KeyEvent

/** Pure controller capability vocabulary, separate from per-console native button semantics. */
object ControllerHardwareProfile {
    enum class Family { XBOX, PLAYSTATION, SWITCH, GENERIC, UNKNOWN }

    fun bindable(code: Int): Boolean =
        code !in
            setOf(
                KeyEvent.KEYCODE_UNKNOWN, KeyEvent.KEYCODE_BUTTON_MODE,
                KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME,
            ) &&
            (
                code in KeyEvent.KEYCODE_BUTTON_A..KeyEvent.KEYCODE_BUTTON_MODE ||
                    code in KeyEvent.KEYCODE_BUTTON_1..KeyEvent.KEYCODE_BUTTON_16 ||
                    code in KeyEvent.KEYCODE_DPAD_UP..KeyEvent.KEYCODE_DPAD_RIGHT
            )

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
        val numbered =
            runCatching {
                device.hasKeys(*(KeyEvent.KEYCODE_BUTTON_1..KeyEvent.KEYCODE_BUTTON_16).toList().toIntArray())
                    .count { it }
            }.getOrDefault(0)
        // A TV remote with just D-pad/OK/Back never qualifies as a gameplay controller.
        val joystickAxes = device.motionRanges.any {
            it.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
        }
        return qualifies(device.isVirtual, hasGamepadSource, hasJoystickSource, maxOf(faces, numbered), joystickAxes)
    }

    fun qualifies(
        virtual: Boolean,
        gamepadSource: Boolean,
        joystickSource: Boolean,
        faceButtons: Int,
        hasAxes: Boolean,
    ): Boolean = !virtual && (gamepadSource || joystickSource) && faceButtons >= 2 && (gamepadSource || hasAxes)

    fun family(device: InputDevice): Family = family(device.name, device.vendorId, device.productId)

    fun family(
        name: String,
        vendor: Int,
        product: Int,
    ): Family =
        when (vendor) {
            0x045e -> Family.XBOX
            0x054c -> Family.PLAYSTATION
            0x057e -> Family.SWITCH
            else -> family(name)
        }

    fun family(name: String): Family {
        val lower = name.lowercase()
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
