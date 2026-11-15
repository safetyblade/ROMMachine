package com.swordfish.lemuroid.app.shared.input

/** Session-owned slots; Android IDs route events, descriptors retain reconnect identity. */
class ControllerPortRegistry(private val capacity: Int) {
    data class Device(val id: Int, val descriptor: String)

    private data class Slot(val descriptor: String, var deviceId: Int?)

    private val slots = mutableMapOf<Int, Slot>()

    fun update(devices: List<Device>): Map<Int, Int> {
        val ids = devices.map { it.id }.toSet()
        slots.values.filter { it.deviceId !in ids }.forEach { it.deviceId = null }
        // Retain only an exact identity match, even when Android reuses an ID.
        slots.values.forEach { slot ->
            if (devices.none { it.id == slot.deviceId && it.descriptor == slot.descriptor }) slot.deviceId = null
        }
        devices.forEach { device ->
            if (slots.values.any { it.deviceId == device.id }) return@forEach
            val port =
                slots.entries.firstOrNull { it.value.deviceId == null && it.value.descriptor == device.descriptor }?.key
                    ?: (0 until capacity).firstOrNull { it !in slots }
                    ?: slots.entries.firstOrNull { it.value.deviceId == null }?.key
                    ?: return@forEach
            slots[port] = Slot(device.descriptor, device.id)
        }
        return slots.mapNotNull { (port, slot) -> slot.deviceId?.let { it to port } }.toMap()
    }

    fun clear() = slots.clear()
}
