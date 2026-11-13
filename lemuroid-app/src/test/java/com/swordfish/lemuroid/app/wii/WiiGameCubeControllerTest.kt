package com.swordfish.lemuroid.app.wii

import com.swordfish.lemuroid.lib.library.ControllerConfigs
import com.swordfish.lemuroid.lib.library.NativeGameSystems
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.touchinput.radial.settings.TouchControllerID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WiiGameCubeControllerTest {
    @Test fun wiiOffersFourRealControllerProfilesWithSidewaysDefault() {
        val wii = NativeGameSystems.configurations().single { it.id == SystemID.WII }
        val controllers = wii.systemCoreConfigs.single().controllerConfigs.getValue(0)
        assertEquals(
            listOf("wii_sideways", "wii_classic", "wii_nunchuk", "wii_gamecube"),
            controllers.map { it.name },
        )
        assertEquals(513, controllers.first().libretroId)
    }

    @Test fun gameCubeWiiChoiceUsesDolphinRetroPadWithNativeGameCubeTouchArt() {
        val gcOnWii = ControllerConfigs.WII_GAMECUBE
        assertEquals("wii_gamecube", gcOnWii.name)
        assertEquals(1, gcOnWii.libretroId)
        assertEquals(TouchControllerID.GAMECUBE, gcOnWii.touchControllerID)
        assertEquals(ControllerConfigs.GAMECUBE.touchControllerID, gcOnWii.touchControllerID)
        assertTrue(gcOnWii.getTouchControllerConfig().nativeInventory != null)
    }

    @Test fun wiiGameCubeChoiceDoesNotReplaceStandaloneGameCubeOrOtherWiiProfiles() {
        assertEquals("default", ControllerConfigs.GAMECUBE.name)
        assertEquals(1, ControllerConfigs.GAMECUBE.libretroId)
        assertEquals(1025, ControllerConfigs.WII_CLASSIC.libretroId)
        assertEquals(769, ControllerConfigs.WII_NUNCHUK.libretroId)
        assertEquals(513, ControllerConfigs.WII_SIDEWAYS.libretroId)
    }
}
