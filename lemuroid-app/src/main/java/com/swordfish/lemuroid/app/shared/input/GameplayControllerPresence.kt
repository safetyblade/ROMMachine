package com.swordfish.lemuroid.app.shared.input

import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.KeyEvent
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

/**
 * Observes real Android gameplay controllers, excluding basic TV remotes and keyboards.
 * The legacy enabled/disabled preference is honoured; the source decoder remains unchanged.
 * A per-device descriptor, rather than its displayed name, identifies multiple identical pads.
 */
class GameplayControllerPresence(context: Context) : InputManager.InputDeviceListener {
    private val manager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
    private val preferences = SharedPreferencesHelper.getSharedPreferences(context)
    private var listener: ((List<InputDevice>) -> Unit)? = null
    private var registered = false

    fun start(onChanged: (List<InputDevice>) -> Unit) {
        listener = onChanged
        if (!registered) {
            manager.registerInputDeviceListener(this, null)
            registered = true
        }
        notifyCurrent()
    }

    fun stop() {
        if (registered) manager.unregisterInputDeviceListener(this)
        registered = false
        listener = null
    }

    fun connected(): List<InputDevice> =
        InputDevice.getDeviceIds()
            .mapNotNull { InputDevice.getDevice(it) }
            .filter(::isEnabledGamepad)
            .sortedWith(compareBy({ it.controllerNumber }, { it.id }))

    private fun isEnabledGamepad(device: InputDevice): Boolean {
        if (!ControllerHardwareProfile.isGameplayPad(device)) return false
        // New native hosts did not previously consult this legacy enabled preference.
        // Default to enabled for capable hardware but respect an explicit user opt-out.
        return preferences.getBoolean(InputDeviceManager.computeEnabledGamePadPreference(device), true)
    }

    private fun notifyCurrent() {
        listener?.invoke(connected())
    }

    override fun onInputDeviceAdded(deviceId: Int) = notifyCurrent()

    override fun onInputDeviceRemoved(deviceId: Int) = notifyCurrent()

    override fun onInputDeviceChanged(deviceId: Int) = notifyCurrent()
}

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
