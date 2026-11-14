package com.swordfish.lemuroid.app.shared.game.xbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XboxHardwareKeyRouterTest {
    @Test
    fun duplicateNativeOutputHasOneDownAndOneFinalUp() {
        val router = XboxHardwareKeyRouter()
        assertEquals(10 to true, router.route(1, 100, true, 10))
        assertNull(router.route(1, 100, true, 10))
        assertNull(router.route(1, 101, true, 10))
        assertNull(router.route(1, 100, false, 10))
        assertEquals(10 to false, router.route(1, 101, false, 10))
        assertNull(router.route(1, 101, false, 10))
    }

    @Test
    fun independentDevicesDoNotReleaseAnotherDeviceHold() {
        val router = XboxHardwareKeyRouter()
        assertEquals(25 to true, router.route(7, 90, true, 25))
        assertNull(router.route(8, 90, true, 25))
        assertTrue(router.removeDisconnected(setOf(8)).isEmpty())
        assertEquals(listOf(25 to false), router.removeDisconnected(emptySet()))
    }

    @Test
    fun releaseUsesOriginalDownBindingEvenIfProfileChanges() {
        val router = XboxHardwareKeyRouter()
        assertEquals(12 to true, router.route(1, 100, true, 12))
        assertEquals(12 to false, router.route(1, 100, false, 40))
    }
}
