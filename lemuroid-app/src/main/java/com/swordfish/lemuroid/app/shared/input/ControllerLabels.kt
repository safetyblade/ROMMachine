package com.swordfish.lemuroid.app.shared.input

import android.view.KeyEvent

/** Player vocabulary; raw Android names belong only in the voluntary tester. */
object ControllerLabels {
    fun input(
        code: Int,
        family: ControllerHardwareProfile.Family = ControllerHardwareProfile.Family.GENERIC,
    ): String =
        when (code) {
            KeyEvent.KEYCODE_DPAD_UP -> "D-pad Up"
            KeyEvent.KEYCODE_DPAD_DOWN -> "D-pad Down"
            KeyEvent.KEYCODE_DPAD_LEFT -> "D-pad Left"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "D-pad Right"
            KeyEvent.KEYCODE_BUTTON_THUMBL -> "L3"
            KeyEvent.KEYCODE_BUTTON_THUMBR -> "R3"
            KeyEvent.KEYCODE_BUTTON_L1 -> if (family == ControllerHardwareProfile.Family.XBOX) "LB" else "L1"
            KeyEvent.KEYCODE_BUTTON_R1 -> if (family == ControllerHardwareProfile.Family.XBOX) "RB" else "R1"
            KeyEvent.KEYCODE_BUTTON_L2 -> if (family == ControllerHardwareProfile.Family.XBOX) "LT" else "L2"
            KeyEvent.KEYCODE_BUTTON_R2 -> if (family == ControllerHardwareProfile.Family.XBOX) "RT" else "R2"
            KeyEvent.KEYCODE_BUTTON_START -> "Start"
            KeyEvent.KEYCODE_BUTTON_SELECT -> "Select / View"
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_BUTTON_MODE -> "Menu / Guide"
            KeyEvent.KEYCODE_BACK -> "Back"
            KeyEvent.KEYCODE_BUTTON_A -> face(family, "A", "Cross", "B", "South face button")
            KeyEvent.KEYCODE_BUTTON_B -> face(family, "B", "Circle", "A", "East face button")
            KeyEvent.KEYCODE_BUTTON_X -> face(family, "X", "Square", "Y", "West face button")
            KeyEvent.KEYCODE_BUTTON_Y -> face(family, "Y", "Triangle", "X", "North face button")
            KeyEvent.KEYCODE_UNKNOWN -> "Unassigned"
            else ->
                if (code in KeyEvent.KEYCODE_BUTTON_1..KeyEvent.KEYCODE_BUTTON_16) {
                    "Button ${code - KeyEvent.KEYCODE_BUTTON_1 + 1}"
                } else {
                    "Other controller button"
                }
        }

    private fun face(
        family: ControllerHardwareProfile.Family,
        xbox: String,
        sony: String,
        switch: String,
        generic: String,
    ): String =
        when (family) {
            ControllerHardwareProfile.Family.XBOX -> xbox
            ControllerHardwareProfile.Family.PLAYSTATION -> sony
            ControllerHardwareProfile.Family.SWITCH -> switch
            else -> generic
        }

    fun xboxOutput(code: Int): String =
        when (code) {
            KeyEvent.KEYCODE_BUTTON_L1 -> "White"
            KeyEvent.KEYCODE_BUTTON_R1 -> "Black"
            KeyEvent.KEYCODE_BUTTON_THUMBL -> "Left Stick Press"
            KeyEvent.KEYCODE_BUTTON_THUMBR -> "Right Stick Press"
            KeyEvent.KEYCODE_BUTTON_L2 -> "Left Trigger"
            KeyEvent.KEYCODE_BUTTON_R2 -> "Right Trigger"
            KeyEvent.KEYCODE_BUTTON_SELECT -> "Back"
            else -> input(code, ControllerHardwareProfile.Family.XBOX)
        }
}
