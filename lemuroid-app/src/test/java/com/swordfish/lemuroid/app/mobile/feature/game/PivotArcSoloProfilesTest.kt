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
    fun arcadeAtariAndDolphinPreserveModeSpecificInputs() {
        for (system in listOf(SystemID.FBNEO, SystemID.MAME2003PLUS)) {
            val four = checkNotNull(PivotArcSoloProfiles.forMode(system, TouchControllerID.ARCADE_4))
            val six = checkNotNull(PivotArcSoloProfiles.forMode(system, TouchControllerID.ARCADE_6))
            assertEquals(4, four.holdKeys.size)
            assertEquals(setOf(KeyEvent.KEYCODE_BUTTON_B), six.holdKeys)
            assertEquals("Coin", six.seat("Select")?.label)
            assertEquals(6, six.seats.count { !it.utility && it.keyCode != null })
        }
        val atari = checkNotNull(PivotArcSoloProfiles.forMode(SystemID.ATARI2600, TouchControllerID.ATARI2600))
        assertEquals(setOf(KeyEvent.KEYCODE_BUTTON_B), atari.holdKeys)
        assertEquals(KeyEvent.KEYCODE_BUTTON_L1, atari.seat("DifficultyA")?.keyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_L2, atari.seat("DifficultyB")?.keyCode)
        val gc = checkNotNull(PivotArcSoloProfiles.forMode(SystemID.GAMECUBE, TouchControllerID.GAMECUBE))
        assertEquals(KeyEvent.KEYCODE_BUTTON_R1, gc.seat("Z")?.keyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_L2, gc.seat("L")?.keyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_R2, gc.seat("R")?.keyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_THUMBL, gc.seat("HalfL")?.keyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_THUMBR, gc.seat("HalfR")?.keyCode)
        assertEquals(gc, PivotArcSoloProfiles.forMode(SystemID.WII, TouchControllerID.GAMECUBE))
        val classic = checkNotNull(PivotArcSoloProfiles.forMode(SystemID.WII, TouchControllerID.WII_CLASSIC))
        assertEquals("ZL", classic.seat("L")?.label)
        assertEquals(KeyEvent.KEYCODE_BUTTON_A, classic.seat("X")?.keyCode)
        assertNull(classic.seat("LS"))
    }

    @Test
    fun xboxNativeUsesSharedSonySeatsWithNativeLegendsAndKeys() {
        val xbox = checkNotNull(PivotArcSoloProfiles.forNativeXbox(TouchControllerID.XBOX))
        val x360 = checkNotNull(PivotArcSoloProfiles.forNativeXbox(TouchControllerID.XBOX360))
        assertEquals("White", xbox.seat("L")?.label)
        assertEquals("Black", xbox.seat("R")?.label)
        assertEquals("LB", x360.seat("L")?.label)
        assertEquals("RB", x360.seat("R")?.label)
        assertEquals(KeyEvent.KEYCODE_BUTTON_A, x360.seat("B")?.keyCode)
        assertEquals(xbox.seats.map { it.x to it.y }, x360.seats.map { it.x to it.y })
        assertEquals(xbox.holdKeys, x360.holdKeys)
        assertEquals(4, x360.holdKeys.size)
        assertNull(PivotArcSoloProfiles.forNativeXbox(TouchControllerID.PSX))
        val nunchuk = checkNotNull(PivotArcSoloProfiles.forMode(SystemID.WII, TouchControllerID.WII_NUNCHUK))
        assertEquals("IR pointer", nunchuk.seat("AnalogR")?.label)
        assertEquals(KeyEvent.KEYCODE_BUTTON_L2, nunchuk.seat("ShakeN")?.keyCode)
        assertEquals(3, nunchuk.holdKeys.size)
    }

    @Test
    fun approvedSonyAndXboxFaceArcAndHoldsStayTogetherAtTheBottom() {
        val profiles =
            listOfNotNull(
                PivotArcSoloProfiles.forNativeXbox(TouchControllerID.XBOX),
                PivotArcSoloProfiles.forNativeXbox(TouchControllerID.XBOX360),
                PivotArcSoloProfiles.forMode(SystemID.PLAYSTATION_2, TouchControllerID.PSX_DUALSHOCK),
            )
        assertEquals(3, profiles.size)
        val device = ControlDeckDeviceFixture(360f, 780f)
        for (profile in profiles) {
            val result = PivotArcSoloSolver.resolve(profile, device.safe, device.viewport, false, 0f, 0f, false)
            val bounds = (result as PivotArcResolution.Fits).placement.controls
            val dpad = bounds.getValue("D")
            val faceArc = listOf("Y", "X", "B", "A").map(bounds::getValue)
            assertTrue(faceArc.zipWithNext().all { (upper, lower) -> upper.centerY < lower.centerY })
            assertTrue(faceArc.all { it.centerX > dpad.centerX })
            assertTrue(faceArc[1].centerY < dpad.centerY && dpad.centerY < faceArc[2].centerY)
            assertEquals(device.safe.bottom, bounds.values.maxOf { it.bottom }, 0.01f)
            for (key in listOf("Y", "X", "B", "A")) {
                val face = bounds.getValue(key)
                val hold = bounds.getValue("Hold$key")
                assertTrue(kotlin.math.hypot(face.centerX - hold.centerX, face.centerY - hold.centerY) < 72f)
            }
        }
    }

    @Test
    fun wiiNunchukLayoutsShareVerifiedNativeKeysAndAdaptivePointer() {
        val pivot = PivotArcSoloProfiles.wiiNunchuk(pointerStick = true)
        val standard = PivotArcSoloProfiles.wiiNunchuk(pointerStick = true, fixedHolds = false)
        val touch = PivotArcSoloProfiles.wiiNunchuk(pointerStick = false)
        assertEquals(pivot.seats.filter { it.holdOf == null }, standard.seats)
        assertEquals(
            setOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_Y),
            pivot.holdKeys,
        )
        assertTrue(standard.holdKeys.isEmpty())
        assertEquals("1", pivot.seat("Start")?.label)
        assertEquals("2", pivot.seat("Select")?.label)
        assertEquals(KeyEvent.KEYCODE_BUTTON_THUMBR, pivot.seat("Home")?.keyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_MODE, pivot.seat("Menu")?.keyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_R2, pivot.seat("Shake")?.keyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_L2, pivot.seat("ShakeN")?.keyCode)
        assertNull(touch.seat("AnalogR"))
        val device = ControlDeckDeviceFixture(360f, 740f)
        val withStick = PivotArcSoloSolver.resolve(pivot, device.safe, device.viewport, false, 0f, 0f, false)
        val withTouch = PivotArcSoloSolver.resolve(touch, device.safe, device.viewport, false, 0f, 0f, false)
        assertEquals(
            (withStick as PivotArcResolution.Fits).placement.controls - "AnalogR",
            (withTouch as PivotArcResolution.Fits).placement.controls,
        )
    }

    @Test
    fun remainingHostAndHandheldModesKeepNativeIdentities() {
        val psp = checkNotNull(PivotArcSoloProfiles.forMode(SystemID.PSP, TouchControllerID.PSP))
        val analog = checkNotNull(PivotArcSoloProfiles.forMode(SystemID.PSP, TouchControllerID.PSP, true))
        assertEquals(4, psp.holdKeys.size)
        assertEquals(psp.seat("D")?.x, analog.seat("AnalogL")?.x)
        assertEquals(psp.seat("L")?.y, psp.seat("R")?.y)
        val ws = checkNotNull(PivotArcSoloProfiles.forMode(SystemID.WS, TouchControllerID.WS_PORTRAIT))
        assertEquals(KeyEvent.KEYCODE_BUTTON_L1, ws.seat("A")?.keyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_R1, ws.seat("B")?.keyCode)
        assertTrue(checkNotNull(ws.seat("Cursor")).y < checkNotNull(ws.seat("B")).y)
        val ps3 = PivotArcSoloProfiles.forNativeHost(SystemID.PLAYSTATION_3, TouchControllerID.PSX_DUALSHOCK)
        assertNotNull(ps3?.seat("AnalogR"))
        val wiiu = PivotArcSoloProfiles.forNativeHost(SystemID.WII_U, TouchControllerID.WII_CLASSIC)
        assertEquals("L", wiiu?.seat("L")?.label)
        assertEquals(KeyEvent.KEYCODE_BUTTON_THUMBL, wiiu?.seat("LS")?.keyCode)
        assertNull(PivotArcSoloProfiles.forNativeHost(SystemID.XBOX, TouchControllerID.XBOX360))
        assertEquals(KeyEvent.KEYCODE_BUTTON_L2, wiiu?.seat("L")?.keyCode)
        assertEquals("B", pivotArcLegacyKey(SystemID.SMS, "A", false))
        assertEquals("1", pivotArcLegacyLabel(SystemID.SMS, "A", false))
        assertEquals("I", pivotArcLegacyLabel(SystemID.PC_ENGINE, "A", false))
        assertEquals("A", pivotArcLegacyKey(SystemID.GBA, "A", false))
        assertEquals("A", pivotArcLegacyKey(SystemID.SMS, "A", true))
    }

    @Test
    fun allThirtyThreeSystemsHaveADeclaredSoloSourcePath() {
        val systems = SystemID.entries
        assertEquals(33, systems.size)
        assertTrue(
            systems.all {
                PivotArcLayoutProfile.activeFor(it) != null || PivotArcSoloProfiles.supportsSystem(it)
            },
        )
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
            PivotArcSoloSolver.resolve(
                profile,
                tooSmall,
                video,
                false,
                0f,
                0f,
                false,
            ) is PivotArcResolution.Unavailable,
        )
    }
}
