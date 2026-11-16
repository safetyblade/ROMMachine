package com.swordfish.lemuroid.app.shared.input

import android.content.Context
import android.content.SharedPreferences
import android.hardware.input.InputManager
import android.view.InputDevice
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import kotlinx.coroutines.launch

/**
 * Observes real Android gameplay controllers, excluding basic TV remotes and keyboards.
 * The legacy enabled/disabled preference is honoured; the source decoder remains unchanged.
 * A per-device descriptor, rather than its displayed name, identifies multiple identical pads.
 */
class GameplayControllerPresence(context: Context) : InputManager.InputDeviceListener {
    private val manager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
    private val preferences = SharedPreferencesHelper.getSharedPreferences(context)
    private var listener: ((List<InputDevice>) -> Unit)? = null
    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            val enabledPreferenceChanged =
                InputDevice.getDeviceIds().any { id ->
                    val device = InputDevice.getDevice(id)
                    device != null && InputDeviceManager.computeEnabledGamePadPreference(device) == key
                }
            if (enabledPreferenceChanged) notifyCurrent()
        }
    private var registered = false
    private val profileStore = ControllerProfileStore(context)
    private val scope =
        kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() +
                kotlinx.coroutines.Dispatchers.Main.immediate,
        )
    private var profileJob: kotlinx.coroutines.Job? = null
    private val slots = ControllerPortRegistry(7)
    private var ports = emptyMap<Int, Int>()

    fun port(deviceId: Int): Int? = ports[deviceId]

    fun start(onChanged: (List<InputDevice>) -> Unit) {
        listener = onChanged
        if (!registered) {
            manager.registerInputDeviceListener(this, null)
            preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
            registered = true
        }
        profileJob?.cancel()
        profileJob = scope.launch { profileStore.changes().collect { notifyCurrent() } }
        notifyCurrent()
    }

    fun stop() {
        if (registered) {
            manager.unregisterInputDeviceListener(this)
            preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        }
        registered = false
        profileJob?.cancel()
        profileJob = null
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
        val devices = connected()
        slots.update(devices.map { ControllerPortRegistry.Device(it.id, it.descriptor) })
        devices.filter { device -> devices.count { it.descriptor == device.descriptor } == 1 }.forEach { device ->
            runCatching { profileStore.read(device.descriptor).playerPort }.getOrNull()?.let {
                slots.assign(device.id, it)
            }
        }
        ports = slots.update(devices.map { ControllerPortRegistry.Device(it.id, it.descriptor) })
        listener?.invoke(devices)
    }

    override fun onInputDeviceAdded(deviceId: Int) = notifyCurrent()

    override fun onInputDeviceRemoved(deviceId: Int) = notifyCurrent()

    override fun onInputDeviceChanged(deviceId: Int) = notifyCurrent()
}
