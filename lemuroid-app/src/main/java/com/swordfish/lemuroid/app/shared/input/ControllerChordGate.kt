package com.swordfish.lemuroid.app.shared.input

/** Buffer menu chord members so shortcuts cannot enter the guest; replay independent clicks on release. */
class ControllerChordGate {
    data class Edge(val key: Int, val down: Boolean)

    data class Decision(
        val consumed: Boolean,
        val invoke: Boolean = false,
        val replay: List<Edge> = emptyList(),
    )

    private data class Pending(
        val keys: Set<Int>,
        val held: MutableSet<Int> = mutableSetOf(),
        var fired: Boolean = false,
    )

    private val devices = mutableMapOf<Int, Pending>()

    fun route(
        device: Int,
        key: Int,
        down: Boolean,
        keys: Set<Int>,
        canceled: Boolean = false,
    ): Decision {
        if (canceled) {
            devices.remove(device)
            return Decision(true)
        }
        if (devices[device]?.held?.isEmpty() != false) {
            if (keys.size != 2) {
                devices.remove(device)
                return Decision(false)
            }
            if (devices[device]?.keys != keys) devices[device] = Pending(keys.toSet())
        }
        val state = devices[device] ?: return Decision(false)
        if (key !in state.keys || state.keys.size != 2) return Decision(false)
        if (down) {
            if (!state.held.add(key)) return Decision(true)
            if (!state.fired && state.held.containsAll(state.keys)) {
                state.fired = true
                return Decision(true, invoke = true)
            }
            return Decision(true)
        }
        if (!state.held.remove(key)) return Decision(true)
        val replay = if (state.fired) emptyList() else listOf(Edge(key, true), Edge(key, false))
        if (state.held.isEmpty()) devices.remove(device)
        return Decision(true, replay = replay)
    }

    fun retainDevices(ids: Set<Int>) {
        devices.keys.retainAll(ids)
    }

    fun clear() = devices.clear()
}
