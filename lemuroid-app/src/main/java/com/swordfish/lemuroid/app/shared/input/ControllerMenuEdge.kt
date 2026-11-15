package com.swordfish.lemuroid.app.shared.input

/** Host-local press ownership. No engine dispatch and no process-global state. */
class ControllerMenuEdge {
    private data class Press(val device: Int, val key: Int)

    private val armed = mutableMapOf<Press, Long>()
    private val completed = mutableMapOf<Press, Long>()

    fun handle(
        deviceId: Int,
        keyCode: Int,
        down: Boolean,
        repeat: Int = 0,
        canceled: Boolean = false,
        downTime: Long = 0,
    ): Boolean {
        val press = Press(deviceId, keyCode)
        if (canceled) {
            armed.remove(press)
            completed[press] = downTime
            return false
        }
        if (repeat != 0) return false
        if (down) {
            if (completed[press] != downTime) armed.putIfAbsent(press, downTime)
            return false
        }
        val started = armed.remove(press) ?: return false
        completed[press] = started
        return started == downTime
    }

    fun retainDevices(ids: Set<Int>) {
        armed.keys.removeAll { it.device !in ids }
        completed.keys.removeAll { it.device !in ids }
    }

    fun clear() {
        armed.clear()
        completed.clear()
    }
}
