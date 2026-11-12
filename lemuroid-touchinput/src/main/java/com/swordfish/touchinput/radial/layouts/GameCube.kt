package com.swordfish.touchinput.radial.layouts

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import com.swordfish.touchinput.radial.controls.LemuroidControlAnalog
import com.swordfish.touchinput.radial.controls.LemuroidControlButton
import com.swordfish.touchinput.radial.controls.LemuroidControlCross
import com.swordfish.touchinput.radial.controls.LemuroidControlFaceButtons
import com.swordfish.touchinput.radial.layouts.shared.ComposeTouchLayouts
import com.swordfish.touchinput.radial.layouts.shared.SecondaryAnalogRight
import com.swordfish.touchinput.radial.layouts.shared.SecondaryButtonL
import com.swordfish.touchinput.radial.layouts.shared.SecondaryButtonMenu
import com.swordfish.touchinput.radial.layouts.shared.SecondaryButtonMenuPlaceholder
import com.swordfish.touchinput.radial.layouts.shared.SecondaryButtonR
import com.swordfish.touchinput.radial.layouts.shared.SecondaryButtonStart
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import com.swordfish.touchinput.radial.ui.LemuroidButtonForeground
import gg.padkit.PadKitScope
import gg.padkit.ids.Id
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

/**
 * Dolphin GameCube RetroPad mapping, not PlayStation's DualShock.
 *
 * Visual face compass: Y north, B west, A south, X east.
 * L/R = RetroPad L1/R1; Z = R2. Main analog = left stick,
 * C-Stick = right stick. Stick-clicks expose analog half-triggers.
 *
 * The native Dolphin engine/port ID and other systems remain unchanged.
 */
@Composable
fun PadKitScope.GameCubeLeft(
    modifier: Modifier = Modifier,
    settings: TouchControllerSettingsManager.Settings,
) {
    BaseLayoutLeft(
        settings = settings,
        modifier = modifier,
        primaryDial = {
            LemuroidControlAnalog(
                id = Id.ContinuousDirection(ComposeTouchLayouts.MOTION_SOURCE_LEFT_STICK),
                analogPressId = Id.Key(KeyEvent.KEYCODE_BUTTON_THUMBL),
                hardwareSeatId = "analog-left",
            )
        },
        secondaryDials = {
            SecondaryButtonL()
            LemuroidControlCross(
                modifier = Modifier.radialPosition(-80f).radialScale(1.7f),
                id = Id.DiscreteDirection(ComposeTouchLayouts.MOTION_SOURCE_DPAD),
            )
            SecondaryButtonMenuPlaceholder(settings)
        },
    )
}

@Composable
fun PadKitScope.GameCubeRight(
    modifier: Modifier = Modifier,
    settings: TouchControllerSettingsManager.Settings,
) {
    BaseLayoutRight(
        settings = settings,
        modifier = modifier,
        primaryDial = {
            LemuroidControlFaceButtons(
                // PadKit compass order: east, south, west, north.
                // Original GameCube: X east, A south, B west, Y north.
                ids =
                    persistentListOf(
                        Id.Key(KeyEvent.KEYCODE_BUTTON_X),
                        Id.Key(KeyEvent.KEYCODE_BUTTON_A),
                        Id.Key(KeyEvent.KEYCODE_BUTTON_B),
                        Id.Key(KeyEvent.KEYCODE_BUTTON_Y),
                    ),
                idsForegrounds =
                    persistentMapOf<Id.Key, @Composable (State<Boolean>) -> Unit>(
                        Id.Key(KeyEvent.KEYCODE_BUTTON_A) to {
                            LemuroidButtonForeground(pressed = it, label = "A")
                        },
                        Id.Key(KeyEvent.KEYCODE_BUTTON_B) to {
                            LemuroidButtonForeground(pressed = it, label = "B")
                        },
                        Id.Key(KeyEvent.KEYCODE_BUTTON_X) to {
                            LemuroidButtonForeground(pressed = it, label = "X")
                        },
                        Id.Key(KeyEvent.KEYCODE_BUTTON_Y) to {
                            LemuroidButtonForeground(pressed = it, label = "Y")
                        },
                    ),
            )
        },
        secondaryDials = {
            SecondaryButtonR()
            LemuroidControlButton(
                modifier = Modifier.radialPosition(60f),
                id = Id.Key(KeyEvent.KEYCODE_BUTTON_R2),
                label = "Z",
            )
            SecondaryButtonStart(position = 2)
            // Dolphin maps this right-hand analog input to GameCube C-Stick.
            SecondaryAnalogRight()
            SecondaryButtonMenu(settings)
        },
    )
}
