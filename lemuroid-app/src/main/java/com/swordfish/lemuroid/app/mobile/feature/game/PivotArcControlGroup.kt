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
import com.swordfish.lemuroid.app.shared.game.viewmodel.WonderSwanPivotInput
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.touchinput.radial.LocalCartdeckControlBounds
import com.swordfish.touchinput.radial.layouts.shared.ComposeTouchLayouts
import com.swordfish.touchinput.radial.settings.NativeControlInventory
import com.swordfish.touchinput.radial.settings.TouchControllerID
import com.swordfish.touchinput.radial.ui.CartdeckButtonShape
import com.swordfish.touchinput.radial.ui.LemuroidButtonForeground
import com.swordfish.touchinput.radial.ui.LemuroidControlBackground
import com.swordfish.touchinput.radial.ui.LemuroidCrossForeground
import gg.padkit.PadKitScope
import gg.padkit.controls.ControlAnalog
import gg.padkit.controls.ControlButton
import gg.padkit.controls.ControlCross
import gg.padkit.ids.Id
import androidx.compose.runtime.key as compositionKey
import com.swordfish.touchinput.controller.R as TouchR

/**
 * Same PadKit scope and same input IDs used by Standard. Only the geometry and
 * visual seats differ. The decorative layers do not register pointers.
 */
