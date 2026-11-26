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
        val codes = (keys.keys + (KeyEvent.KEYCODE_BUTTON_1..KeyEvent.KEYCODE_BUTTON_16)).toIntArray()
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
        if (!min.isFinite() || !max.isFinite() || max <= min) return 0f
        if (trigger) return ((value - min) / (max - min)).coerceIn(0f, 1f)
        val middle = (centre ?: if (min < 0f && max > 0f) 0f else (min + max) / 2f)
        if (!middle.isFinite() || middle <= min || middle >= max) return 0f
        val span = if (value >= middle) max - middle else middle - min
        if (span <= 0f) return 0f
        val normalized = ((value - middle) / span).coerceIn(-1f, 1f)
        val flat = maxOf(deadzone, (range?.flat ?: 0f) / span).coerceIn(0f, 0.95f)
        if (!flat.isFinite()) return 0f
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

    /** Use only ranges advertised for this event's joystick source. */
    fun eventRanges(event: MotionEvent): Map<Int, Range> {
        if (!event.isFromSource(InputDevice.SOURCE_JOYSTICK)) return emptyMap()
        return event.device?.motionRanges.orEmpty()
            .filter { it.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK }
            .filter { event.isFromSource(it.source) }
            .associate { it.axis to Range(it.min, it.max, it.flat) }
    }

    private fun signedStick(range: Range?): Boolean = range != null && range.minimum < 0f && range.maximum > 0f

    private fun stickSupported(
        code: Int,
        ranges: Map<Int, Range>,
        profile: ControllerProfileStore.Profile,
    ): Boolean {
        val range = ranges[code] ?: return false
        val centre = profile.centres[code]
        return signedStick(range) ||
            (centre != null && centre.isFinite() && centre > range.minimum && centre < range.maximum)
    }

    private fun resolvedRightAxes(
        ranges: Map<Int, Range>,
        profile: ControllerProfileStore.Profile,
    ): Pair<Int, Int>? {
        val candidates =
            listOfNotNull(
                profile.rightX?.let { x -> profile.rightY?.let { y -> x to y } },
                MotionEvent.AXIS_Z to MotionEvent.AXIS_RZ,
                MotionEvent.AXIS_RX to MotionEvent.AXIS_RY,
            )
        return candidates.firstOrNull { (x, y) ->
            x != y && stickSupported(x, ranges, profile) && stickSupported(y, ranges, profile)
        }
    }

    fun effectiveDeadzone(profile: ControllerProfileStore.Profile): Float =
        if (profile.calibrated || profile.deadzone > 0f) profile.deadzone else 0.10f

    /** Unadvertised axes and ambiguous unipolar stick axes are neutral, not full negative travel. */
    fun sampleAxis(
        raw: (Int) -> Float,
        ranges: Map<Int, Range>,
        requested: Int,
        profile: ControllerProfileStore.Profile,
    ): Float {
        val actual =
            when (requested) {
                MotionEvent.AXIS_Z, MotionEvent.AXIS_RX -> resolvedRightAxes(ranges, profile)?.first
                MotionEvent.AXIS_RZ, MotionEvent.AXIS_RY -> resolvedRightAxes(ranges, profile)?.second
                else -> requested
            } ?: return 0f
        val range = ranges[actual] ?: return 0f
        val value = raw(actual)
        if (!value.isFinite() || !range.minimum.isFinite() || !range.maximum.isFinite()) return 0f
        if (range.maximum <= range.minimum) return 0f
        if (actual == MotionEvent.AXIS_HAT_X || actual == MotionEvent.AXIS_HAT_Y) {
            return value.coerceIn(-1f, 1f)
        }
        val trigger =
            actual in
                setOf(
                    MotionEvent.AXIS_LTRIGGER,
                    MotionEvent.AXIS_RTRIGGER,
                    MotionEvent.AXIS_BRAKE,
                    MotionEvent.AXIS_GAS,
                    MotionEvent.AXIS_THROTTLE,
                )
        if (!trigger && !stickSupported(actual, ranges, profile)) return 0f
        return axis(value, range, trigger, effectiveDeadzone(profile), profile.centres[actual])
    }

    fun sample(
        event: MotionEvent,
        requested: Int,
        profile: ControllerProfileStore.Profile,
    ): Float = sampleAxis({ event.getAxisValue(it) }, eventRanges(event), requested, profile)

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
            listOf(
                Control.STICK_L_X to MotionEvent.AXIS_X,
                Control.STICK_L_Y to MotionEvent.AXIS_Y,
                Control.STICK_R_X to MotionEvent.AXIS_Z,
                Control.STICK_R_Y to MotionEvent.AXIS_RZ,
            ).forEach { (control, code) ->
                analog[control] = sampleAxis(raw, ranges, code, profile)
            }
            analog[Control.TRIGGER_L] = listOf(MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_BRAKE)
                .filter { it in ranges }.maxOfOrNull { axis(raw(it), ranges[it], true) } ?: 0f
            analog[Control.TRIGGER_R] = listOf(
                MotionEvent.AXIS_RTRIGGER, MotionEvent.AXIS_GAS, MotionEvent.AXIS_THROTTLE,
            )
                .filter { it in ranges }.maxOfOrNull { axis(raw(it), ranges[it], true) } ?: 0f
            val x = sampleAxis(raw, ranges, MotionEvent.AXIS_HAT_X, profile)
            val y = sampleAxis(raw, ranges, MotionEvent.AXIS_HAT_Y, profile)
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

/** A configuration-flow emission must never replay an already consumed Android event. */
internal class ControllerFrameGate {
    private var last = Long.MIN_VALUE

    fun accepts(
        sequence: Long,
        generation: Long,
        currentGeneration: Long,
    ): Boolean {
        if (generation != currentGeneration || sequence <= last) return false
        last = sequence
        return true
    }
}

/** Digital motion axes have a separate held-output set per guest port. */
internal class ControllerDigitalAxisState {
    data class Edge(val code: Int, val down: Boolean)

    private val pressed = mutableMapOf<Int, Set<Int>>()

    fun update(
        port: Int,
        next: Set<Int>,
    ): List<Edge> {
        val previous = pressed[port].orEmpty()
        pressed[port] = next.toSet()
        return (previous - next).map { Edge(it, false) } + (next - previous).map { Edge(it, true) }
    }

    fun clear() = pressed.clear()
}
