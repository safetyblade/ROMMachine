package com.swordfish.lemuroid.app.mobile.feature.game

import android.view.KeyEvent
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckEditionPreset
import com.swordfish.lemuroid.lib.BuildConfig
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.touchinput.radial.settings.TouchControllerID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Console layout activation contract: each registered, playable libretro
 * system must be able to resolve its Standard PadKit controls. This does not
 * imply an unbundled emulator core or physical gameplay has been verified.
 */
class CartdeckConsoleLayoutCoverageTest {
    @Test
    fun everyRegisteredConsoleHasAConfiguredStandardController() {
        val systems = GameSystem.all()
        val nativeSystems =
            setOf(SystemID.DREAMCAST, SystemID.GAMECUBE, SystemID.PLAYSTATION_2, SystemID.WII, SystemID.XBOX)
        val enabledNativeNames = BuildConfig.NATIVE_SYSTEMS.split(",").toSet()
        val expected = SystemID.entries.filter { it !in nativeSystems || it.dbname in enabledNativeNames }.toSet()
        assertEquals(
            "Every legacy and enabled native system must retain its Standard profile",
            expected,
            systems.map { it.id }.toSet(),
        )
        for (system in systems) {
            if (system.id == SystemID.XBOX) {
                assertEquals("hakux", system.nativeBackend)
                assertTrue(
                    "Xbox must use its native host instead of a fake Libretro core",
                    system.systemCoreConfigs.isEmpty(),
                )
                assertNotNull(TouchControllerID.getConfig(TouchControllerID.XBOX))
                continue
            }
            assertTrue(system.id.name, system.systemCoreConfigs.isNotEmpty())
            for (core in system.systemCoreConfigs) {
                assertTrue(
                    "${system.id}/${core.coreID} must register a playable first-pad layout",
                    core.controllerConfigs[0]?.isNotEmpty() == true,
                )
                core.controllerConfigs.forEach { (port, controllers) ->
                    assertTrue("${system.id} port $port", controllers.isNotEmpty())
                    for (controller in controllers) {
                        assertNotNull(
                            "${system.id} / ${controller.touchControllerID}",
                            TouchControllerID.getConfig(controller.touchControllerID),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun gameCubeHasItsOwnTouchControllerWithoutChangingPlayStation2() {
        val gameCube =
            GameSystem.all().firstOrNull { it.id == SystemID.GAMECUBE }
                ?: com.swordfish.lemuroid.lib.library.NativeGameSystems.configurations()
                    .first { it.id == SystemID.GAMECUBE }
        val gameCubePad = gameCube.systemCoreConfigs.first().controllerConfigs.getValue(0).first()
        assertEquals(TouchControllerID.GAMECUBE, gameCubePad.touchControllerID)
        assertEquals("default", gameCubePad.name)
        assertEquals(1, gameCubePad.libretroId)
        assertNotNull(TouchControllerID.getConfig(TouchControllerID.GAMECUBE))
        val ps2 =
            com.swordfish.lemuroid.lib.library.NativeGameSystems.configurations()
                .first { it.id == SystemID.PLAYSTATION_2 }
        assertEquals(
            TouchControllerID.PSX_DUALSHOCK,
            ps2.systemCoreConfigs.first().controllerConfigs.getValue(0).first().touchControllerID,
        )
        val gcInputs = GripArcInputCatalog.forSystem(SystemID.GAMECUBE)
        assertTrue(gcInputs.any { it.label == "Z" && it.code == KeyEvent.KEYCODE_BUTTON_R2 })
        assertTrue(gcInputs.none { it.label == "Square" || it.label == "Triangle" })
        assertTrue(GripArcInputCatalog.forSystem(SystemID.NGC).none { it.label == "Z" })
    }

    @Test
    fun dreamcastUsesItsOwnFlycastPadAndPhysicalButtonMappings() {
        val dc =
            com.swordfish.lemuroid.lib.library.NativeGameSystems.configurations()
                .first { it.id == SystemID.DREAMCAST }
        val controller = dc.systemCoreConfigs.first().controllerConfigs.getValue(0).first()
        assertEquals(TouchControllerID.DREAMCAST, controller.touchControllerID)
        assertEquals("default", controller.name)
        assertEquals(1, controller.libretroId)
        assertNotNull(TouchControllerID.getConfig(TouchControllerID.DREAMCAST))
        val inputs = GripArcInputCatalog.forSystem(SystemID.DREAMCAST)
        val mapped = inputs.associate { it.label to it.code }
        assertEquals(KeyEvent.KEYCODE_BUTTON_B, mapped.getValue("A"))
        assertEquals(KeyEvent.KEYCODE_BUTTON_A, mapped.getValue("B"))
        assertEquals(KeyEvent.KEYCODE_BUTTON_Y, mapped.getValue("X"))
        assertEquals(KeyEvent.KEYCODE_BUTTON_X, mapped.getValue("Y"))
        assertEquals(KeyEvent.KEYCODE_BUTTON_L2, mapped.getValue("L"))
        assertEquals(KeyEvent.KEYCODE_BUTTON_R2, mapped.getValue("R"))
        assertTrue("Dreamcast has no physical Select", "Select" !in mapped)
        assertTrue("No PlayStation symbols", inputs.none { it.label == "Circle" || it.label == "Triangle" })

        // Regressions: PS2 and GameCube retain the controller configs they
        // already used before this Dreamcast-only change.
        val native = com.swordfish.lemuroid.lib.library.NativeGameSystems.configurations()
        val ps2 = native.first { it.id == SystemID.PLAYSTATION_2 }
        val gc = native.first { it.id == SystemID.GAMECUBE }
        assertEquals(
            TouchControllerID.PSX_DUALSHOCK,
            ps2.systemCoreConfigs.first().controllerConfigs.getValue(0).first().touchControllerID,
        )
        assertEquals(
            TouchControllerID.GAMECUBE,
            gc.systemCoreConfigs.first().controllerConfigs.getValue(0).first().touchControllerID,
        )
    }

    @Test
    fun dreamcastControllerClearanceIsPortraitAndStandaloneOnly() {
        assertEquals(24.dp, GameScreenLayout.portraitBottomInsetFor(SystemID.DREAMCAST, true, false, 780.dp))
        assertEquals(12.dp, GameScreenLayout.portraitBottomInsetFor(SystemID.DREAMCAST, true, false, 600.dp))
        assertEquals(0.dp, GameScreenLayout.portraitBottomInsetFor(SystemID.DREAMCAST, true, true, 780.dp))
        assertEquals(0.dp, GameScreenLayout.portraitBottomInsetFor(SystemID.DREAMCAST, false, false, 780.dp))
        assertEquals(0.dp, GameScreenLayout.portraitBottomInsetFor(SystemID.PLAYSTATION_2, true, false, 780.dp))
        assertEquals(0.dp, GameScreenLayout.portraitBottomInsetFor(SystemID.GBA, true, false, 780.dp))
    }

    @Test
    fun everyConsoleUsesSharedCARTDECKVideoTreatment() {
        for (system in GameSystem.all()) {
            val expected =
                if (system.id in setOf(SystemID.NDS, SystemID.NINTENDO_3DS)) {
                    CartdeckViewportPolicy.PortraitMode.DUAL_SCREEN
                } else {
                    CartdeckViewportPolicy.PortraitMode.STANDARD
                }
            assertEquals(system.id.name, expected, CartdeckViewportPolicy.portraitMode(system.id))
        }
    }

    @Test
    fun allFactoryFinishesProvideSpecialConsoleHardwareSeats() {
        for (edition in CartdeckEditionPreset.entries) {
            assertNotNull(CartdeckHardwareResource.seat(edition, "dpad"))
            assertEquals(
                CartdeckHardwareResource.seat(edition, "dpad"),
                CartdeckHardwareResource.seat(edition, "c-direction"),
            )
            for (key in listOf("face-ab", "face-abxy", "analog-left", "analog-right")) {
                assertNotNull("$edition $key", CartdeckHardwareResource.seat(edition, key))
            }
            for (key in listOf("shoulder-L", "shoulder-R", "shoulder-L2", "shoulder-R2")) {
                assertNotNull("$edition $key", CartdeckHardwareResource.seat(edition, key))
            }
            for (key in listOf("start", "select", "menu", "ds-mic", "ds-layout")) {
                assertNotNull("$edition $key", CartdeckHardwareResource.seat(edition, key))
            }
        }
    }

    @Test
    fun pivotArcRemainsOptInAndUnsupportedConsolesRetainStandard() {
        assertNotNull(PivotArcLayoutProfile.activeFor(SystemID.GBA))
        assertNotNull(PivotArcLayoutProfile.activeFor(SystemID.SNES))
        for (system in listOf(SystemID.PSX, SystemID.PSP, SystemID.N64, SystemID.NDS)) {
            assertEquals(null, PivotArcLayoutProfile.activeFor(system))
        }
    }
}
