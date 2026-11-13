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

data class NativeControlInventory(val controller: TouchControllerID, val controls: List<NativeControlSpec>) {
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
        fun forController(id: TouchControllerID): NativeControlInventory? {
            val sony = id in setOf(TouchControllerID.PSX, TouchControllerID.PSX_DUALSHOCK, TouchControllerID.PSP)
            val twoFace = id in setOf(TouchControllerID.GB, TouchControllerID.GBA)
            val supported =
                setOf(
                    TouchControllerID.GB, TouchControllerID.GBA, TouchControllerID.SNES, TouchControllerID.PSX,
                    TouchControllerID.PSX_DUALSHOCK, TouchControllerID.PSP,
                    TouchControllerID.GAMECUBE, TouchControllerID.DREAMCAST,
                    TouchControllerID.XBOX,
                )
            // Unmapped auxiliary variants retain native/Standard until their complete measured contract exists.
            if (id !in supported) return null
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
                    TouchControllerID.DREAMCAST, TouchControllerID.XBOX -> listOf("B", "A", "X", "Y")
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
                        icon = if (sony) icons[key] else null, faceX = offset.first, faceY = offset.second,
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
            if (id != TouchControllerID.GB) {
                val dcTriggers = id == TouchControllerID.DREAMCAST
                val numbered = id in setOf(TouchControllerID.PSX, TouchControllerID.PSX_DUALSHOCK)
                button(
                    "L",
                    if (numbered) {
                        "shoulder-L1"
                    } else if (id == TouchControllerID.XBOX) {
                        "shoulder-L1"
                    } else {
                        "shoulder-L"
                    },
                    if (dcTriggers) KeyEvent.KEYCODE_BUTTON_L2 else KeyEvent.KEYCODE_BUTTON_L1,
                    if (numbered) {
                        "L1"
                    } else if (id == TouchControllerID.XBOX) {
                        "White"
                    } else {
                        "L"
                    },
                    left = true,
                )
                button(
                    "R",
                    if (numbered) {
                        "shoulder-R1"
                    } else if (id == TouchControllerID.XBOX) {
                        "shoulder-R1"
                    } else {
                        "shoulder-R"
                    },
                    if (dcTriggers) KeyEvent.KEYCODE_BUTTON_R2 else KeyEvent.KEYCODE_BUTTON_R1,
                    if (numbered) {
                        "R1"
                    } else if (id == TouchControllerID.XBOX) {
                        "Black"
                    } else {
                        "R"
                    },
                )
            }
            if (id in setOf(TouchControllerID.PSX, TouchControllerID.PSX_DUALSHOCK, TouchControllerID.XBOX)) {
                button(
                    "L2",
                    "shoulder-L2",
                    KeyEvent.KEYCODE_BUTTON_L2,
                    if (id == TouchControllerID.XBOX) "LT" else "L2",
                    left = true,
                )
                button(
                    "R2",
                    "shoulder-R2",
                    KeyEvent.KEYCODE_BUTTON_R2,
                    if (id == TouchControllerID.XBOX) "RT" else "R2",
                )
            }
            if (id == TouchControllerID.GAMECUBE) button("Z", "aux-Z", KeyEvent.KEYCODE_BUTTON_R2)
            if (id !in setOf(TouchControllerID.GAMECUBE, TouchControllerID.DREAMCAST)) {
                button(
                    "Select",
                    "select",
                    KeyEvent.KEYCODE_BUTTON_SELECT,
                    if (id == TouchControllerID.XBOX) "Back" else "Select",
                    if (id == TouchControllerID.XBOX) null else NativeControlIcon.SELECT,
                    true,
                )
            }
            button("Start", "start", KeyEvent.KEYCODE_BUTTON_START, icon = NativeControlIcon.START)
            button("Menu", "menu", KeyEvent.KEYCODE_BUTTON_MODE, icon = NativeControlIcon.MENU)
            if (id in
                setOf(
                    TouchControllerID.PSX_DUALSHOCK, TouchControllerID.PSP,
                    TouchControllerID.GAMECUBE, TouchControllerID.DREAMCAST, TouchControllerID.XBOX,
                )
            ) {
                result +=
                    NativeControlSpec(
                        "AnalogL", "analog-left", NativeControlKind.ANALOG,
                        motionSource = ComposeTouchLayouts.MOTION_SOURCE_LEFT_STICK,
                        pressCode = KeyEvent.KEYCODE_BUTTON_THUMBL, leftHand = true,
                    )
            }
            if (id in setOf(TouchControllerID.PSX_DUALSHOCK, TouchControllerID.GAMECUBE, TouchControllerID.XBOX)) {
                result +=
                    NativeControlSpec(
                        "AnalogR", "analog-right", NativeControlKind.ANALOG,
                        motionSource = ComposeTouchLayouts.MOTION_SOURCE_RIGHT_STICK,
                        pressCode = KeyEvent.KEYCODE_BUTTON_THUMBR,
                        label = if (id == TouchControllerID.GAMECUBE) "C-Stick" else "AnalogR",
                    )
            }
            return NativeControlInventory(id, result)
        }
    }
}
