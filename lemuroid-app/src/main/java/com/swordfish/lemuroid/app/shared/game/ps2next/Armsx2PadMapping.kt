package com.swordfish.lemuroid.app.shared.game.ps2next

import android.view.KeyEvent
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Exact native ABI indices from pinned ARMSX2 native-lib.cpp::applyPadButton().
 * Return null for unknown controls: upstream defaults unknown codes to Cross.
 */
object Armsx2PadMapping {
    data class PadState(
        val nativeCode: Int,
        val range: Int,
        val pressed: Boolean,
    )

    fun button(
        keyCode: Int,
        pressed: Boolean,
    ): PadState? =
        if (keyCode in DIRECT_KEYS) {
            // Upstream interprets range=0 and pressed=true as full pressure.
            PadState(keyCode, 0, pressed)
        } else {
            null
        }

    /** Pressure uses the same 0..32767 ABI as the native DualShock pad. */
    fun trigger(value: Float, right: Boolean): PadState {
        val pressure = if (value.isFinite()) value.coerceIn(0f, 1f) else 0f
        val magnitude = (pressure * 32767f).roundToInt()
        return PadState(
            if (right) KeyEvent.KEYCODE_BUTTON_R2 else KeyEvent.KEYCODE_BUTTON_L2,
            magnitude,
            magnitude > 0,
        )
    }

    /** Always emit release events too; otherwise changing stick direction can remain latched. */
    fun stick(
        x: Float,
        y: Float,
        right: Boolean,
    ): List<PadState> {
        val horizontal = if (x.isFinite()) x.coerceIn(-1f, 1f) else 0f
        val vertical = if (y.isFinite()) y.coerceIn(-1f, 1f) else 0f
        val first = if (right) 120 else 110

        fun magnitude(value: Float): Int = (abs(value) * 32767f).roundToInt().coerceIn(0, 32767)
        val up = magnitude(vertical.coerceAtMost(0f))
        val down = magnitude(vertical.coerceAtLeast(0f))
        val left = magnitude(horizontal.coerceAtMost(0f))
        val east = magnitude(horizontal.coerceAtLeast(0f))
        return listOf(
            PadState(first, up, up > 0),
            PadState(first + 1, east, east > 0),
            PadState(first + 2, down, down > 0),
            PadState(first + 3, left, left > 0),
        )
    }

    private val DIRECT_KEYS =
        setOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_X,
            KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_SELECT,
            KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_L2,
            KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_BUTTON_THUMBL,
            KeyEvent.KEYCODE_BUTTON_THUMBR,
        )
}
