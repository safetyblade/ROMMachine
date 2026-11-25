package com.swordfish.lemuroid.app.shared.game.viewmodel

import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Shared source-ownership arbiter for PivotArc holds and GripArc momentary
 * chords. Existing PadKit and physical controller events are native owners.
 * One DOWN is delivered on the first owner and one UP on the final release.
 */
class PivotArcHeldInputGate(
    private val send: (action: Int, keyCode: Int) -> Unit,
) {
    private val sources = mutableMapOf<Int, MutableSet<String>>()
    private val pressedGripSlots = mutableMapOf<Int, Set<Int>>()
    private val mutableHolds = MutableStateFlow<Set<Int>>(emptySet())
    val holds: StateFlow<Set<Int>> = mutableHolds

    private var pivotEnabled = false
    private var gripEnabled = false
    private var trayKeys = emptySet<Int>()
    private var pivotFaceKeys = DEFAULT_FACE_KEYS
    private var pivotCursorKeys = emptySet<Int>()

    @Synchronized
    fun isEnabled(): Boolean = pivotEnabled

    @Synchronized
    fun setEnabled(
        value: Boolean,
        faceKeys: Set<Int> = DEFAULT_FACE_KEYS,
        cursorKeys: Set<Int> = emptySet(),
    ) {
        require((faceKeys + cursorKeys).all { it in MANAGED_KEYS })
        if (pivotEnabled == value && pivotFaceKeys == faceKeys && pivotCursorKeys == cursorKeys) return
        clear()
        pivotEnabled = value
        pivotFaceKeys = faceKeys
        pivotCursorKeys = cursorKeys
    }

    @Synchronized
    fun setGripEnabled(value: Boolean) {
        if (gripEnabled == value) return
        clear()
        gripEnabled = value
    }

    @Synchronized
    fun setStandardHoldKeys(keys: Set<Int>) {
        val supported = keys.filter { it in MANAGED_KEYS }.take(3).toSet()
        if (supported == trayKeys) return
        clear()
        trayKeys = supported
    }

    /**
     * False means Standard's original pipeline should send this input.
     * For active GripArc, all supported virtual/physical keys participate in
     * the same arbiter; PivotArc-only still intercepts only A/B as before.
     */
    @Synchronized
    fun setSource(
        keyCode: Int,
        source: String,
        pressed: Boolean,
    ): Boolean {
        val owns =
            (gripEnabled && keyCode in MANAGED_KEYS) ||
                (pivotEnabled && (keyCode in pivotFaceKeys || keyCode in pivotCursorKeys)) || keyCode in trayKeys
        if (!owns) return false
        val wasDown = sources[keyCode]?.isNotEmpty() == true
        val owners = sources.getOrPut(keyCode) { mutableSetOf() }
        if (pressed) owners.add(source) else owners.remove(source)
        if (owners.isEmpty()) sources.remove(keyCode)
        val isDown = sources[keyCode]?.isNotEmpty() == true
        if (wasDown != isDown) {
            send(if (isDown) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, keyCode)
        }
        return true
    }

    @Synchronized
    fun toggleHold(keyCode: Int) {
        val latchable = if (pivotEnabled) pivotFaceKeys else trayKeys
        if (keyCode !in latchable) return
        val active = sources[keyCode]?.contains(HOLD_SOURCE) == true
        setSource(keyCode, HOLD_SOURCE, !active)
        mutableHolds.value =
            latchable.filterTo(mutableSetOf()) { key ->
                sources[key]?.contains(HOLD_SOURCE) == true
            }
    }

    /**
     * Snapshot the chord at pointer down, so editing a mapping or hiding a
     * slot cannot leave old inputs latched. No timed macros or sequencers.
     */
    @Synchronized
    fun setGripSlot(
        slot: Int,
        pressed: Boolean,
        mapping: Set<Int> = emptySet(),
    ) {
        if (slot !in 1..4) return
        val owner = "grip:G$slot"
        val previous = pressedGripSlots.remove(slot).orEmpty()
        previous.forEach { key -> setSource(key, owner, false) }
        if (!gripEnabled || !pressed) return
        val safe = mapping.filterTo(linkedSetOf()) { it in MANAGED_KEYS }
        if (safe.isEmpty()) return
        pressedGripSlots[slot] = safe
        safe.forEach { key -> setSource(key, owner, true) }
    }

    @Synchronized
    fun releaseGripSlots() {
        pressedGripSlots.keys.toList().forEach { setGripSlot(it, false) }
    }

    @Synchronized
    fun clear() {
        sources.filterValues { it.isNotEmpty() }.keys.toList().forEach { key ->
            send(KeyEvent.ACTION_UP, key)
        }
        sources.clear()
        pressedGripSlots.clear()
        mutableHolds.value = emptySet()
    }

    companion object {
        private const val HOLD_SOURCE = "pivotarc-latch"
        private val DEFAULT_FACE_KEYS =
            setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B)
        private val MANAGED_KEYS =
            DEFAULT_FACE_KEYS +
                setOf(
                    KeyEvent.KEYCODE_BUTTON_X,
                    KeyEvent.KEYCODE_BUTTON_Y,
                    KeyEvent.KEYCODE_BUTTON_L1,
                    KeyEvent.KEYCODE_BUTTON_R1,
                    KeyEvent.KEYCODE_BUTTON_L2,
                    KeyEvent.KEYCODE_BUTTON_R2,
                    KeyEvent.KEYCODE_BUTTON_THUMBL,
                    KeyEvent.KEYCODE_BUTTON_THUMBR,
                    KeyEvent.KEYCODE_BUTTON_SELECT,
                    KeyEvent.KEYCODE_BUTTON_START,
                )
    }
}
