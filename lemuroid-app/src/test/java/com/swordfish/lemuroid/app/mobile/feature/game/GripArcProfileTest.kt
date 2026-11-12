package com.swordfish.lemuroid.app.mobile.feature.game

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GripArcProfileTest {
    private val a = KeyEvent.KEYCODE_BUTTON_A
    private val b = KeyEvent.KEYCODE_BUTTON_B
    private val l = KeyEvent.KEYCODE_BUTTON_L1
    private val r = KeyEvent.KEYCODE_BUTTON_R1
    private val allowed = setOf(a, b, l, r)

    @Test fun independentCartridgeModesAreExclusive() {
        assertEquals(4, ArcControlMode.values().size)
        assertFalse(ArcControlMode.STANDARD.hasGrip)
        assertTrue(ArcControlMode.PIVOTARC.hasPivot)
        assertTrue(ArcControlMode.STANDARD_GRIPARC.hasGrip)
        assertTrue(ArcControlMode.PIVOTARC_GRIPARC.hasPivot)
        assertTrue(ArcControlMode.PIVOTARC_GRIPARC.hasGrip)
    }

    @Test fun allFourMappingsSurviveCountReductionAndReEnable() {
        val original =
            GripArcProfile(
                mode = ArcControlMode.PIVOTARC_GRIPARC,
                side = GripArcSide.LEFT,
                count = 4,
                mappings = listOf(listOf(l, a), listOf(r, b), listOf(l, r), listOf(a, b)),
            )
        val reduced = original.copy(count = 1)
        val encoded = GripArcProfileStore.encode(reduced)
        val restored = GripArcProfileStore.decode(encoded, GripArcProfile(), allowed)
        assertEquals(1, restored.count)
        assertEquals(original.mappings, restored.mappings)
        assertEquals(GripArcSide.LEFT, restored.side)
        assertEquals(ArcControlMode.PIVOTARC_GRIPARC, restored.mode)
        assertEquals(original.mappings, restored.copy(count = 4).mappings)
    }

    @Test fun unknownOrBrokenProfilesFallbackWithoutCrashing() {
        val fallback = GripArcProfile(mode = ArcControlMode.STANDARD)
        assertEquals(fallback, GripArcProfileStore.decode("not-json", fallback, allowed))
        assertEquals(
            fallback,
            GripArcProfileStore.decode("{\"schema\":999,\"mode\":\"PIVOTARC\"}", fallback, allowed),
        )
    }

    @Test fun unsupportedKeysAndUnsafeOffsetsCannotLeakToRuntime() {
        val profile =
            GripArcProfile(
                count = 7,
                mappings = listOf(listOf(a, 999999, a, b, l, r)),
                dx = Float.NaN,
                dy = 9000f,
            ).validated(allowed)
        assertEquals(4, profile.count)
        assertEquals(listOf(a, b, l, r), profile.mappings[0])
        assertEquals(0f, profile.dx, 0.001f)
        assertEquals(0f, profile.dy, 0.001f)
    }
}
