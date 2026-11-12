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
                    val code =
                        when (key) {
                            "A" -> KeyEvent.KEYCODE_BUTTON_A
                            "B" -> KeyEvent.KEYCODE_BUTTON_B
                            "X" -> KeyEvent.KEYCODE_BUTTON_X
                            "Y" -> KeyEvent.KEYCODE_BUTTON_Y
                            "L" -> KeyEvent.KEYCODE_BUTTON_L1
                            "R" -> KeyEvent.KEYCODE_BUTTON_R1
                            "L2" -> KeyEvent.KEYCODE_BUTTON_L2
                            "R2" -> KeyEvent.KEYCODE_BUTTON_R2
                            "Select" -> KeyEvent.KEYCODE_BUTTON_SELECT
                            "Start" -> KeyEvent.KEYCODE_BUTTON_START
                            "Menu" -> KeyEvent.KEYCODE_BUTTON_MODE
                            else -> return@forEach
                        }
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
