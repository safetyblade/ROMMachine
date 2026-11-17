package com.swordfish.lemuroid.app.shared.input

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.abs

/** Physical positions, independent of printed labels and emulated-console semantics. */
object PhysicalControllerInput {
    enum class Control {
        FACE_SOUTH,
        FACE_EAST,
        FACE_WEST,
        FACE_NORTH,
        DPAD_UP,
        DPAD_DOWN,
        DPAD_LEFT,
        DPAD_RIGHT,
        STICK_L_X,
        STICK_L_Y,
        STICK_R_X,
        STICK_R_Y,
        STICK_L_PRESS,
        STICK_R_PRESS,
        BUMPER_L,
        BUMPER_R,
        TRIGGER_L,
        TRIGGER_R,
        START,
        SELECT,
        MENU,
    }

    data class Range(val minimum: Float, val maximum: Float, val flat: Float = 0f)

    data class Hardware(
        val descriptor: String,
        val vendor: Int,
        val product: Int,
        val sources: Int,
        val family: ControllerHardwareProfile.Family,
        val keys: Set<Int>,
        val ranges: Map<Int, Range>,
    )

    val keys =
        mapOf(
            KeyEvent.KEYCODE_BUTTON_A to Control.FACE_SOUTH,
            KeyEvent.KEYCODE_BUTTON_B to Control.FACE_EAST,
            KeyEvent.KEYCODE_BUTTON_X to Control.FACE_WEST,
            KeyEvent.KEYCODE_BUTTON_Y to Control.FACE_NORTH,
            KeyEvent.KEYCODE_DPAD_UP to Control.DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN to Control.DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT to Control.DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT to Control.DPAD_RIGHT,
            KeyEvent.KEYCODE_BUTTON_THUMBL to Control.STICK_L_PRESS,
            KeyEvent.KEYCODE_BUTTON_THUMBR to Control.STICK_R_PRESS,
            KeyEvent.KEYCODE_BUTTON_L1 to Control.BUMPER_L,
            KeyEvent.KEYCODE_BUTTON_R1 to Control.BUMPER_R,
            KeyEvent.KEYCODE_BUTTON_L2 to Control.TRIGGER_L,
            KeyEvent.KEYCODE_BUTTON_R2 to Control.TRIGGER_R,
            KeyEvent.KEYCODE_BUTTON_START to Control.START,
            KeyEvent.KEYCODE_BUTTON_SELECT to Control.SELECT,
            KeyEvent.KEYCODE_BUTTON_MODE to Control.MENU,
            KeyEvent.KEYCODE_MENU to Control.MENU,
        )

    fun hardware(device: InputDevice): Hardware {
        val codes = keys.keys.toIntArray()
        val available = runCatching { device.hasKeys(*codes) }.getOrDefault(BooleanArray(codes.size))
        return Hardware(
            device.descriptor,
            device.vendorId,
            device.productId,
            device.sources,
            ControllerHardwareProfile.family(device),
            codes.filterIndexed { index, _ -> available[index] }.toSet(),
            device.motionRanges.associate { it.axis to Range(it.min, it.max, it.flat) },
        )
    }

    /** Explicit calibration is optional; driver-provided flat and range remain the safe defaults. */
    fun axis(
        value: Float,
        range: Range?,
        trigger: Boolean,
        deadzone: Float = 0f,
        centre: Float? = null,
    ): Float {
        if (!value.isFinite()) return 0f
        val min = range?.minimum ?: if (trigger) 0f else -1f
        val max = range?.maximum ?: 1f
        if (max <= min) return 0f
        if (trigger) return ((value - min) / (max - min)).coerceIn(0f, 1f)
        val middle = (centre ?: if (min < 0f && max > 0f) 0f else (min + max) / 2f).coerceIn(min, max)
        val span = if (value >= middle) max - middle else middle - min
        if (span <= 0f) return 0f
        val normalized = ((value - middle) / span).coerceIn(-1f, 1f)
        val flat = maxOf(deadzone, (range?.flat ?: 0f) / span).coerceIn(0f, 0.95f)
        return if (abs(normalized) <= flat) {
            0f
        } else {
            ((abs(normalized) - flat) / (1f - flat)) * if (normalized < 0f) -1f else 1f
        }
    }

