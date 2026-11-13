package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme
import com.swordfish.touchinput.radial.settings.NativeControlIcon
import com.swordfish.touchinput.radial.settings.NativeControlKind
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
internal fun PadKitScope.GripArcCombinedControls(placement: GripArcCombinedPlacement) {
    require(placement.complete) { "Incomplete native controller placement" }
    Box(Modifier.fillMaxSize()) {
        placement.native.controls.forEach { spec ->
            val key = spec.key
            val rect = placement.controls.getValue(key)
            val visual = placement.visuals.getValue(key)
            val position = Modifier.offset(rect.left.dp, rect.top.dp).size(rect.width.dp, rect.height.dp)
            when (spec.kind) {
                NativeControlKind.DPAD ->
                    ControlCross(
                        modifier = position,
                        id = Id.DiscreteDirection(spec.motionSource!!),
                        allowDiagonals = true,
                        background = {},
                        foreground = { state ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(visual.w.dp, visual.h.dp)) {
                                    LemuroidCrossForeground(allowDiagonals = true, directionState = state)
                                }
                            }
                        },
                    )
                NativeControlKind.ANALOG ->
                    ControlAnalog(
                        modifier = position,
                        id = Id.ContinuousDirection(spec.motionSource!!),
                        analogPressId = spec.pressCode?.let { Id.Key(it) },
                        background = {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                LemuroidControlBackground(Modifier.fillMaxSize(0.8f))
                                if (spec.label == "C-Stick") {
                                    Text(
                                        text = "C-Stick",
                                        modifier = Modifier.align(Alignment.BottomCenter),
                                        fontSize = 8.sp,
                                        color = LocalLemuroidPadTheme.current.palette.onButton,
                                    )
                                }
                            }
                        },
                        foreground = { LemuroidButtonForeground(pressed = it) },
                    )
                else -> {
                    val code = spec.keyCode!!
                    val icon =
                        when (spec.icon) {
                            NativeControlIcon.SELECT -> TouchR.drawable.button_select
                            NativeControlIcon.START -> TouchR.drawable.button_start
                            NativeControlIcon.MENU -> TouchR.drawable.button_menu
                            NativeControlIcon.CIRCLE -> TouchR.drawable.psx_circle
                            NativeControlIcon.CROSS -> TouchR.drawable.psx_cross
                            NativeControlIcon.TRIANGLE -> TouchR.drawable.psx_triangle
                            NativeControlIcon.SQUARE -> TouchR.drawable.psx_square
                            null -> null
                        }
                    val shoulder = spec.kind == NativeControlKind.SHOULDER && !spec.roundButton
                    val utility = spec.kind == NativeControlKind.UTILITY
                    ControlButton(
                        modifier = position,
                        id = Id.Key(code),
                        background = {},
                        foreground = { pressed ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(visual.w.dp, visual.h.dp)) {
                                    LemuroidButtonForeground(
                                        pressed = pressed,
                                        label = spec.label.takeIf { icon == null },
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
