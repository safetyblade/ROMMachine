package com.swordfish.touchinput.radial.controls

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.swordfish.touchinput.controller.R
import com.swordfish.touchinput.radial.LocalCartdeckControlBounds
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme
import com.swordfish.touchinput.radial.ui.CartdeckButtonShape
import com.swordfish.touchinput.radial.ui.LemuroidButtonForeground
import gg.padkit.PadKitScope
import gg.padkit.controls.ControlButton
import gg.padkit.ids.Id
import gg.padkit.layouts.radial.secondarydials.LayoutRadialSecondaryDialsScope

context(PadKitScope, LayoutRadialSecondaryDialsScope)
@Composable
fun LemuroidControlButton(
    modifier: Modifier = Modifier,
    id: Id.Key,
    label: String? = null,
    icon: Int? = null,
) {
    val theme = LocalLemuroidPadTheme.current
    val shape =
        when {
            label in setOf("L", "R", "L1", "R1", "L2", "R2", "White", "Black", "LT", "RT") ->
                CartdeckButtonShape.SHOULDER
            label == "Back" -> CartdeckButtonShape.PILL
            icon == R.drawable.button_start ||
                icon == R.drawable.button_select ||
                icon == R.drawable.button_menu -> CartdeckButtonShape.PILL
            else -> CartdeckButtonShape.FACE
        }
    val observe = LocalCartdeckControlBounds.current
    val seatId =
        when {
            label == "Back" -> "select"
            label == "White" -> "shoulder-L1"
            label == "Black" -> "shoulder-R1"
            label == "LT" -> "shoulder-L2"
            label == "RT" -> "shoulder-R2"
            icon == R.drawable.button_start -> "start"
            icon == R.drawable.button_select -> "select"
            icon == R.drawable.button_menu -> "menu"
            icon == R.drawable.button_mic -> "ds-mic"
            icon == R.drawable.button_close_screen -> "ds-layout"
            label in setOf("L", "R", "L1", "R1", "L2", "R2") -> "shoulder-$label"
            // Genesis six-button C/Z and N64 Z have independent active
            // input targets, so they need independent hardware wells too.
            label in setOf("C", "Z") -> "aux-$label"
            else -> null
        }
    ControlButton(
        modifier =
            modifier.padding(theme.padding).then(
                if (observe != null && seatId != null) {
                    Modifier.onGloballyPositioned { observe(seatId, it.boundsInRoot()) }
                } else {
                    Modifier
                },
            ),
        id = id,
        foreground = {
            LemuroidButtonForeground(
                pressed = it,
                icon = icon,
                label = label,
                shape = shape,
            )
        },
        // The former circular disc was decorative; PadKit retains the hit zone.
        background = {},
    )
}
