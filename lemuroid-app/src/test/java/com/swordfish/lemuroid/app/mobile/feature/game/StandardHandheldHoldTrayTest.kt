package com.swordfish.lemuroid.app.mobile.feature.game

import android.view.KeyEvent
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.touchinput.radial.settings.NativeControlInventory
import com.swordfish.touchinput.radial.settings.TouchControllerID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StandardHandheldHoldTrayTest {
    @Test fun wonderSwanUsesTheActiveNativeLabelsAndKeepsBothOrientationsDistinct() {
        assertEquals(12, standardHoldTraySystems.distinct().size)
        assertTrue(SystemID.WS in standardHoldTraySystems && SystemID.WSC in standardHoldTraySystems)
        val horizontal = NativeControlInventory.forHoldTray(TouchControllerID.WS_LANDSCAPE)!!
        val vertical = NativeControlInventory.forHoldTray(TouchControllerID.WS_PORTRAIT)!!
        val holdCodes = standardHoldTrayInputs(SystemID.WS).map { it.code }.toSet()
        val horizontalLabels = horizontal.controls.filter { it.keyCode in holdCodes }.map { it.label }.toSet()
        val verticalLabels = vertical.controls.filter { it.keyCode in holdCodes }.map { it.label }.toSet()
        assertEquals(setOf("A", "B", "Start"), horizontalLabels)
        assertEquals(setOf("X1", "X2", "X3", "X4", "Start"), verticalLabels)
        assertEquals(standardHoldTrayInputs(SystemID.WS), standardHoldTrayInputs(SystemID.WSC))
        assertFalse(horizontal.supportsGripArc)
        assertFalse(vertical.supportsGripArc)
    }

    @Test fun dualScreenInventoriesReserveSpecialControlsWithoutMakingThemHoldTargets() {
        val ds = NativeControlInventory.forHoldTray(TouchControllerID.MELONDS)!!
        val threeDs = NativeControlInventory.forHoldTray(TouchControllerID.NINTENDO_3DS)!!
        val special = ds.controls.filter { it.seat in setOf("ds-mic", "ds-layout") }.map { it.keyCode }.toSet()
        assertEquals(setOf(KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_THUMBL), special)
        assertTrue(special.intersect(standardHoldTrayInputs(SystemID.NDS).map { it.code }.toSet()).isEmpty())
        assertTrue(threeDs.controls.any { it.seat == "analog-left" && it.pressCode == KeyEvent.KEYCODE_BUTTON_THUMBL })
        for ((system, inventory) in listOf(SystemID.NDS to ds, SystemID.NINTENDO_3DS to threeDs)) {
            assertTrue(system in standardHoldTraySystems)
            val available = inventory.controls.mapNotNull { it.keyCode }.toSet()
            assertTrue(standardHoldTrayInputs(system).all { it.code in available })
            assertFalse(inventory.supportsGripArc)
        }
    }
}
