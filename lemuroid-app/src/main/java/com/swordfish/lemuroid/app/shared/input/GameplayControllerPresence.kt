package com.swordfish.lemuroid.app.shared.input

import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
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
            .map { InputDevice.getDevice(it) }
            .filterNotNull()
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