@Composable
fun PadKitScope.PivotArcControlGroup(
    placement: PivotArcPlacement,
    system: SystemID,
    heldKeys: Set<Int>,
    accent: Color,
    onToggleHold: (Int) -> Unit,
    combined: Boolean = false,
    touchController: TouchControllerID? = null,
    soloProfile: PivotArcSoloProfile? = null,
) {
    val observe = LocalCartdeckControlBounds.current
    val wonderSwan = WonderSwanPivotInput.forController(touchController)
    Box(Modifier.fillMaxSize()) {
        placement.controls.forEach { (key, bounds) ->
            val seat = soloProfile?.seat(key)
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
                                        "Analog" -> "analog-left"
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
            if (seat?.holdOf != null || key == "HoldA" || key == "HoldB") {
                val face = seat?.holdOf ?: if (key == "HoldA") "A" else "B"
                val code =
                    soloProfile?.seat(face)?.keyCode ?: if (face == "A") {
                        wonderSwan?.a ?: KeyEvent.KEYCODE_BUTTON_A
                    } else {
                        wonderSwan?.b ?: KeyEvent.KEYCODE_BUTTON_B
                    }
                PivotArcHoldSwitch(
                    modifier = position,
                    label = soloProfile?.seat(face)?.label ?: PivotArcLayoutProfile.nativeLabel(system, face),
                    showLabel = combined,
                    diameter = visual.w,
                    active = code in heldKeys,
                    accent = accent,
                    onToggle = { onToggleHold(code) },
                )
            } else if (key == "Analog" || seat?.analog == true) {
                ControlAnalog(
                    modifier = position.semantics { contentDescription = "Analog stick" },
                    id = Id.ContinuousDirection(seat?.direction ?: ComposeTouchLayouts.MOTION_SOURCE_LEFT_STICK),
                    analogPressId = if (seat == null) Id.Key(KeyEvent.KEYCODE_BUTTON_THUMBL) else null,
                    background = { LemuroidControlBackground(Modifier.fillMaxSize(0.8f)) },
                    foreground = { LemuroidButtonForeground(pressed = it) },
                )
            } else if (seat?.direction != null || key == "D" || (key == "Cursor" && wonderSwan != null)) {
                val secondary = key == "Cursor"
                val label = if (secondary) wonderSwan?.secondaryLabel else wonderSwan?.primaryLabel
                val directionSource =
                    seat?.direction ?: if (secondary) {
                        checkNotNull(wonderSwan).source
                    } else {
                        ComposeTouchLayouts.MOTION_SOURCE_DPAD
                    }
                ControlCross(
                    modifier = if (label == null) position else position.semantics { contentDescription = label },
                    id = Id.DiscreteDirection(directionSource),
                    allowDiagonals = seat?.diagonals ?: true,
                    background = {},
                    foreground = { direction ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Box(Modifier.size(visual.w.dp, visual.h.dp)) {
                                LemuroidCrossForeground(allowDiagonals = seat?.diagonals ?: true, directionState = direction)
                                if (label != null) {
                                    Text(
                                        label.take(1),
                                        Modifier.align(Alignment.Center),
                                        color = accent,
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                        }
                    },
                )
            } else {
                val inputKey =
                    seat?.keyCode ?: when (key) {
                        "A" -> wonderSwan?.a ?: KeyEvent.KEYCODE_BUTTON_A
                        "B" -> wonderSwan?.b ?: KeyEvent.KEYCODE_BUTTON_B
                        "X" -> KeyEvent.KEYCODE_BUTTON_X
                        "Y" -> KeyEvent.KEYCODE_BUTTON_Y
                        "L" -> KeyEvent.KEYCODE_BUTTON_L1
                        "R" -> KeyEvent.KEYCODE_BUTTON_R1
                        "L2" -> KeyEvent.KEYCODE_BUTTON_L2
                        "R2" -> KeyEvent.KEYCODE_BUTTON_R2
                        "Select" -> KeyEvent.KEYCODE_BUTTON_SELECT
                        "Start" -> KeyEvent.KEYCODE_BUTTON_START
                        "Menu" -> KeyEvent.KEYCODE_BUTTON_MODE
                        "Mic", "Lid" -> touchController?.let { pivotArcDsAuxiliaryKey(it, key) }
                        else -> null
                    } ?: return@forEach
                val label = seat?.label ?: PivotArcLayoutProfile.nativeLabel(system, key)
                val icon =
                    when (label) {
                        "Circle" -> TouchR.drawable.psx_circle
                        "Cross" -> TouchR.drawable.psx_cross
                        "Triangle" -> TouchR.drawable.psx_triangle
                        "Square" -> TouchR.drawable.psx_square
                        else -> null
                    } ?: when (key) {
                        "Select" -> TouchR.drawable.button_select.takeIf { label == key }
                        "Keyboard" -> TouchR.drawable.button_keyboard
                        "Start" -> TouchR.drawable.button_start.takeIf { label == key }
                        "Menu" -> TouchR.drawable.button_menu
                        "Mic" -> TouchR.drawable.button_mic
                        "Lid" -> TouchR.drawable.button_close_screen
                        else -> null
                    }
                val shape =
                    when (key) {
                        "L", "R", "L2", "R2" -> CartdeckButtonShape.SHOULDER
                        "Select", "Start", "Menu", "Keyboard", "Home", "Mic", "Lid" -> CartdeckButtonShape.PILL
                        else -> CartdeckButtonShape.FACE
                    }
                compositionKey(key, inputKey) {
                    ControlButton(
                        modifier = position.semantics { contentDescription = label },
                        id = Id.Key(inputKey),
                        foreground = { pressed ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(visual.w.dp, visual.h.dp)) {
                                    LemuroidButtonForeground(
                                        pressed = pressed,
                                        label = label.takeIf { icon == null },
                                        icon = icon,
                                        shape = shape,
                                    )
                                    if (inputKey in heldKeys && (seat != null || key == "A" || key == "B")) {
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
    showLabel: Boolean = false,
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
                text = if (showLabel) "Hold $label" else "H",
                color = if (active) accent else Color.LightGray,
                fontSize = if (showLabel) 8.sp else 10.sp,
            )
        }
    }
}

internal fun pivotArcDsAuxiliaryKey(
    controller: TouchControllerID,
    control: String,
): Int? = NativeControlInventory.forHoldTray(controller)?.controls?.singleOrNull { it.key == control }?.keyCode
