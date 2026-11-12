package com.swordfish.lemuroid.app.mobile.feature.game

import android.view.KeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swordfish.touchinput.controller.R as TouchR
import com.swordfish.touchinput.radial.LocalCartdeckControlBounds
import com.swordfish.touchinput.radial.layouts.shared.ComposeTouchLayouts
import com.swordfish.touchinput.radial.ui.CartdeckButtonShape
import com.swordfish.touchinput.radial.ui.LemuroidButtonForeground
import com.swordfish.touchinput.radial.ui.LemuroidCrossForeground
import gg.padkit.PadKitScope
import gg.padkit.controls.ControlButton
import gg.padkit.controls.ControlCross
import gg.padkit.ids.Id

/**
 * Same PadKit scope and same input IDs used by Standard. Only the geometry and
 * visual seats differ. The decorative layers do not register pointers.
 */
@Composable
fun PadKitScope.PivotArcControlGroup(
    placement: PivotArcPlacement,
    heldKeys: Set<Int>,
    accent: Color,
    onToggleHold: (Int) -> Unit,
) {
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
            if (key == "HoldA" || key == "HoldB") {
                val code = if (key == "HoldA") KeyEvent.KEYCODE_BUTTON_A else KeyEvent.KEYCODE_BUTTON_B
                PivotArcHoldSwitch(
                    modifier = position,
                    label = if (key == "HoldA") "A" else "B",
                    diameter = visual.w,
                    active = code in heldKeys,
                    accent = accent,
                    onToggle = { onToggleHold(code) },
                )
            } else if (key == "D") {
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
                                if ((key == "A" || key == "B") && inputKey in heldKeys) {
                                    Canvas(Modifier.fillMaxSize()) {
                                        val r = size.minDimension / 2f - 2.dp.toPx()
                                        drawCircle(
                                            color = accent.copy(alpha = 0.32f),
                                            radius = r,
                                            style = Stroke(width = 5.dp.toPx()),
                                        )
                                        drawCircle(
                                            color = accent,
                                            radius = r,
                                            style = Stroke(width = 1.8.dp.toPx()),
                                        )
                                    }
                                }
                            }
                        }
                    },
                    background = {},
                )
            }
        }
    }
}

/**
 * A tactile 28dp recessed toggle inside an independent 48dp hit target.
 * The ring is steady: no motion is required to understand the held state.
 */
@Composable
private fun PivotArcHoldSwitch(
    modifier: Modifier,
    label: String,
    diameter: Float,
    active: Boolean,
    accent: Color,
    onToggle: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .toggleable(
                    value = active,
                    role = Role.Switch,
                    onValueChange = { onToggle() },
                )
                .semantics {
                    contentDescription = "Hold $label"
                    stateDescription = if (active) "Held" else "Released"
                },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(diameter.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                drawCircle(color = Color.Black.copy(alpha = 0.48f), radius = radius)
                drawCircle(
                    color = if (active) accent.copy(alpha = 0.27f) else Color.Gray.copy(alpha = 0.24f),
                    radius = radius - 1.dp.toPx(),
                )
                drawCircle(
                    color = if (active) accent else Color.Gray.copy(alpha = 0.64f),
                    radius = radius - 2.dp.toPx(),
                    style = Stroke(width = if (active) 2.dp.toPx() else 1.dp.toPx()),
                )
                drawCircle(
                    color = Color.White.copy(alpha = if (active) 0.42f else 0.12f),
                    radius = radius - 5.dp.toPx(),
                    style = Stroke(width = 0.8.dp.toPx()),
                )
            }
            Text(
                text = "H",
                color = if (active) accent else Color.LightGray,
                fontSize = 10.sp,
            )
        }
    }
}
