package com.swordfish.lemuroid.app.shared.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WiiControllerCompatibilityTest {
    @Test fun savedExactControllerWinsAndUnknownFallsBackToCoreDefault() {
        val available = listOf("wii_remote", "classic", "sideways", "wiimote_nunchuk")
        assertEquals("classic", WiiControllerCompatibility.selectVerifiedOrFallback(available, "classic"))
        assertEquals("wii_remote", WiiControllerCompatibility.selectVerifiedOrFallback(available, "not_present"))
        assertNull(WiiControllerCompatibility.selectVerifiedOrFallback(emptyList(), "classic"))
    }

    @Test fun variantsNeverInferMotionFromGenericGamepads() {
        assertEquals(
            WiiControllerCompatibility.Variant.CLASSIC,
            WiiControllerCompatibility.variant("Classic Controller"),
        )
        assertEquals(
            WiiControllerCompatibility.Variant.SIDEWAYS_REMOTE,
            WiiControllerCompatibility.variant("Sideways Wii Remote"),
        )
        assertEquals(
            WiiControllerCompatibility.Variant.REMOTE_WITH_NUNCHUK,
            WiiControllerCompatibility.variant("Wiimote + Nunchuk"),
        )
        assertEquals(
            WiiControllerCompatibility.Variant.OTHER,
            WiiControllerCompatibility.variant("Wiimote"),
        )
    }
}
