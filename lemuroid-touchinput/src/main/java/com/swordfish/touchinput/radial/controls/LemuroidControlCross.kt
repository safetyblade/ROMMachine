package com.swordfish.touchinput.radial.controls

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.swordfish.touchinput.radial.LocalCartdeckControlBounds
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme
import com.swordfish.touchinput.radial.ui.LemuroidCrossForeground
import gg.padkit.PadKitScope
import gg.padkit.controls.ControlCross
import gg.padkit.ids.Id

context(PadKitScope)
@Composable
fun LemuroidControlCross(
    modifier: Modifier = Modifier,
    id: Id.DiscreteDirection,
    allowDiagonals: Boolean = true,
    hardwareSeatId: String = "dpad",
    background: @Composable () -> Unit = {},
    foreground: @Composable (State<Offset>) -> Unit = {
        LemuroidCrossForeground(
            allowDiagonals = allowDiagonals,
            directionState = it,
        )
    },
) {
    val theme = LocalLemuroidPadTheme.current
    val observe = LocalCartdeckControlBounds.current
    ControlCross(
        modifier =
            modifier.padding(theme.padding).then(
                if (observe != null) {
                    Modifier.onGloballyPositioned { observe(hardwareSeatId, it.boundsInRoot()) }
                } else {
                    Modifier
                },
            ),
        id = id,
        allowDiagonals = allowDiagonals,
        background = background,
        foreground = foreground,
    )
}
