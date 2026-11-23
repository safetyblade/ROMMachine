package com.swordfish.lemuroid.app.shared.game.viewmodel

import android.view.KeyEvent
import com.swordfish.touchinput.radial.settings.TouchControllerID

/** RetroPad mappings from the two input tables in the bundled Beetle Cygne core. */
internal enum class WonderSwanPivotInput(
    val source: Int,
    val primaryLabel: String,
    val secondaryLabel: String,
    val a: Int,
    val b: Int,
    private val up: Int,
    private val right: Int,
    private val down: Int,
    private val left: Int,
) {
    HORIZONTAL(
        41, "X cursor", "Y cursor",
        KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B,
        KeyEvent.KEYCODE_BUTTON_R2, KeyEvent.KEYCODE_BUTTON_R1,
        KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_L1,
    ),
    VERTICAL(
        42, "Y cursor", "X cursor",
        KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1,
        KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BUTTON_X,
        KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B,
    ),
    ;

    val faceKeys: Set<Int> get() = setOf(a, b)
    val cursorKeys: Set<Int> get() = setOf(up, right, down, left)

    fun dispatch(
        x: Float,
        y: Float,
        update: (Int, Boolean) -> Unit,
    ) {
        update(up, y < 0f)
        update(right, x > 0f)
        update(down, y > 0f)
        update(left, x < 0f)
    }

    companion object {
        val HORIZONTAL_SOURCE: Int get() = HORIZONTAL.source
        val VERTICAL_SOURCE: Int get() = VERTICAL.source

        fun forController(controller: TouchControllerID?): WonderSwanPivotInput? =
            when (controller) {
                TouchControllerID.WS_LANDSCAPE -> HORIZONTAL
                TouchControllerID.WS_PORTRAIT -> VERTICAL
                else -> null
            }
    }
}
