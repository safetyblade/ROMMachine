package com.swordfish.lemuroid.app.shared.input

/** Axis hats are four independent digital sources, so diagonals preserve both directions. */
class ControllerHatEdges {
    private var held = emptySet<Int>()

    fun update(
        x: Float,
        y: Float,
    ): List<Pair<Int, Boolean>> {
        val next =
            buildSet {
                if (x < -0.5f) add(21)
                if (x > 0.5f) add(22)
                if (y < -0.5f) add(19)
                if (y > 0.5f) add(20)
            }
        val transitions = (held - next).map { it to false } + (next - held).map { it to true }
        held = next
        return transitions
    }

    fun clear() {
        held = emptySet()
    }
}
