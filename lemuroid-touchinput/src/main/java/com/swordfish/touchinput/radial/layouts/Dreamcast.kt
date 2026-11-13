package com.swordfish.touchinput.radial.layouts

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import com.swordfish.touchinput.radial.controls.LemuroidControlButton
import com.swordfish.touchinput.radial.controls.LemuroidControlCross
import com.swordfish.touchinput.radial.controls.LemuroidControlFaceButtons
import com.swordfish.touchinput.radial.layouts.shared.ComposeTouchLayouts
import com.swordfish.touchinput.radial.layouts.shared.SecondaryAnalogLeft
import com.swordfish.touchinput.radial.layouts.shared.SecondaryButtonMenu
import com.swordfish.touchinput.radial.layouts.shared.SecondaryButtonMenuPlaceholder
import com.swordfish.touchinput.radial.layouts.shared.SecondaryButtonStart
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import com.swordfish.touchinput.radial.ui.LemuroidButtonForeground
import gg.padkit.PadKitScope
import gg.padkit.ids.Id
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

internal object DreamcastPadGeometry {
    // Dreamcast-only tuning; do not alter other systems' PadKit dial behaviour.
    const val DPAD_FACTOR = 0.90f
    const val ANALOG_POSITION_DEGREES = -50f
    const val ANALOG_RADIAL_SCALE = 1.80f
    const val TRIGGER_POSITION_DEGREES = 90f
}

/**
 * Sega Dreamcast / Flycast touch pad; never reuse DualShock symbols.
 *
 * Flycast default mapping:
 * Dreamcast A = RetroPad B; B = RetroPad A;
 * X = RetroPad Y; Y = RetroPad X;
 * L/R triggers = RetroPad L2/R2.
 *
 * Visible Dreamcast compass: B east, A south, X west, Y north.
 * The Dreamcast has ONE left analog stick plus a D-pad (no right stick).
 * Continue to use the existing PadKit input pathway and CARTDECK materials.
 */
@Composable
fun PadKitScope.DreamcastLeft(
    modifier: Modifier = Modifier,
    settings: TouchControllerSettingsManager.Settings,
) {
    BaseLayoutLeft(
        settings = settings,
        modifier = modifier,
        primaryDialSizeFactor = DreamcastPadGeometry.DPAD_FACTOR,
        primaryDial = {
            LemuroidControlCross(
                id = Id.DiscreteDirection(ComposeTouchLayouts.MOTION_SOURCE_DPAD),
            )
        },
        secondaryDials = {
            LemuroidControlButton(
                modifier = Modifier.radialPosition(DreamcastPadGeometry.TRIGGER_POSITION_DEGREES),
                id = Id.Key(KeyEvent.KEYCODE_BUTTON_L2),
                label = "L",
            )
            SecondaryAnalogLeft(
                positionDegrees = DreamcastPadGeometry.ANALOG_POSITION_DEGREES,
                radialScale = DreamcastPadGeometry.ANALOG_RADIAL_SCALE,
            )
            SecondaryButtonMenuPlaceholder(settings)
        },
    )
}

@Composable
fun PadKitScope.DreamcastRight(
    modifier: Modifier = Modifier,
    settings: TouchControllerSettingsManager.Settings,
) {
    BaseLayoutRight(
        settings = settings,
        modifier = modifier,
        primaryDial = {
            LemuroidControlFaceButtons(
                ids =
                    persistentListOf(
                        Id.Key(KeyEvent.KEYCODE_BUTTON_A),
                        Id.Key(KeyEvent.KEYCODE_BUTTON_B),
                        Id.Key(KeyEvent.KEYCODE_BUTTON_Y),
                        Id.Key(KeyEvent.KEYCODE_BUTTON_X),
                    ),
                idsForegrounds =
                    persistentMapOf<Id.Key, @Composable (State<Boolean>) -> Unit>(
                        Id.Key(KeyEvent.KEYCODE_BUTTON_A) to {
                            LemuroidButtonForeground(pressed = it, label = "B")
                        },
                        Id.Key(KeyEvent.KEYCODE_BUTTON_B) to {
                            LemuroidButtonForeground(pressed = it, label = "A")
                        },
                        Id.Key(KeyEvent.KEYCODE_BUTTON_Y) to {
                            LemuroidButtonForeground(pressed = it, label = "X")
                        },
                        Id.Key(KeyEvent.KEYCODE_BUTTON_X) to {
                            LemuroidButtonForeground(pressed = it, label = "Y")
                        },
                    ),
            )
        },
        secondaryDials = {
            LemuroidControlButton(
                modifier = Modifier.radialPosition(DreamcastPadGeometry.TRIGGER_POSITION_DEGREES),
                id = Id.Key(KeyEvent.KEYCODE_BUTTON_R2),
                label = "R",
            )
            SecondaryButtonStart(position = 2)
            SecondaryButtonMenu(settings)
        },
    )
}
