package com.swordfish.lemuroid.app.shared.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControllerHardwareProfileTest {
    @Test fun remoteKeyboardAndVirtualDevicesNeverQualify() {
        assertFalse(ControllerHardwareProfile.qualifies(false, false, false, 0, false))
        assertFalse(ControllerHardwareProfile.qualifies(false, true, false, 1, false))
        assertFalse(ControllerHardwareProfile.qualifies(true, true, true, 4, true))
        assertFalse(ControllerHardwareProfile.qualifies(false, false, true, 4, false))
        assertTrue(ControllerHardwareProfile.qualifies(false, true, false, 4, false))
        assertTrue(ControllerHardwareProfile.qualifies(false, false, true, 4, true))
    }

    @Test fun knownAndGenericNamesRetainPhysicalFamilyWithoutInvertingButtons() {
        assertEquals(
            ControllerHardwareProfile.Family.XBOX,
            ControllerHardwareProfile.family("Xbox Wireless Controller"),
        )
        assertEquals(ControllerHardwareProfile.Family.PLAYSTATION, ControllerHardwareProfile.family("DualSense"))
        assertEquals(
            ControllerHardwareProfile.Family.SWITCH,
            ControllerHardwareProfile.family("Nintendo Switch Pro Controller"),
        )
        assertEquals(ControllerHardwareProfile.Family.GENERIC, ControllerHardwareProfile.family("USB HID Controller"))
    }
}
