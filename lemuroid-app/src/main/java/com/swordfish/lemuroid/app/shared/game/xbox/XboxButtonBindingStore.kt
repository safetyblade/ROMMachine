package com.swordfish.lemuroid.app.shared.game.xbox

import android.content.Context
import android.view.InputDevice
import android.view.KeyEvent
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import java.security.MessageDigest

/**
 * Additive native-Xbox physical button mappings. These never modify the original
 * Xbox input contract, emulator configuration, existing RetroPad preferences or saves.
 * Lookup precedence: this cartridge > this physical device > original Android key.
 */
class XboxButtonBindingStore(context: Context) {
    private val prefs = SharedPreferencesHelper.getSharedPreferences(context)

    fun output(
        device: InputDevice?,
        source: Int,
        cartridge: String?,
    ): Int {
        val descriptor = device?.descriptor ?: return source
        val specific = key(descriptor, cartridge, source)
        val deviceDefault = key(descriptor, null, source)
        return when {
            cartridge != null && prefs.contains(specific) -> prefs.getInt(specific, source)
            prefs.contains(deviceDefault) -> prefs.getInt(deviceDefault, source)
            else -> source
        }
    }

    fun save(
        device: InputDevice,
        source: Int,
        target: Int,
        cartridge: String?,
    ): Boolean {
        if (source !in editableKeys || target !in editableKeys) return false
        return prefs.edit().putInt(key(device.descriptor, cartridge, source), target).commit()
    }

    fun reset(
        device: InputDevice,
        cartridge: String?,
    ): Boolean {
        val prefix = scope(device.descriptor, cartridge)
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(prefix) }.forEach { editor.remove(it) }
        return editor.commit()
    }

    fun mappedSources(
        device: InputDevice,
        target: Int,
        cartridge: String?,
    ): List<Int> = editableKeys.filter { output(device, it, cartridge) == target }

    private fun key(
        descriptor: String,
        cartridge: String?,
        source: Int,
    ) = scope(descriptor, cartridge) + source

    private fun scope(
        descriptor: String,
        cartridge: String?,
    ): String {
        val raw = descriptor + "\n" + (cartridge ?: "DEVICE")
        val digest =
            MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it.toInt() and 255) }
        return "cartdeck.xbox.input.v1.$digest."
    }

    companion object {
        val editableKeys =
            listOf(
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_SELECT,
            KeyEvent.KEYCODE_BUTTON_THUMBL, KeyEvent.KEYCODE_BUTTON_THUMBR,
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
        )
    }
}

/**
 * Track every raw button's owner before remapping. Releasing one source must not
 * release an Xbox button still held by another source; repeated downs are ignored.
 */
class XboxHardwareKeyRouter {
    private val held = mutableMapOf<Pair<Int, Int>, Int>()

    fun route(
        deviceId: Int,
        source: Int,
        pressed: Boolean,
        mapped: Int,
    ): Pair<Int, Boolean>? {
        val owner = deviceId to source
        if (pressed) {
            if (held.containsKey(owner)) return null
            val alreadyHeld = held.values.count { it == mapped }
            held[owner] = mapped
            return if (alreadyHeld == 0) mapped to true else null
        }
        val existing = held.remove(owner) ?: return null
        return if (existing !in held.values) existing to false else null
    }

    fun removeDisconnected(activeDeviceIds: Set<Int>): List<Pair<Int, Boolean>> {
        val released = mutableListOf<Pair<Int, Boolean>>()
        held.keys.filter { it.first !in activeDeviceIds }.toList().forEach { owner ->
            val mapped = held.remove(owner) ?: return@forEach
            if (mapped !in held.values) released += mapped to false
        }
        return released
    }

    fun clear() = held.clear()
}
