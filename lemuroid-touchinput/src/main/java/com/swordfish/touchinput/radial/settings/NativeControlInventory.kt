package com.swordfish.touchinput.radial.settings

import android.view.KeyEvent
import com.swordfish.touchinput.radial.layouts.shared.ComposeTouchLayouts

/** Native configuration metadata; combined layouts must consume this exact inventory, never a system guess. */
enum class NativeControlKind { DPAD, FACE, SHOULDER, UTILITY, ANALOG }

enum class NativeControlIcon { CIRCLE, CROSS, TRIANGLE, SQUARE, START, SELECT, MENU }

data class NativeControlSpec(
    val key: String,
    val seat: String,
    val kind: NativeControlKind,
    val keyCode: Int? = null,
    val motionSource: Int? = null,
    val pressCode: Int? = null,
    val label: String = key,
    val icon: NativeControlIcon? = null,
    val leftHand: Boolean = false,
    val faceX: Float = 0f,
    val faceY: Float = 0f,
    val roundButton: Boolean = false,
)

data class NativeControlInventory(
    val controller: TouchControllerID,
    val controls: List<NativeControlSpec>,
    val supportsGripArc: Boolean = true,
) {
    init {
        require(controls.map { it.key }.distinct().size == controls.size)
        require(
            controls.all {
                if (it.kind in setOf(NativeControlKind.DPAD, NativeControlKind.ANALOG)) {
                    it.motionSource != null
                } else {
                    it.keyCode != null
                }
            },
        )
    }

    val inputIds: Set<String> get() =
        controls.flatMap { spec ->
            buildList {
                spec.keyCode?.let { add("key:$it") }
                spec.pressCode?.let { add("key:$it") }
                spec.motionSource?.let { add("${if (spec.kind == NativeControlKind.DPAD) "dpad" else "analog"}:$it") }
            }
        }.toSet()

    fun matchesNativeInputs(observed: Set<String>): Boolean = inputIds == observed

    companion object {
        fun forController(id: TouchControllerID): NativeControlInventory? = inventory(id, allowTrayOnly = false)

        fun forHoldTray(id: TouchControllerID): NativeControlInventory? = inventory(id, allowTrayOnly = true)

        private fun inventory(
            id: TouchControllerID,
            allowTrayOnly: Boolean,
        ): NativeControlInventory? {
            val sony = id in setOf(TouchControllerID.PSX, TouchControllerID.PSX_DUALSHOCK, TouchControllerID.PSP)
            val twoFace =
                id in
                    setOf(
                        TouchControllerID.GB, TouchControllerID.GBA, TouchControllerID.GG,
                        TouchControllerID.LYNX, TouchControllerID.NGP, TouchControllerID.WS_LANDSCAPE,
                    )
            val supported =
                setOf(
                    TouchControllerID.GB, TouchControllerID.GBA, TouchControllerID.GG, TouchControllerID.LYNX,
                    TouchControllerID.NGP, TouchControllerID.SNES, TouchControllerID.PSX,
                    TouchControllerID.PSX_DUALSHOCK, TouchControllerID.PSP,
                    TouchControllerID.GAMECUBE, TouchControllerID.DREAMCAST,
                    TouchControllerID.XBOX, TouchControllerID.XBOX360,
                )
            val standardTrayOnly =
                setOf(
                    TouchControllerID.MELONDS,
                    TouchControllerID.NINTENDO_3DS,
                    TouchControllerID.WS_LANDSCAPE,
                    TouchControllerID.WS_PORTRAIT,
                )
            if (id !in supported && (!allowTrayOnly || id !in standardTrayOnly)) return null
            val result =
                mutableListOf(
                    NativeControlSpec(
                        "D",
                        "dpad",
                        NativeControlKind.DPAD,
                        motionSource = ComposeTouchLayouts.MOTION_SOURCE_DPAD,
                        leftHand = true,
                    ),
                )
            val faces =
                when (id) {
                    TouchControllerID.GAMECUBE -> listOf("X", "A", "B", "Y")
                    TouchControllerID.DREAMCAST, TouchControllerID.XBOX, TouchControllerID.XBOX360 ->
                        listOf(
                            "B",
                            "A",
                            "X",
                            "Y",
                        )
                    else -> listOf("A", "B", "Y", "X")
                }
            val code =
                mapOf(
                    "A" to KeyEvent.KEYCODE_BUTTON_A,
                    "B" to KeyEvent.KEYCODE_BUTTON_B,
                    "X" to KeyEvent.KEYCODE_BUTTON_X,
                    "Y" to KeyEvent.KEYCODE_BUTTON_Y,
                )
            val dc =
                mapOf(
                    "A" to KeyEvent.KEYCODE_BUTTON_B,
                    "B" to KeyEvent.KEYCODE_BUTTON_A,
                    "X" to KeyEvent.KEYCODE_BUTTON_Y,
                    "Y" to KeyEvent.KEYCODE_BUTTON_X,
                )
            val icons =
                mapOf(
                    "A" to NativeControlIcon.CIRCLE,
                    "B" to NativeControlIcon.CROSS,
                    "X" to NativeControlIcon.TRIANGLE,
                    "Y" to NativeControlIcon.SQUARE,
                )
            val offsets =
                if (twoFace) {
                    listOf(
                        0.866f to -0.5f,
                        -0.866f to 0.5f,
                    )
                } else {
                    listOf(1f to 0f, 0f to 1f, -1f to 0f, 0f to -1f)
                }
            (if (twoFace) listOf("A", "B") else faces).zip(offsets).forEach { (key, offset) ->
                result +=
                    NativeControlSpec(
                        key, if (twoFace) "face-ab" else "face-abxy", NativeControlKind.FACE,
                        keyCode = (if (id == TouchControllerID.DREAMCAST) dc else code).getValue(key),
                        icon = if (sony) icons[key] else null,
                        label =
                            when (id) {
                                TouchControllerID.GG -> if (key == "A") "2" else "1"
                                TouchControllerID.NGP -> if (key == "A") "B" else "A"
                                TouchControllerID.WS_PORTRAIT ->
                                    when (key) {
                                        "A" -> "X3"
                                        "B" -> "X4"
                                        "Y" -> "X1"
                                        else -> "X2"
                                    }
                                else -> key
                            },
                        faceX = offset.first, faceY = offset.second,
                    )
            }

            fun button(
                key: String,
                seat: String,
                value: Int,
                label: String = key,
                icon: NativeControlIcon? = null,
                left: Boolean = false,
            ) {
                result +=
                    NativeControlSpec(
                        key, seat,
                        if (key in setOf("Select", "Start", "Menu")) {
                            NativeControlKind.UTILITY
                        } else {
                            NativeControlKind.SHOULDER
                        },
                        keyCode = value, label = label, icon = icon, leftHand = left,
                        roundButton = key == "Z",
                    )
            }
            if (id !in
                setOf(
                    TouchControllerID.GB, TouchControllerID.GG, TouchControllerID.NGP,
                    TouchControllerID.WS_LANDSCAPE, TouchControllerID.WS_PORTRAIT,
                )
            ) {
                val dcTriggers = id == TouchControllerID.DREAMCAST
                val numbered = id in setOf(TouchControllerID.PSX, TouchControllerID.PSX_DUALSHOCK)
                button(
                    "L",
                    if (numbered) {
                        "shoulder-L1"
                    } else if (id in setOf(TouchControllerID.XBOX, TouchControllerID.XBOX360)) {
                        "shoulder-L1"
                    } else {
                        "shoulder-L"
                    },
                    if (dcTriggers) KeyEvent.KEYCODE_BUTTON_L2 else KeyEvent.KEYCODE_BUTTON_L1,
                    if (numbered) {
                        "L1"
                    } else if (id == TouchControllerID.LYNX) {
                        "Option 1"
                    } else if (id in setOf(TouchControllerID.XBOX, TouchControllerID.XBOX360)) {
                        if (id == TouchControllerID.XBOX360) "LB" else "White"
                    } else {
                        "L"
                    },
                    left = true,
                )
                button(
                    "R",
                    if (numbered) {
                        "shoulder-R1"
                    } else if (id in setOf(TouchControllerID.XBOX, TouchControllerID.XBOX360)) {
                        "shoulder-R1"
                    } else {
                        "shoulder-R"
                    },
                    if (dcTriggers) KeyEvent.KEYCODE_BUTTON_R2 else KeyEvent.KEYCODE_BUTTON_R1,
                    if (numbered) {
                        "R1"
                    } else if (id == TouchControllerID.LYNX) {
                        "Option 2"
                    } else if (id in setOf(TouchControllerID.XBOX, TouchControllerID.XBOX360)) {
                        if (id == TouchControllerID.XBOX360) "RB" else "Black"
                    } else {
                        "R"
                    },
                )
            }
            if (id in
                setOf(
                    TouchControllerID.PSX, TouchControllerID.PSX_DUALSHOCK,
                    TouchControllerID.XBOX, TouchControllerID.XBOX360,
                )
            ) {
                button(
                    "L2",
                    "shoulder-L2",
                    KeyEvent.KEYCODE_BUTTON_L2,
                    if (id in setOf(TouchControllerID.XBOX, TouchControllerID.XBOX360)) "LT" else "L2",
                    left = true,
                )
                button(
                    "R2",
                    "shoulder-R2",
                    KeyEvent.KEYCODE_BUTTON_R2,
                    if (id in setOf(TouchControllerID.XBOX, TouchControllerID.XBOX360)) "RT" else "R2",
                )
            }
            if (id == TouchControllerID.GAMECUBE) button("Z", "aux-Z", KeyEvent.KEYCODE_BUTTON_R2)
            if (id == TouchControllerID.MELONDS) {
                button("Mic", "ds-mic", KeyEvent.KEYCODE_BUTTON_L2, left = true)
                button("Lid", "ds-layout", KeyEvent.KEYCODE_BUTTON_THUMBL, left = true)
            }
            if (id !in
                setOf(
                    TouchControllerID.GAMECUBE, TouchControllerID.DREAMCAST,
                    TouchControllerID.GG, TouchControllerID.LYNX, TouchControllerID.NGP,
                    TouchControllerID.WS_LANDSCAPE, TouchControllerID.WS_PORTRAIT,
                )
            ) {
                button(
                    "Select",
                    "select",
                    KeyEvent.KEYCODE_BUTTON_SELECT,
                    if (id in setOf(TouchControllerID.XBOX, TouchControllerID.XBOX360)) {
                        "Back"
                    } else {
                        "Select"
                    },
                    if (id in setOf(TouchControllerID.XBOX, TouchControllerID.XBOX360)) {
                        null
                    } else {
                        NativeControlIcon.SELECT
                    },
                    true,
                )
            }
            button(
                "Start",
                "start",
                KeyEvent.KEYCODE_BUTTON_START,
                label = if (id == TouchControllerID.LYNX) "Pause" else "Start",
                icon = NativeControlIcon.START,
            )
            button("Menu", "menu", KeyEvent.KEYCODE_BUTTON_MODE, icon = NativeControlIcon.MENU)
            if (id in
                setOf(
                    TouchControllerID.PSX_DUALSHOCK, TouchControllerID.PSP,
                    TouchControllerID.GAMECUBE, TouchControllerID.DREAMCAST,
                    TouchControllerID.XBOX, TouchControllerID.XBOX360, TouchControllerID.NINTENDO_3DS,
                )
            ) {
                result +=
                    NativeControlSpec(
                        "AnalogL", "analog-left", NativeControlKind.ANALOG,
                        motionSource = ComposeTouchLayouts.MOTION_SOURCE_LEFT_STICK,
                        pressCode = KeyEvent.KEYCODE_BUTTON_THUMBL, leftHand = true,
                    )
            }
            if (id in
                setOf(
                    TouchControllerID.PSX_DUALSHOCK, TouchControllerID.GAMECUBE,
                    TouchControllerID.XBOX, TouchControllerID.XBOX360,
                )
            ) {
                result +=
                    NativeControlSpec(
                        "AnalogR", "analog-right", NativeControlKind.ANALOG,
                        motionSource = ComposeTouchLayouts.MOTION_SOURCE_RIGHT_STICK,
                        pressCode = KeyEvent.KEYCODE_BUTTON_THUMBR,
                        label = if (id == TouchControllerID.GAMECUBE) "C-Stick" else "AnalogR",
                    )
            }
            return NativeControlInventory(id, result, supportsGripArc = id in supported)
        }
    }
}
