package com.swordfish.lemuroid.app.mobile.feature.game

import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.touchinput.radial.layouts.shared.ComposeTouchLayouts
import com.swordfish.touchinput.radial.ui.CartdeckButtonShape
import com.swordfish.touchinput.radial.ui.LemuroidButtonForeground
import com.swordfish.touchinput.radial.ui.LemuroidControlBackground
import com.swordfish.touchinput.radial.ui.LemuroidCrossForeground
import gg.padkit.PadKitScope
import gg.padkit.controls.ControlAnalog
import gg.padkit.controls.ControlButton
import gg.padkit.controls.ControlCross
import gg.padkit.ids.Id
import com.swordfish.touchinput.controller.R as TouchR

/**
 * Physical labels resolve to the existing PadKit/RetroPad keys.
 * Dreamcast native mapping: A/B/X/Y -> RetroPad B/A/Y/X;
 * Dreamcast L/R triggers -> RetroPad L2/R2.
 * Keep aligned with Dreamcast.kt and GripArcInputCatalog from PR #141.
 */
internal fun gripArcCombinedKeyCode(
    system: SystemID,
    key: String,
): Int? {
    // Do not allow Dreamcast's non-existent Select or duplicate L2/R2 buttons
    // to leak back into a combined controller through a future layout change.
    if (system == SystemID.DREAMCAST && key in setOf("Select", "L2", "R2")) return null
    return when (key) {
        "A" -> if (system == SystemID.DREAMCAST) KeyEvent.KEYCODE_BUTTON_B else KeyEvent.KEYCODE_BUTTON_A
        "B" -> if (system == SystemID.DREAMCAST) KeyEvent.KEYCODE_BUTTON_A else KeyEvent.KEYCODE_BUTTON_B
        "X" -> if (system == SystemID.DREAMCAST) KeyEvent.KEYCODE_BUTTON_Y else KeyEvent.KEYCODE_BUTTON_X
        "Y" -> if (system == SystemID.DREAMCAST) KeyEvent.KEYCODE_BUTTON_X else KeyEvent.KEYCODE_BUTTON_Y
        "L" -> if (system == SystemID.DREAMCAST) KeyEvent.KEYCODE_BUTTON_L2 else KeyEvent.KEYCODE_BUTTON_L1
        "R" -> if (system == SystemID.DREAMCAST) KeyEvent.KEYCODE_BUTTON_R2 else KeyEvent.KEYCODE_BUTTON_R1
        "L2" -> KeyEvent.KEYCODE_BUTTON_L2
        "R2" -> KeyEvent.KEYCODE_BUTTON_R2
        "Select" -> KeyEvent.KEYCODE_BUTTON_SELECT
        "Start" -> KeyEvent.KEYCODE_BUTTON_START
        "Menu" -> KeyEvent.KEYCODE_BUTTON_MODE
        else -> null
    }
}

/** Dedicated targets; same PadKit scope, motion sources and Android keys as Standard. */
@Composable
internal fun PadKitScope.GripArcCombinedControls(
    placement: GripArcCombinedPlacement,
    system: SystemID,
) {
    val sony = system in setOf(SystemID.PSX, SystemID.PLAYSTATION_2, SystemID.PSP)
    Box(Modifier.fillMaxSize()) {
        placement.controls.forEach { (key, rect) ->
            val position = Modifier.offset(rect.left.dp, rect.top.dp).size(rect.width.dp, rect.height.dp)
            when (key) {
                "D" ->
                    ControlCross(
                        modifier = position,
                        id = Id.DiscreteDirection(ComposeTouchLayouts.MOTION_SOURCE_DPAD),
                        allowDiagonals = true,
                        background = {},
                        foreground = { LemuroidCrossForeground(allowDiagonals = true, directionState = it) },
                    )
                "AnalogL", "AnalogR" ->
                    ControlAnalog(
                        modifier = position,
                        id =
                            Id.ContinuousDirection(
                                if (key == "AnalogL") {
                                    ComposeTouchLayouts.MOTION_SOURCE_LEFT_STICK
                                } else {
                                    ComposeTouchLayouts.MOTION_SOURCE_RIGHT_STICK
                                },
                            ),
                        analogPressId =
                            Id.Key(
                                if (key == "AnalogL") {
                                    KeyEvent.KEYCODE_BUTTON_THUMBL
                                } else {
                                    KeyEvent.KEYCODE_BUTTON_THUMBR
                                },
                            ),
                        background = { LemuroidControlBackground(Modifier.fillMaxSize(0.8f)) },
                        foreground = { LemuroidButtonForeground(pressed = it) },
                    )
                else -> {
                    val code = gripArcCombinedKeyCode(system, key) ?: return@forEach
                    val icon =
                        when {
                            key == "Select" -> TouchR.drawable.button_select
                            key == "Start" -> TouchR.drawable.button_start
                            key == "Menu" -> TouchR.drawable.button_menu
                            sony && key == "A" -> TouchR.drawable.psx_circle
                            sony && key == "B" -> TouchR.drawable.psx_cross
                            sony && key == "X" -> TouchR.drawable.psx_triangle
                            sony && key == "Y" -> TouchR.drawable.psx_square
                            else -> null
                        }
                    val shoulder = key in setOf("L", "R", "L2", "R2")
                    val utility = key in setOf("Select", "Start", "Menu")
                    val label = if (system == SystemID.GAMECUBE && key == "R2") "Z" else key
                    ControlButton(
                        modifier = position,
                        id = Id.Key(code),
                        background = {},
                        foreground = { pressed ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(42.dp, (if (shoulder || utility) 30 else 42).dp)) {
                                    LemuroidButtonForeground(
                                        pressed = pressed,
                                        label = label.takeIf { icon == null },
                                        icon = icon,
                                        shape =
                                            when {
                                                shoulder -> CartdeckButtonShape.SHOULDER
                                                utility -> CartdeckButtonShape.PILL
                                                else -> CartdeckButtonShape.FACE
                                            },
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
