package com.swordfish.lemuroid.app.mobile.feature.game

import android.view.KeyEvent
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.touchinput.radial.settings.TouchControllerID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PivotArcSoloProfilesTest {
    @Test
    fun nativeModeControlsAreExplicitAndHoldsUseActualKeys() {
        val digital = checkNotNull(PivotArcSoloProfiles.forMode(SystemID.PSX, TouchControllerID.PSX))
        val dual = checkNotNull(PivotArcSoloProfiles.forMode(SystemID.PSX, TouchControllerID.PSX_DUALSHOCK))
        assertNull(digital.seat("AnalogL"))
        assertNotNull(dual.seat("AnalogR"))
        assertEquals(KeyEvent.KEYCODE_BUTTON_B, digital.seat("B")?.keyCode)
        assertEquals("Cross", digital.seat("B")?.label)
        assertEquals(4, dual.holdKeys.size)
        val dc = checkNotNull(PivotArcSoloProfiles.forMode(SystemID.DREAMCAST, TouchControllerID.DREAMCAST))
        assertEquals(KeyEvent.KEYCODE_BUTTON_B, dc.seat("A")?.keyCode)
        assertTrue(KeyEvent.KEYCODE_BUTTON_L2 in dc.holdKeys)
        assertTrue(KeyEvent.KEYCODE_BUTTON_R2 in dc.holdKeys)
        val n64 = checkNotNull(PivotArcSoloProfiles.forMode(SystemID.N64, TouchControllerID.N64))
        assertEquals(KeyEvent.KEYCODE_BUTTON_L2, n64.seat("Z")?.keyCode)
        assertFalse(checkNotNull(n64.seat("CDirection")).diagonals)
        assertNull(PivotArcSoloProfiles.forMode(SystemID.PSX, TouchControllerID.GBA))
        assertNull(PivotArcSoloProfiles.forMode(SystemID.WII_U, TouchControllerID.PSX_DUALSHOCK))
    }

    @Test
    fun fitsKeepEverySeatOrExplainFallback() {
        val profile = checkNotNull(PivotArcSoloProfiles.forMode(SystemID.PSX, TouchControllerID.PSX))
        val safe = PivotArcBounds(8f, 8f, 620f, 1100f)
        val video = PivotArcBounds(22f, 84f, 606f, 340f)
        val right = PivotArcSoloSolver.resolve(profile, safe, video, false, 0f, 0f, false)
        assertTrue(right is PivotArcResolution.Fits)
        val left = PivotArcSoloSolver.resolve(profile, safe, video, true, 0f, 0f, false)
        assertTrue(left is PivotArcResolution.Fits)
        val r = (right as PivotArcResolution.Fits).placement.controls
        val l = (left as PivotArcResolution.Fits).placement.controls
        assertEquals(profile.seats.map { it.id }.toSet(), r.keys)
        assertEquals(r.keys, l.keys)
        for ((key, bounds) in r) {
            assertTrue(bounds.width >= 48f && bounds.height >= 48f)
            assertFalse(bounds.intersects(video))
            assertTrue(bounds.inside(safe))
            assertEquals(safe.left + safe.right - bounds.right, l.getValue(key).left, 0.01f)
            assertTrue(r.filterKeys { it != key }.values.none(bounds::intersects))
        }
        val tooSmall = PivotArcBounds(8f, 8f, 300f, 390f)
        assertTrue(
            PivotArcSoloSolver.resolve(profile, tooSmall, video, false, 0f, 0f, false) is PivotArcResolution.Unavailable,
        )
    }
}
