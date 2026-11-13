package com.swordfish.lemuroid.app.mobile.feature.game

import com.swordfish.touchinput.radial.settings.NativeControlInventory
import com.swordfish.touchinput.radial.settings.TouchControllerID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NativeControllerInventoryConfigTest {
    @Test fun nativeConfigurationPublishesTheSameInventoryUsedByCombinedLayout() {
        for (id in listOf(
            TouchControllerID.GB,
            TouchControllerID.GBA,
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
}
