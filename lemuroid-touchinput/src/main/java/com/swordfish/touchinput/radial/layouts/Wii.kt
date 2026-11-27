package com.swordfish.touchinput.radial.layouts

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.swordfish.touchinput.radial.controls.LemuroidControlAnalog
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

/** null preserves the ordinary radial layout used outside the Wii gameplay screen. */
val LocalWiiNunchukPointerStick = staticCompositionLocalOf<Boolean?> { null }

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
            if (profile == WiiLayoutProfile.SIDEWAYS) {
                // Sideways Remote: no Classic shoulders or analog sticks.
                // Keep the exact Dolphin RetroPad IDs used by the working Wii host.
                LemuroidControlButton(
                    Modifier.radialPosition(90f),
                    Id.Key(KeyEvent.KEYCODE_BUTTON_SELECT),
                    label = "-",
                )
                LemuroidControlButton(
                    Modifier.radialPosition(120f),
                    Id.Key(KeyEvent.KEYCODE_BUTTON_THUMBR),
                    label = "Home",
                )
                SecondaryButtonMenuPlaceholder(settings)
            } else {
                LemuroidControlButton(
                    Modifier.radialPosition(90f),
                    Id.Key(KeyEvent.KEYCODE_BUTTON_L1),
                    label = if (profile == WiiLayoutProfile.CLASSIC) "ZL" else "-",
                )
                LemuroidControlButton(
                    Modifier.radialPosition(120f),
                    Id.Key(KeyEvent.KEYCODE_BUTTON_L2),
                    label = if (profile == WiiLayoutProfile.CLASSIC) "L" else "Shake N",
                )
                if (profile == WiiLayoutProfile.NUNCHUK && LocalWiiNunchukPointerStick.current != null) {
                    LemuroidControlButton(
                        Modifier.radialPosition(60f),
                        Id.Key(KeyEvent.KEYCODE_BUTTON_SELECT),
                        label = "2",
                    )
                    LemuroidControlButton(
                        Modifier.radialPosition(150f),
                        Id.Key(KeyEvent.KEYCODE_BUTTON_THUMBR),
                        label = "Home",
                    )
                } else {
                    SecondaryButtonSelect(position = 2)
                }
                SecondaryButtonMenuPlaceholder(settings)
                SecondaryAnalogLeft()
            }
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
            if (profile == WiiLayoutProfile.SIDEWAYS) {
                LemuroidControlButton(
                    Modifier.radialPosition(90f),
                    Id.Key(KeyEvent.KEYCODE_BUTTON_START),
                    label = "+",
                )
                LemuroidControlButton(
                    Modifier.radialPosition(60f),
                    Id.Key(KeyEvent.KEYCODE_BUTTON_R2),
                    label = "Shake",
                )
                // Keep CARTDECK Menu separate from the emulated Wii Home button.
                SecondaryButtonMenu(settings)
            } else {
                LemuroidControlButton(
                    Modifier.radialPosition(90f),
                    Id.Key(KeyEvent.KEYCODE_BUTTON_R1),
                    label = if (profile == WiiLayoutProfile.CLASSIC) "ZR" else "+",
                )
                LemuroidControlButton(
                    Modifier.radialPosition(60f),
                    Id.Key(KeyEvent.KEYCODE_BUTTON_R2),
                    label = if (profile == WiiLayoutProfile.CLASSIC) "R" else "Shake",
                )
                val pointerStick = LocalWiiNunchukPointerStick.current
                if (profile == WiiLayoutProfile.NUNCHUK && pointerStick != null) {
                    LemuroidControlButton(
                        Modifier.radialPosition(120f),
                        Id.Key(KeyEvent.KEYCODE_BUTTON_START),
                        label = "1",
                    )
                    if (pointerStick) {
                        LemuroidControlAnalog(
                            modifier = Modifier.radialPosition(-100f).radialScale(2f),
                            id = Id.ContinuousDirection(ComposeTouchLayouts.MOTION_SOURCE_RIGHT_STICK),
                            hardwareSeatId = "analog-right",
                        )
                    }
                } else {
                    SecondaryButtonStart(position = 2)
                    SecondaryAnalogRight()
                }
                SecondaryButtonMenu(settings)
            }
        },
    )
}
