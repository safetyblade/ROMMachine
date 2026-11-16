package com.swordfish.lemuroid.app.shared.input

/** Voluntary diagnostics only. The probe never forwards or synthesizes guest input. */
class ControllerInputProbe(private val clockMillis: () -> Long = { System.nanoTime() / 1_000_000 }) {
    data class Axis(val value: Float, val minimum: Float, val maximum: Float)

    val physical = PhysicalControllerInput.State()

    fun motion(
        event: android.view.MotionEvent,
        profile: ControllerProfileStore.Profile = ControllerProfileStore.Profile(),
    ) {
        val ranges = event.device?.let(PhysicalControllerInput::hardware)?.ranges.orEmpty()
        physical.motion({ event.getAxisValue(it) }, ranges, profile)
    }

    private val since = mutableMapOf<Int, Long>()
    private val repeated = mutableMapOf<Int, Int>()
    val repeatedKeys: Set<Int> get() = repeated.filterValues { it > 4 }.keys

    fun heldLongerThan(milliseconds: Long): Set<Int> = since.filterValues { clockMillis() - it >= milliseconds }.keys

    private val buttons = mutableSetOf<Int>()
    val held: Set<Int> get() = buttons.toSet()
    val axes = mutableMapOf<Int, Axis>()
    var lastEvent: String = "Press a controller button"
        private set

    fun key(
        code: Int,
        down: Boolean,
    ) {
        physical.key(code, down)
        if (down) {
            if (buttons.add(code)) {
                since[code] = clockMillis()
            } else {
                repeated[code] = (repeated[code] ?: 0) + 1
            }
        } else {
            buttons.remove(code)
            since.remove(code)
            repeated.remove(code)
        }
        lastEvent = "$code ${if (down) "pressed" else "released"}"
    }

    fun axis(
        code: Int,
        value: Float,
    ) {
        if (!value.isFinite()) return
        val old = axes[code]
        axes[code] = Axis(value, minOf(old?.minimum ?: value, value), maxOf(old?.maximum ?: value, value))
    }

    fun clear() {
        since.clear()
        repeated.clear()
        physical.clear()
        buttons.clear()
        axes.clear()
        lastEvent = "Input cleared"
    }
}
