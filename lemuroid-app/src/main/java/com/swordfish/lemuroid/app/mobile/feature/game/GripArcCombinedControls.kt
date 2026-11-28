package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme
import com.swordfish.touchinput.radial.settings.NativeControlIcon
import com.swordfish.touchinput.radial.settings.NativeControlKind
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
import com.swordfish.touchinput.controller.R as TouchR

/** Dedicated targets; same PadKit scope, motion sources and Android keys as Standard. */
@Composable
internal fun PadKitScope.GripArcCombinedControls(
    placement: GripArcCombinedPlacement,
    standalone: Boolean = false,
) {
    require(placement.complete) { "Incomplete native controller placement" }
    Box(Modifier.fillMaxSize()) {
        placement.native.controls.forEach { spec ->
            val key = spec.key
            val rect = placement.controls.getValue(key)
            val visual = placement.visuals.getValue(key)
            val position = Modifier.offset(rect.left.dp, rect.top.dp).size(rect.width.dp, rect.height.dp)
            val diagonals = !standalone || key != "CDirection"
            when (spec.kind) {
                NativeControlKind.DPAD ->
                    ControlCross(
                        modifier = position,
                        id = Id.DiscreteDirection(spec.motionSource!!),
                        allowDiagonals = diagonals,
                        background = {},
                        foreground = { state ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(visual.w.dp, visual.h.dp)) {
                                    when {
                                        standalone && placement.native.controller == TouchControllerID.NGP ->
                                            CartdeckDigitalClickStick(
                                                state.value,
                                                LocalLemuroidPadTheme.current.palette.accent,
                                            )
                                        standalone && placement.native.controller in
                                            setOf(
                                                TouchControllerID.ARCADE_4, TouchControllerID.ARCADE_6,
                                            )
                                        -> CartdeckDigitalBallTop(state.value)
                                        else ->
                                            LemuroidCrossForeground(
                                                allowDiagonals = diagonals,
                                                directionState = state,
                                            )
                                    }
                                    if (standalone && key == "CDirection") {
                                        val color = LocalLemuroidPadTheme.current.palette.accent
                                        Text(
                                            "C▲",
                                            Modifier.align(Alignment.TopCenter),
                                            color = color,
                                            fontSize = 10.sp,
                                        )
                                        Text(
                                            "C◀",
                                            Modifier.align(Alignment.CenterStart),
                                            color = color,
                                            fontSize = 10.sp,
                                        )
                                        Text(
                                            "C▶",
                                            Modifier.align(Alignment.CenterEnd),
                                            color = color,
                                            fontSize = 10.sp,
                                        )
                                        Text(
                                            "C▼",
                                            Modifier.align(Alignment.BottomCenter),
                                            color = color,
                                            fontSize = 10.sp,
                                        )
                                    } else if (standalone && spec.label.endsWith(" cursor")) {
                                        Text(
                                            spec.label.substringBefore(' '),
                                            modifier = Modifier.align(Alignment.Center),
                                            fontSize = 12.sp,
                                            color = LocalLemuroidPadTheme.current.palette.onButton,
                                        )
                                    }
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
                                if (standalone) {
                                    LemuroidControlBackground(Modifier.size(visual.w.dp, visual.h.dp))
                                } else {
                                    LemuroidControlBackground(Modifier.fillMaxSize(0.8f))
                                }
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
                        foreground = { pressed ->
                            if (standalone) {
                                val diameter = (visual.w * 0.52f).dp
                                Box(Modifier.requiredSize(diameter), contentAlignment = Alignment.Center) {
                                    LemuroidButtonForeground(pressed = pressed)
                                }
                            } else {
                                LemuroidButtonForeground(pressed = pressed)
                            }
                        },
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
                            null ->
                                if (standalone) {
                                    when (key) {
                                        "Keyboard" -> TouchR.drawable.button_keyboard
                                        "Mic" -> TouchR.drawable.button_mic
                                        "Lid" -> TouchR.drawable.button_close_screen
                                        else -> null
                                    }
                                } else {
                                    null
                                }
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

@Composable
private fun CartdeckDigitalBallTop(direction: Offset) {
    val palette = LocalLemuroidPadTheme.current.palette
    Canvas(Modifier.fillMaxSize()) {
        val radius = size.minDimension / 2f
        val shaft = center + direction * radius * 0.23f
        val light = shaft - Offset(radius * 0.17f, radius * 0.2f)
        drawCircle(palette.bezel, radius)
        drawCircle(palette.onButton.copy(alpha = 0.32f), radius * 0.90f, style = Stroke(1.5.dp.toPx()))
        drawCircle(palette.chassis, radius * 0.69f)
        drawCircle(
            Brush.radialGradient(
                listOf(palette.onButton.copy(alpha = 0.68f), palette.button, Color.Black),
                center = light,
                radius = radius * 0.93f,
            ),
            radius * 0.56f,
            shaft,
        )
        if (direction != Offset.Zero) {
            drawCircle(palette.accent, radius * 0.06f, center + direction * radius * 0.83f)
        }
    }
}
