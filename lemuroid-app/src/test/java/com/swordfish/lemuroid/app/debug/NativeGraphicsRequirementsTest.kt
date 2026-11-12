package com.swordfish.lemuroid.app.debug

import com.swordfish.lemuroid.lib.core.NativeGraphicsRequirements
import com.swordfish.lemuroid.lib.library.CoreID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeGraphicsRequirementsTest {
    @Test
    fun playNeedsActualGles32WhileOtherEnginesKeepTheirOwnRequirements() {
        assertFalse(NativeGraphicsRequirements.supportsAdvertisedVersion(CoreID.PLAY_MODERN, 0x30000))
        assertFalse(NativeGraphicsRequirements.supportsAdvertisedVersion(CoreID.PLAY_MODERN, 0x30001))
        assertTrue(NativeGraphicsRequirements.supportsAdvertisedVersion(CoreID.PLAY_MODERN, 0x30002))
        assertTrue(NativeGraphicsRequirements.supportsAdvertisedVersion(CoreID.PLAY_MODERN, 0x40000))
        assertTrue(NativeGraphicsRequirements.supportsAdvertisedVersion(CoreID.DOLPHIN_MODERN, 0x30000))
        assertTrue(NativeGraphicsRequirements.supportsAdvertisedVersion(CoreID.PLAY_MODERN, 0))
    }
}
