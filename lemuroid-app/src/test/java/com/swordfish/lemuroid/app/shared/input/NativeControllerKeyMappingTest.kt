package com.swordfish.lemuroid.app.shared.input

import org.junit.Assert.assertEquals
import org.junit.Test

class NativeControllerKeyMappingTest {
    @Test fun existingDefaultRetroPadPositionsPreserveNativeButtons() {
        val defaults = mapOf(96 to 97, 97 to 96, 99 to 100, 100 to 99)
        listOf(96, 97, 99, 100, 108, 109, 102, 103, 104, 105).forEach { code ->
            assertEquals(code, NativeControllerKeyMapping.output(code, defaults))
        }
    }

    @Test fun customBindingsTranslateOnlyVerifiedKeysAndKeepUnknownSources() {
        assertEquals(96, NativeControllerKeyMapping.output(104, mapOf(104 to 97)))
        assertEquals(110, NativeControllerKeyMapping.output(108, mapOf(108 to 110)))
        assertEquals(19, NativeControllerKeyMapping.output(19, mapOf(19 to 0)))
        assertEquals(106, NativeControllerKeyMapping.output(106, emptyMap()))
    }

    @Test fun releaseRetainsItsAcceptedOutputAcrossPreferenceChangesAndDeviceIds() {
        val held = NativeControllerKeyMapping.HeldOutputs()
        assertEquals(96, held.key(1, 104, true, 97))
        assertEquals(96, held.key(1, 104, true, 96))
        assertEquals(96, held.key(1, 104, false, 96))
        assertEquals(97, held.key(2, 104, true, 96))
        held.retainDevices(setOf(1))
        assertEquals(96, held.key(2, 104, false, 97))
        held.clear()
        assertEquals(104, held.key(1, 104, false, null))
        assertEquals(96, NativeControllerKeyMapping.output(96, 999))
    }
}