    fun rightAxes(axes: Set<Int>): Pair<Int, Int> =
        if (MotionEvent.AXIS_Z in axes && MotionEvent.AXIS_RZ in axes) {
            MotionEvent.AXIS_Z to MotionEvent.AXIS_RZ
        } else {
            MotionEvent.AXIS_RX to MotionEvent.AXIS_RY
        }

    fun sample(
        event: MotionEvent,
        requested: Int,
        profile: ControllerProfileStore.Profile,
    ): Float {
        val device = event.device
        val right = ControllerHardwareProfile.rightStickAxes(device)
        val actual =
            when (requested) {
                MotionEvent.AXIS_Z, MotionEvent.AXIS_RX -> profile.rightX ?: right.first
                MotionEvent.AXIS_RZ, MotionEvent.AXIS_RY -> profile.rightY ?: right.second
                else -> requested
            }
        val range = device?.getMotionRange(actual)?.let { Range(it.min, it.max, it.flat) }
        val trigger =
            actual in
                setOf(
                    MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_RTRIGGER,
                    MotionEvent.AXIS_BRAKE, MotionEvent.AXIS_GAS, MotionEvent.AXIS_THROTTLE,
                )
        val stick =
            actual in
                setOf(
                    MotionEvent.AXIS_X, MotionEvent.AXIS_Y,
                    MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ, MotionEvent.AXIS_RX, MotionEvent.AXIS_RY,
                )
        return axis(
            event.getAxisValue(actual),
            range,
            trigger,
            if (stick) profile.deadzone else 0f,
            profile.centres[actual],
        )
    }

    class State {
        private val held = mutableSetOf<Int>()
        private val analog = mutableMapOf<Control, Float>()
        val observedKeys = mutableSetOf<Int>()
        val values: Map<Control, Float>
            get() {
                val result = analog.toMutableMap()
                held.mapNotNull(keys::get).forEach { result[it] = 1f }
                return result
            }

        fun key(
            code: Int,
            down: Boolean,
        ) {
            observedKeys.add(code)
            if (down) held.add(code) else held.remove(code)
        }

        fun motion(
            raw: (Int) -> Float,
            ranges: Map<Int, Range>,
            profile: ControllerProfileStore.Profile = ControllerProfileStore.Profile(),
        ) {
            val reported = rightAxes(ranges.keys)
            val right = (profile.rightX ?: reported.first) to (profile.rightY ?: reported.second)
            listOf(
                Triple(Control.STICK_L_X, MotionEvent.AXIS_X, false),
                Triple(Control.STICK_L_Y, MotionEvent.AXIS_Y, false),
                Triple(Control.STICK_R_X, right.first, false),
                Triple(Control.STICK_R_Y, right.second, false),
            ).forEach { (control, code, trigger) ->
                if (code in ranges) {
                    analog[control] =
                        axis(
                            raw(code), ranges[code], trigger, profile.deadzone, profile.centres[code],
                        )
                }
            }
            analog[Control.TRIGGER_L] = listOf(MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_BRAKE)
                .filter { it in ranges }.maxOfOrNull { axis(raw(it), ranges[it], true) } ?: 0f
            analog[Control.TRIGGER_R] = listOf(
                MotionEvent.AXIS_RTRIGGER, MotionEvent.AXIS_GAS, MotionEvent.AXIS_THROTTLE,
            )
                .filter { it in ranges }.maxOfOrNull { axis(raw(it), ranges[it], true) } ?: 0f
            val x = raw(MotionEvent.AXIS_HAT_X)
            val y = raw(MotionEvent.AXIS_HAT_Y)
            analog[Control.DPAD_LEFT] = if (x < -0.5f) 1f else 0f
            analog[Control.DPAD_RIGHT] = if (x > 0.5f) 1f else 0f
            analog[Control.DPAD_UP] = if (y < -0.5f) 1f else 0f
            analog[Control.DPAD_DOWN] = if (y > 0.5f) 1f else 0f
        }

        fun clear() {
            held.clear()
            analog.clear()
            observedKeys.clear()
        }
    }
}
