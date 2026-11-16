package com.swordfish.lemuroid.app.shared.input

/** Translate persisted RetroPad positions at existing native boundaries, as the PS3 host already does. */
object NativeControllerKeyMapping {
    fun output(
        source: Int,
        bindings: Map<Int, Int>,
    ): Int = output(source, bindings[source])

    fun output(
        source: Int,
        target: Int?,
    ): Int =
        when (target) {
            null, 0 -> source
            96 -> 97
            97 -> 96
            99 -> 100
            100 -> 99
            19, 20, 21, 22, 102, 103, 104, 105, 106, 107, 108, 109, 110 -> target
            else -> source
        }

    /** A preference change cannot reroute the release edge of an already accepted press. */
    class HeldOutputs {
        private val held = mutableMapOf<Pair<Int, Int>, Int>()

        fun key(
            deviceId: Int,
            source: Int,
            down: Boolean,
            target: Int?,
        ): Int {
            val owner = deviceId to source
            return if (down) {
                held.getOrPut(owner) { output(source, target) }
            } else {
                held.remove(owner) ?: output(source, target)
            }
        }

        fun retainDevices(ids: Set<Int>) {
            held.keys.removeAll { it.first !in ids }
        }

        fun clear() = held.clear()
    }
}
