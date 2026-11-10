package com.swordfish.lemuroid.app.mobile.feature.game

import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.swordfish.touchinput.radial.LocalCartdeckControlBounds
import com.swordfish.touchinput.radial.layouts.shared.ComposeTouchLayouts
import com.swordfish.touchinput.radial.ui.CartdeckButtonShape
import com.swordfish.touchinput.radial.ui.LemuroidButtonForeground
import com.swordfish.touchinput.radial.ui.LemuroidCrossForeground
import gg.padkit.PadKitScope
import gg.padkit.controls.ControlButton
import gg.padkit.controls.ControlCross
import gg.padkit.ids.Id
import com.swordfish.touchinput.controller.R as TouchR

/**
 * Same PadKit scope and same input IDs used by Standard. Only the geometry and
 * visual seats differ. The decorative layers do not register pointers.
 */
@Composable
fun PadKitScope.PivotArcControlGroup(placement: PivotArcPlacement) {
    val observe = LocalCartdeckControlBounds.current
    Box(Modifier.fillMaxSize()) {
        placement.controls.forEach { (key, bounds) ->
            val visual = placement.visuals.getValue(key)
            val position =
                Modifier
                    .offset(x = bounds.left.dp, y = bounds.top.dp)
                    .size(width = bounds.width.dp, height = bounds.height.dp)
                    .then(
                        if (observe != null) {
                            Modifier.onGloballyPositioned {
                                observe(
                                    when (key) {
                                        "D" -> "dpad"
                                        "L", "R" -> "shoulder-$key"
                                        else -> key.lowercase()
                                    },
                                    it.boundsInRoot(),
                                )
                            }
                        } else {
                            Modifier
                        },
                    )
            if (key == "D") {
                ControlCross(
                    modifier = position,
                    id = Id.DiscreteDirection(ComposeTouchLayouts.MOTION_SOURCE_DPAD),
                    allowDiagonals = true,
                    background = {},
                    foreground = { direction ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Box(Modifier.size(visual.w.dp, visual.h.dp)) {
                                LemuroidCrossForeground(allowDiagonals = true, directionState = direction)
                            }
                        }
                    },
                )
            } else {
                val inputKey =
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
                        else -> null
                    } ?: return@forEach
                val icon =
                    when (key) {
                        "Select" -> TouchR.drawable.button_select
                        "Start" -> TouchR.drawable.button_start
                        "Menu" -> TouchR.drawable.button_menu
                        else -> null
                    }
                val shape =
                    when (key) {
                        "L", "R", "L2", "R2" -> CartdeckButtonShape.SHOULDER
                        "Select", "Start", "Menu" -> CartdeckButtonShape.PILL
                        else -> CartdeckButtonShape.FACE
                    }
                ControlButton(
                    modifier = position,
                    id = Id.Key(inputKey),
                    foreground = { pressed ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Box(Modifier.size(visual.w.dp, visual.h.dp)) {
                                LemuroidButtonForeground(
                                    pressed = pressed,
                                    label = key.takeIf { icon == null },
                                    icon = icon,
                                    shape = shape,
                                )
                            }
                        }
                    },
                    background = {},
                )
            }
        }
    }
}
