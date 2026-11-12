package com.swordfish.lemuroid.app.mobile.feature.game

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
