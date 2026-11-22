package com.swordfish.lemuroid.app.mobile.feature.game

import android.view.KeyEvent
import com.swordfish.touchinput.radial.settings.NativeControlInventory
import com.swordfish.touchinput.radial.settings.NativeControlKind
import com.swordfish.touchinput.radial.settings.TouchControllerID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class NativeControllerInventoryConfigTest {
    @Test fun nativeConfigurationPublishesTheSameInventoryUsedByCombinedLayout() {
        for (id in listOf(
            TouchControllerID.GB,
            TouchControllerID.GBA,
            TouchControllerID.GG,
            TouchControllerID.LYNX,
            TouchControllerID.NGP,
            TouchControllerID.SNES,
            TouchControllerID.PSX,
            TouchControllerID.PSX_DUALSHOCK,
            TouchControllerID.PSP,
            TouchControllerID.GAMECUBE,
            TouchControllerID.DREAMCAST,
        )) {
            val selected = TouchControllerID.getConfig(id).nativeInventory
            assertNotNull(id.name, selected)
            assertEquals(NativeControlInventory.forController(id), selected)
        }
    }

    @Test fun compactHandheldNativeInventoriesMatchRealPadKitControlsWithoutPhantoms() {
        val gg = NativeControlInventory.forController(TouchControllerID.GG)!!
        assertEquals(setOf("D", "A", "B", "Start", "Menu"), gg.controls.map { it.key }.toSet())
        assertEquals("2", gg.controls.single { it.key == "A" }.label)
        assertEquals("1", gg.controls.single { it.key == "B" }.label)
        assertFalse(gg.controls.any { it.kind == NativeControlKind.SHOULDER })

        val ngp = NativeControlInventory.forController(TouchControllerID.NGP)!!
        assertEquals(setOf("D", "A", "B", "Start", "Menu"), ngp.controls.map { it.key }.toSet())
        assertEquals("B", ngp.controls.single { it.key == "A" }.label)
        assertEquals("A", ngp.controls.single { it.key == "B" }.label)

        val lynx = NativeControlInventory.forController(TouchControllerID.LYNX)!!
        assertEquals(setOf("D", "A", "B", "L", "R", "Start", "Menu"), lynx.controls.map { it.key }.toSet())
        assertEquals("Option 1", lynx.controls.single { it.key == "L" }.label)
        assertEquals("Option 2", lynx.controls.single { it.key == "R" }.label)
        assertEquals("Pause", lynx.controls.single { it.key == "Start" }.label)
        assertEquals(KeyEvent.KEYCODE_BUTTON_L1, lynx.controls.single { it.key == "L" }.keyCode)
        assertEquals(KeyEvent.KEYCODE_BUTTON_R1, lynx.controls.single { it.key == "R" }.keyCode)

        for (native in listOf(gg, ngp, lynx)) {
            assertFalse(native.controls.any { it.key == "Select" })
            assertEquals(native.inputIds, NativeControlInventory.forController(native.controller)!!.inputIds)
            assertFalse(native.matchesNativeInputs(native.inputIds + "key:999"))
        }
        // Incomplete dual-direction and motion variants are still deliberately unsupported.
        assertEquals(null, NativeControlInventory.forController(TouchControllerID.WS_PORTRAIT))
        assertEquals(null, NativeControlInventory.forController(TouchControllerID.NINTENDO_3DS))
    }
}
