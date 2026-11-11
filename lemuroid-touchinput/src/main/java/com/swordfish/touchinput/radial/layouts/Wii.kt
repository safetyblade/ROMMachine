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
import com.swordfish.touchinput.radial.layouts.shared.SecondaryAnalogRight
import com.swordfish.touchinput.radial.layouts.shared.SecondaryButtonMenu
import com.swordfish.touchinput.radial.layouts.shared.SecondaryButtonMenuPlaceholder
import com.swordfish.touchinput.radial.layouts.shared.SecondaryButtonSelect
import com.swordfish.touchinput.radial.layouts.shared.SecondaryButtonStart
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import com.swordfish.touchinput.radial.ui.LemuroidButtonForeground
import gg.padkit.PadKitScope
import gg.padkit.ids.Id
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

/** Labels match the pinned Dolphin input descriptors; PadKit keeps simultaneous input. */
enum class WiiLayoutProfile(val a: String, val b: String, val x: String, val y: String) {
    CLASSIC("A", "B", "X", "Y"),
    SIDEWAYS("2", "1", "A", "B"),
    NUNCHUK("A", "B", "C", "Z"),
}

@Composable
fun PadKitScope.WiiLeft(
    modifier: Modifier = Modifier,
    settings: TouchControllerSettingsManager.Settings,
    profile: WiiLayoutProfile,
) {
    BaseLayoutLeft(
        settings = settings,
        modifier = modifier,
        primaryDial = { LemuroidControlCross(id = Id.DiscreteDirection(ComposeTouchLayouts.MOTION_SOURCE_DPAD)) },
        secondaryDials = {
            LemuroidControlButton(
                Modifier.radialPosition(90f),
                Id.Key(
                    if (profile == WiiLayoutProfile.SIDEWAYS) {
                        KeyEvent.KEYCODE_BUTTON_SELECT
                    } else {
                        KeyEvent.KEYCODE_BUTTON_L1
                    },
                ),
                label = if (profile == WiiLayoutProfile.CLASSIC) "ZL" else "-",
            )
            LemuroidControlButton(
                Modifier.radialPosition(120f),
                Id.Key(
                    if (profile == WiiLayoutProfile.SIDEWAYS) {
                        KeyEvent.KEYCODE_BUTTON_THUMBR
                    } else {
                        KeyEvent.KEYCODE_BUTTON_L2
                    },
                ),
                label =
                    when (profile) {
                        WiiLayoutProfile.CLASSIC -> "L"
                        WiiLayoutProfile.NUNCHUK -> "Shake N"
                        WiiLayoutProfile.SIDEWAYS -> "Home"
                    },
            )
            SecondaryButtonSelect(position = 2)
            SecondaryButtonMenuPlaceholder(settings)
            SecondaryAnalogLeft()
        },
    )
}

@Composable
fun PadKitScope.WiiRight(
    modifier: Modifier = Modifier,
    settings: TouchControllerSettingsManager.Settings,
    profile: WiiLayoutProfile,
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
                            LemuroidButtonForeground(
                                pressed = it,
                                label = profile.a,
                            )
                        },
                        Id.Key(KeyEvent.KEYCODE_BUTTON_B) to {
                            LemuroidButtonForeground(
                                pressed = it,
                                label = profile.b,
                            )
                        },
                        Id.Key(KeyEvent.KEYCODE_BUTTON_Y) to {
                            LemuroidButtonForeground(
                                pressed = it,
                                label = profile.y,
                            )
                        },
                        Id.Key(KeyEvent.KEYCODE_BUTTON_X) to {
                            LemuroidButtonForeground(
                                pressed = it,
                                label = profile.x,
                            )
                        },
                    ),
            )
        },
        secondaryDials = {
            LemuroidControlButton(
                Modifier.radialPosition(90f),
                Id.Key(
                    if (profile == WiiLayoutProfile.SIDEWAYS) {
                        KeyEvent.KEYCODE_BUTTON_START
                    } else {
                        KeyEvent.KEYCODE_BUTTON_R1
                    },
                ),
                label = if (profile == WiiLayoutProfile.CLASSIC) "ZR" else "+",
            )
            LemuroidControlButton(
                Modifier.radialPosition(60f),
                Id.Key(KeyEvent.KEYCODE_BUTTON_R2),
                label = if (profile == WiiLayoutProfile.CLASSIC) "R" else "Shake",
            )
            SecondaryButtonStart(position = 2)
            SecondaryAnalogRight()
            SecondaryButtonMenu(settings)
        },
    )
}
