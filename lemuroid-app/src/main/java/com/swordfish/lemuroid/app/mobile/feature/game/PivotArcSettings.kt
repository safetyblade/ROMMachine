package com.swordfish.lemuroid.app.mobile.feature.game

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

enum class CartdeckLayoutMode { STANDARD, ONE_HAND, GRIPARC }

enum class ControlsOrientation { AUTOMATIC, PORTRAIT, LANDSCAPE }

enum class PivotArcHand { RIGHT, LEFT }

data class PivotArcSettings(
    val mode: CartdeckLayoutMode = CartdeckLayoutMode.STANDARD,
    val hand: PivotArcHand = PivotArcHand.RIGHT,
    val offsetXdp: Float = 0f,
    val offsetYdp: Float = 0f,
    val pspAnalogPrimary: Boolean = false,
    val orientation: ControlsOrientation = ControlsOrientation.AUTOMATIC,
)

/**
 * Independent, additive preferences. No edits to ROM paths, SRAM, save states,
 * PadKit radial preferences or per-cartridge settings. An unknown future schema
 * is treated as Standard without writing over its values.
 */
class PivotArcSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun read(): PivotArcSettings {
        if (preferences.getInt("schema", VERSION) != VERSION) return PivotArcSettings()
        val mode =
            runCatching {
                CartdeckLayoutMode.valueOf(preferences.getString("mode", "STANDARD") ?: "STANDARD")
            }.getOrDefault(CartdeckLayoutMode.STANDARD)
        val hand =
            runCatching {
                PivotArcHand.valueOf(preferences.getString("hand", "RIGHT") ?: "RIGHT")
            }.getOrDefault(PivotArcHand.RIGHT)

        fun readOffset(key: String): Float =
            runCatching { preferences.getFloat(key, 0f) }
                .getOrDefault(0f).takeIf { it.isFinite() && it in -1000f..1000f } ?: 0f
        val pspAnalogPrimary = runCatching { preferences.getBoolean("psp_analog_primary", false) }.getOrDefault(false)
        val orientation =
            runCatching {
                ControlsOrientation.valueOf(preferences.getString("orientation", null).orEmpty())
            }.getOrDefault(ControlsOrientation.AUTOMATIC)
        // Earlier builds persisted the preview's applied translation. It is not a chassis-relative offset.
        return PivotArcSettings(mode, hand, readOffset("dx"), readOffset(CHASSIS_Y), pspAnalogPrimary, orientation)
    }

    fun save(value: PivotArcSettings) {
        require(value.offsetXdp.isFinite() && value.offsetYdp.isFinite())
        preferences.edit()
            .putInt("schema", VERSION)
            .putString("mode", value.mode.name)
            .putString("hand", value.hand.name)
            .putFloat("dx", value.offsetXdp)
            .putFloat(CHASSIS_Y, value.offsetYdp)
            .putBoolean("psp_analog_primary", value.pspAnalogPrimary)
            .putString("orientation", value.orientation.name)
            .apply()
    }

    fun resetPosition() {
        save(read().copy(offsetXdp = 0f, offsetYdp = 0f))
    }

    fun register(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregister(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        private const val PREF_NAME = "cartdeck_pivotarc_v1"
        private const val VERSION = 1
        private const val CHASSIS_Y = "chassis_dy"
    }
}

@Composable
fun rememberPivotArcSettings(): State<PivotArcSettings> {
    val context = LocalContext.current
    val store = remember(context) { PivotArcSettingsStore(context) }
    val state = remember(store) { mutableStateOf(store.read()) }
    DisposableEffect(store) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                state.value = store.read()
            }
        store.register(listener)
        onDispose { store.unregister(listener) }
    }
    return state
}
