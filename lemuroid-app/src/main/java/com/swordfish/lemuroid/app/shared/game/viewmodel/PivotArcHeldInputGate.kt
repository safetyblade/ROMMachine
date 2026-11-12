package com.swordfish.lemuroid.app.shared.game.viewmodel

import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Source-aware arbitration for PivotArc's two optional latched face buttons.
 *
 * This gate is disabled in Standard mode. When enabled, the emulator receives
 * exactly one down/up pair for each transition of the OR of touch, physical
 * and latched sources. Removing one source cannot release another source.
 */
class PivotArcHeldInputGate(
    private val send: (action: Int, keyCode: Int) -> Unit,
) {
    private val sources = mutableMapOf<Int, MutableSet<String>>()
    private val mutableHolds = MutableStateFlow<Set<Int>>(emptySet())

    val holds: StateFlow<Set<Int>> = mutableHolds

    private var enabled = false

    @Synchronized
    fun isEnabled(): Boolean = enabled

    @Synchronized
    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        clear()
        enabled = value
    }

    /** Returns false when Standard must handle the event unchanged. */
    @Synchronized
    fun setSource(
        keyCode: Int,
        source: String,
        pressed: Boolean,
    ): Boolean {
        if (!enabled || keyCode !in LATCHABLE_KEYS) return false
        val previous = sources[keyCode]?.isNotEmpty() == true
        val owners = sources.getOrPut(keyCode) { mutableSetOf() }
        if (pressed) owners.add(source) else owners.remove(source)
        if (owners.isEmpty()) sources.remove(keyCode)
        val now = sources[keyCode]?.isNotEmpty() == true
        if (previous != now) {
            send(if (now) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, keyCode)
        }
        return true
    }

    @Synchronized
    fun toggleHold(keyCode: Int) {
        if (!enabled || keyCode !in LATCHABLE_KEYS) return
        val source = HOLD_SOURCE
        val active = sources[keyCode]?.contains(source) == true
        setSource(keyCode, source, !active)
        mutableHolds.value =
            LATCHABLE_KEYS.filterTo(mutableSetOf()) { key ->
                sources[key]?.contains(HOLD_SOURCE) == true
            }
    }

    /** Release before a controller reset, background, unload or mode switch. */
    @Synchronized
    fun clear() {
        sources.filterValues { it.isNotEmpty() }.keys.forEach { key ->
            send(KeyEvent.ACTION_UP, key)
        }
        sources.clear()
        mutableHolds.value = emptySet()
    }

    companion object {
        private const val HOLD_SOURCE = "pivotarc-latch"
        private val LATCHABLE_KEYS =
            setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B)
    }
}
