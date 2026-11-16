package com.swordfish.lemuroid.app.shared.input

/** Voluntary diagnostics only. The probe never forwards or synthesizes guest input. */
class ControllerInputProbe {
    data class Axis(val value: Float, val minimum: Float, val maximum: Float)

    val physical = PhysicalControllerInput.State()

    fun motion(
        event: android.view.MotionEvent,
        profile: ControllerProfileStore.Profile = ControllerProfileStore.Profile(),
    ) {
        val ranges = event.device?.let(PhysicalControllerInput::hardware)?.ranges.orEmpty()
        physical.motion({ event.getAxisValue(it) }, ranges, profile)
    }

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
        if (down) buttons.add(code) else buttons.remove(code)
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
        physical.clear()
        buttons.clear()
        axes.clear()
        lastEvent = "Input cleared"
    }
}
