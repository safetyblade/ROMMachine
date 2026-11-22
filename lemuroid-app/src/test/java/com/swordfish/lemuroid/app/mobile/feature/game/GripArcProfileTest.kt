package com.swordfish.lemuroid.app.mobile.feature.game

import android.view.KeyEvent
import com.swordfish.lemuroid.lib.library.SystemID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
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

    @Test fun newProfilesExposeFourButtonsAndBothXboxGenerationsHaveInputs() {
        assertEquals(4, GripArcProfile().count)
        for (system in listOf(SystemID.XBOX, SystemID.XBOX360)) {
            val inputs = GripArcInputCatalog.forSystem(system)
            assertTrue(inputs.isNotEmpty())
            assertTrue(inputs.any { it.code == KeyEvent.KEYCODE_BUTTON_A })
            assertTrue(inputs.any { it.code == KeyEvent.KEYCODE_BUTTON_L1 })
        }
    }

    @Test fun newlyMappedHandheldCataloguesUseTheActualPadKitKeysAndLabels() {
        val gg = GripArcInputCatalog.forSystem(SystemID.GG)
        assertEquals(listOf("2", "1", "Start"), gg.map { it.label })
        assertEquals(listOf(a, b, KeyEvent.KEYCODE_BUTTON_START), gg.map { it.code })

        val lynx = GripArcInputCatalog.forSystem(SystemID.LYNX)
        assertEquals(listOf("A", "B", "Option 1", "Option 2", "Pause"), lynx.map { it.label })
        assertEquals(
            listOf(a, b, l, r, KeyEvent.KEYCODE_BUTTON_START),
            lynx.map { it.code },
        )

        val ngp = GripArcInputCatalog.forSystem(SystemID.NGP)
        assertEquals(listOf("B", "A", "Start"), ngp.map { it.label })
        assertEquals(listOf(a, b, KeyEvent.KEYCODE_BUTTON_START), ngp.map { it.code })
        // NGC is Neo Geo Pocket Color, not the GAMECUBE console.
        assertEquals(ngp, GripArcInputCatalog.forSystem(SystemID.NGC))

        val threeDs = GripArcInputCatalog.forSystem(SystemID.NINTENDO_3DS)
        assertEquals(
            listOf(a, b, KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y, l, r,
                KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_SELECT),
            threeDs.map { it.code },
        )
        // Listing a control for mapping does not assert that the native 3DS
        // composite layout or guest sensor bridge is implemented.
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
