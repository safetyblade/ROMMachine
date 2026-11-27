package com.swordfish.touchinput.radial.layouts

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.swordfish.touchinput.radial.controls.LemuroidControlButton
import com.swordfish.touchinput.radial.controls.LemuroidControlCross
import com.swordfish.touchinput.radial.controls.LemuroidControlFaceButtons
import com.swordfish.touchinput.radial.layouts.shared.ComposeTouchLayouts
import com.swordfish.touchinput.radial.layouts.shared.SecondaryButtonCoin
import com.swordfish.touchinput.radial.layouts.shared.SecondaryButtonMenu
import com.swordfish.touchinput.radial.layouts.shared.SecondaryButtonMenuPlaceholder
import com.swordfish.touchinput.radial.layouts.shared.SecondaryButtonStart
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import com.swordfish.touchinput.radial.ui.LemuroidCentralButton
import com.swordfish.touchinput.radial.utils.buildCentral6ButtonsAnchors
import gg.padkit.PadKitScope
import gg.padkit.anchors.Anchor
import gg.padkit.ids.Id
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentMapOf

@Composable
fun PadKitScope.Arcade6Left(
    modifier: Modifier = Modifier,
    settings: TouchControllerSettingsManager.Settings,
) {
    BaseLayoutLeft(
        settings = settings,
        modifier = modifier,
        primaryDial = {
            LemuroidControlCross(id = Id.DiscreteDirection(ComposeTouchLayouts.MOTION_SOURCE_DPAD_AND_LEFT_STICK))
        },
        secondaryDials = {
            SecondaryButtonCoin()
            SecondaryButtonStart(position = 1)
            SecondaryButtonMenuPlaceholder(settings)
        },
    )
}

@Composable
fun PadKitScope.Arcade6Right(
    modifier: Modifier = Modifier,
    settings: TouchControllerSettingsManager.Settings,
    labels: Map<Int, String> = emptyMap(),
    includeStart: Boolean = false,
) {
    val centralAnchors = rememberCentralAnchorsForSixButtons(settings.rotation)

    BaseLayoutRight(
        settings = settings,
        modifier = modifier,
        primaryDial = {
            LemuroidControlFaceButtons(
                primaryAnchors = centralAnchors,
                background = { },
                applyPadding = false,
                trackPointers = false,
                idsForegrounds =
                    persistentMapOf<Id.Key, @Composable (State<Boolean>) -> Unit>(
                        Id.Key(KeyEvent.KEYCODE_BUTTON_X) to {
                            LemuroidCentralButton(it, labels[KeyEvent.KEYCODE_BUTTON_X])
                        },
                        Id.Key(KeyEvent.KEYCODE_BUTTON_A) to {
                            LemuroidCentralButton(it, labels[KeyEvent.KEYCODE_BUTTON_A])
                        },
                        Id.Key(KeyEvent.KEYCODE_BUTTON_B) to {
                            LemuroidCentralButton(it, labels[KeyEvent.KEYCODE_BUTTON_B])
                        },
                        Id.Key(KeyEvent.KEYCODE_BUTTON_Y) to {
                            LemuroidCentralButton(it, labels[KeyEvent.KEYCODE_BUTTON_Y])
                        },
                    ),
            )
        },
        secondaryDials = {
            LemuroidControlButton(
                modifier = Modifier.radialPosition(90f),
                id = Id.Key(KeyEvent.KEYCODE_BUTTON_L1),
                label = labels[KeyEvent.KEYCODE_BUTTON_L1],
            )
            LemuroidControlButton(
                modifier = Modifier.radialPosition(60f),
                id = Id.Key(KeyEvent.KEYCODE_BUTTON_R1),
                label = labels[KeyEvent.KEYCODE_BUTTON_R1],
            )
            if (includeStart) SecondaryButtonStart(position = 2)
            SecondaryButtonMenu(settings)
        },
    )
}

@Composable
private fun rememberCentralAnchorsForSixButtons(rotation: Float): PersistentList<Anchor<Id.Key>> {
    return remember(rotation) {
        buildCentral6ButtonsAnchors(
            rotation,
            KeyEvent.KEYCODE_BUTTON_X,
            KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_B,
        )
    }
}
