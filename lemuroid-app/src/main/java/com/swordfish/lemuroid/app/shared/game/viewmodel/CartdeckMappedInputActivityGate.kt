package com.swordfish.lemuroid.app.shared.game.viewmodel

import android.view.KeyEvent

/**
 * Observe only game-bound, already-forwarded digital button transitions.
 *
 * Tracks currently held (input source, mapped button) pairs in memory solely
 * to reject repeat DOWN edges. No key history, device IDs or game activity
 * are saved, logged or sent to a background service.
 */
internal class CartdeckMappedInputActivityGate(
    private val onAcceptedPress: () -> Unit,
) {
    private val held = mutableSetOf<Pair<String, Int>>()
    private var enabled = false

    @Synchronized
    fun configure(isEnabled: Boolean) {
        if (enabled != isEnabled || !isEnabled) {
            held.clear()
        }
        enabled = isEnabled
    }

    /**
     * Call after the existing game-view key event is forwarded. The return
     * value reports a new, counted press only; it never affects input routing.
     */
    @Synchronized
    fun observe(
        source: String,
        mappedKey: Int,
        action: Int,
    ): Boolean {
        if (!enabled || source.isEmpty() || mappedKey < 0 || mappedKey in excludedKeys) return false
        val key = source to mappedKey
        if (action == KeyEvent.ACTION_UP) {
            held.remove(key)
            return false
        }
        if (action != KeyEvent.ACTION_DOWN || held.size >= MAX_HELD_BUTTONS) return false
        if (!held.add(key)) return false
        onAcceptedPress()
        return true
    }

    companion object {
        const val MAX_HELD_BUTTONS = 64

        private val excludedKeys =
            setOf(
                KeyEvent.KEYCODE_BUTTON_MODE,
                KeyEvent.KEYCODE_MENU,
                KeyEvent.KEYCODE_BACK,
            )
    }
}
