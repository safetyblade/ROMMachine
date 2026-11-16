package com.swordfish.lemuroid.app.shared.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ControllerPortRegistryTest {
    private fun device(
        id: Int,
        descriptor: String,
    ) = ControllerPortRegistry.Device(id, descriptor)

    @Test fun reconnectWithNewIdRetainsPortWithoutMovingOtherPlayers() {
        val ports = ControllerPortRegistry(7)
        assertEquals(mapOf(1 to 0, 2 to 1), ports.update(listOf(device(1, "a"), device(2, "b"))))
        assertEquals(mapOf(2 to 1), ports.update(listOf(device(2, "b"))))
        assertEquals(mapOf(9 to 0, 2 to 1), ports.update(listOf(device(2, "b"), device(9, "a"))))
    }

    @Test fun identicalNamesAndDescriptorsNeverCollapseLivePads() {
        val ports = ControllerPortRegistry(7)
        val devices = (1..7).map { device(it, "identical descriptor") }
        assertEquals((0..6).toSet(), ports.update(devices).values.toSet())
        val retained = ports.update(devices.drop(1))
        assertEquals(1, retained[2])
        val next = ports.update(devices.drop(1) + device(21, "identical descriptor"))
        assertEquals(0, next[21])
        assertFalse(
            ports.update(devices.drop(1) + device(21, "identical descriptor") + device(22, "extra")).containsKey(22),
        )
    }

    @Test fun explicitPortReassignmentSwapsOwnersAndSurvivesReconnect() {
        val ports = ControllerPortRegistry(7)
        val first = device(1, "one")
        val second = device(2, "two")
        ports.update(listOf(first, second))
        org.junit.Assert.assertTrue(ports.assign(1, 1))
        assertEquals(mapOf(1 to 1, 2 to 0), ports.update(listOf(first, second)))
        ports.update(listOf(second))
        assertEquals(mapOf(9 to 1, 2 to 0), ports.update(listOf(device(9, "one"), second)))
        assertFalse(ports.assign(9, 7))
    }

    @Test fun reusedIdAndSessionTeardownDoNotRetainForeignIdentity() {
        val ports = ControllerPortRegistry(7)
        ports.update(listOf(device(1, "a"), device(2, "b")))
        assertEquals(2, ports.update(listOf(device(1, "c"), device(2, "b")))[1])
        ports.clear()
        assertEquals(mapOf(2 to 0), ports.update(listOf(device(2, "b"))))
    }
}
