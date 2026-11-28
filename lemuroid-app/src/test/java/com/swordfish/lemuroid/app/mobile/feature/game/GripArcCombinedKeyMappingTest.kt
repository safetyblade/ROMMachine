package com.swordfish.lemuroid.app.mobile.feature.game

import android.view.KeyEvent
import com.swordfish.touchinput.radial.settings.NativeControlIcon
import com.swordfish.touchinput.radial.settings.NativeControlInventory
import com.swordfish.touchinput.radial.settings.TouchControllerID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GripArcCombinedKeyMappingTest {
    @Test fun dreamcastNativeMappingRetainsTriggerPairAndOneStick() {
        val controls =
            NativeControlInventory
                .forController(
                    TouchControllerID.DREAMCAST,
                )!!
                .controls
                .associateBy { it.key }
        assertEquals(KeyEvent.KEYCODE_BUTTON_B, controls.getValue("A").keyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_A, controls.getValue("B").keyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_Y, controls.getValue("X").keyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_X, controls.getValue("Y").keyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_L2, controls.getValue("L").keyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_R2, controls.getValue("R").keyCode)
        assertFalse("Select" in controls || "L2" in controls || "R2" in controls || "AnalogR" in controls)
        assertEquals(1, controls.getValue("AnalogL").motionSource)
    }

    @Test fun xboxProgrammableCatalogueMatchesNativeWhiteBlackAndTriggers() {
        val xbox = GripArcInputCatalog.forSystem(com.swordfish.lemuroid.lib.library.SystemID.XBOX)
        val native = NativeControlInventory.forController(TouchControllerID.XBOX)!!
        val available = native.inputIds
        assertEquals(10, xbox.size)
        assertTrue(xbox.map { "key:" + it.code }.all { it in available })
        assertTrue(xbox.any { it.label == "White" && it.code == KeyEvent.KEYCODE_BUTTON_L1 })
        assertTrue(xbox.any { it.label == "Black" && it.code == KeyEvent.KEYCODE_BUTTON_R1 })
    }

    /**
     * C02-B dispatch evidence: the two/three-button arcade variants carry
     * exactly the first N numbered seats of the four-button arrangement,
     * with the same per-seat key events, on both arcade systems — so the
     * GripARC guest dispatch (BUTTON_1 + seat number - 1) and the native
     * dispatch agree per seat.
     */
    @Test fun arcadeButtonCountVariantsDispatchTheirOwnSeats() {
        val four =
            checkNotNull(StandaloneGripArcLayout.inventoryFor(TouchControllerID.ARCADE_4))
                .controls
                .associateBy { it.key }
        for ((controller, count) in listOf(TouchControllerID.ARCADE_2 to 2, TouchControllerID.ARCADE_3 to 3)) {
            val inventory = checkNotNull(StandaloneGripArcLayout.inventoryFor(controller))
            val faces = inventory.controls.filter { it.label.toIntOrNull() != null }
            assertEquals(count, faces.size)
            assertEquals((1..count).map { it.toString() }.toSet(), faces.map { it.label }.toSet())
            for (face in faces) {
                // Same seat identity and key event as the four-button cabinet.
                val seatFour = four.values.single { it.label == face.label }
                assertEquals(seatFour.key, face.key)
                assertEquals(seatFour.keyCode, face.keyCode)
            }
            // Utility/dispatch inventory is unchanged: D-pad, Coin, Start, Menu.
            assertEquals(
                setOf("D", "Select", "Start", "Menu"),
                inventory.controls
                    .filter { it.label.toIntOrNull() == null }
                    .map { it.key }
                    .toSet(),
            )
            // Both arcade systems resolve the identical solo profile.
            for (system in listOf(
                com.swordfish.lemuroid.lib.library.SystemID.FBNEO,
                com.swordfish.lemuroid.lib.library.SystemID.MAME2003PLUS,
            )) {
                val profile = checkNotNull(PivotArcSoloProfiles.forMode(system, controller))
                assertEquals(
                    faces.map { it.key }.toSet(),
                    profile.seats
                        .filter { it.holdOf == null && it.label.toIntOrNull() != null }
                        .map { it.id }
                        .toSet(),
                )
            }
        }
    }

    @Test fun selectedControllerVariantDeterminesCompleteInventory() {
        val digital = NativeControlInventory.forController(TouchControllerID.PSX)!!.controls.associateBy { it.key }
        val dual =
            NativeControlInventory
                .forController(
                    TouchControllerID.PSX_DUALSHOCK,
                )!!
                .controls
                .associateBy { it.key }
        assertFalse("AnalogL" in digital || "AnalogR" in digital)
        assertEquals(digital.keys + setOf("AnalogL", "AnalogR"), dual.keys)
        assertEquals(NativeControlIcon.CIRCLE, dual.getValue("A").icon)
        assertEquals(KeyEvent.KEYCODE_BUTTON_L2, dual.getValue("L2").keyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_THUMBR, dual.getValue("AnalogR").pressCode)
        val gc = NativeControlInventory.forController(TouchControllerID.GAMECUBE)!!.controls.associateBy { it.key }
        assertEquals(KeyEvent.KEYCODE_BUTTON_R2, gc.getValue("Z").keyCode)
        assertTrue(gc.getValue("Z").roundButton)
        assertEquals("C-Stick", gc.getValue("AnalogR").label)
        assertFalse("Select" in gc)
        assertNull(NativeControlInventory.forController(TouchControllerID.WII_NUNCHUK))
    }
}
