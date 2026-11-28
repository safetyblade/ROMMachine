package com.swordfish.lemuroid.app.engine

import com.swordfish.lemuroid.app.shared.game.engine.NativeEngineRegistry
import com.swordfish.lemuroid.app.shared.game.wiiu.WiiuEngine
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.NativeGameSystems
import com.swordfish.lemuroid.lib.library.SystemID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeEngineRegistryTest {
    @Test fun xeniaSelectionAllowsExtensionlessPackagesToReachValidation() {
        val system = GameSystem.findById(SystemID.XBOX360.dbname)
        val engine = NativeEngineRegistry.engines(system).single()
        assertTrue(NativeEngineRegistry.acceptsMedia(engine, ""))
        assertTrue(NativeEngineRegistry.acceptsMedia(engine, "XCP"))
        assertFalse(NativeEngineRegistry.acceptsMedia(engine, "m3u"))
        assertFalse(NativeEngineRegistry.acceptsMedia(engine, "zip"))
    }

    @Test
    fun everyRegisteredSystemHasOnlyCompatibleIntegratedEnginePaths() {
        val systems = (GameSystem.all() + NativeGameSystems.configurations()).distinctBy { it.id }
        systems.forEach { system ->
            val engines = NativeEngineRegistry.engines(system)
            if (system.id == SystemID.WII_U && !WiiuEngine.engineeringBuild) {
                // Catalogue-visible Wii U must not fabricate a GLES/Libretro engine before Cemu's Vulkan host ships.
                assertTrue("Wii U has no integrated runtime yet", engines.isEmpty())
                return@forEach
            }
            assertTrue(system.id.dbname, engines.isNotEmpty())
            assertEquals(engines.size, engines.map { it.id }.distinct().size)
            engines.forEach { engine ->
                assertEquals(system.id, engine.system)
                if (engine.host == NativeEngineRegistry.Host.ARMSX2_ANDROID) {
                    assertEquals(setOf("iso", "chd"), engine.formats)
                    assertEquals(null, engine.core)
                    assertEquals("native/ps2/armsx2", engine.stateNamespace)
                    return@forEach
                }
                assertEquals(system.supportedExtensions.toSet(), engine.formats)
                if (system.id == SystemID.WII_U) {
                    assertEquals(NativeEngineRegistry.Host.CEMU_VULKAN, engine.host)
                    assertEquals(null, engine.core)
                    assertEquals("native/wiiu", engine.stateNamespace)
                } else if (system.id == SystemID.PLAYSTATION_3) {
                    assertTrue(
                        engine.host in
                            setOf(
                                NativeEngineRegistry.Host.ARMSX3_ANDROID,
                                NativeEngineRegistry.Host.RPCS3_LIBRETRO,
                            ),
                    )
                    assertEquals(null, engine.core)
                    assertEquals("native/ps3/${engine.id}", engine.stateNamespace)
                    assertEquals(setOf("arm64-v8a"), engine.architectures)
                    assertEquals(0x00401000, engine.minimumVulkan)
                    assertEquals(setOf("armsx3", "rpcs3-libretro"), engines.map { it.id }.toSet())
                } else if (system.id == SystemID.XBOX) {
                    assertEquals(NativeEngineRegistry.Host.HAKUX_SDL3, engine.host)
                    assertEquals(null, engine.core)
                } else if (system.id == SystemID.XBOX360) {
                    assertEquals(NativeEngineRegistry.Host.XENIA_ANDROID, engine.host)
                    assertEquals(null, engine.core)
                    assertEquals("native/xbox360", engine.stateNamespace)
                } else {
                    assertEquals(NativeEngineRegistry.Host.LIBRETRO, engine.host)
                    assertTrue(engine.core != null)
                }
            }
        }
    }

    @Test
    fun ps2RejectsLowerGlesAndNonArm64BeforeNativeStart() {
        val engine =
            NativeEngineRegistry.engines(
                NativeGameSystems.configurations().first {
                    it.id == SystemID.PLAYSTATION_2
                },
            ).single { it.core == CoreID.PLAY_MODERN }
        val capable = NativeEngineRegistry.Hardware(28, setOf("arm64-v8a"), 0x00030002, 0)
        assertTrue(NativeEngineRegistry.hardwareEligibility(engine, capable).eligible)
        assertFalse(NativeEngineRegistry.hardwareEligibility(engine, capable.copy(gles = 0x00030001)).eligible)
        assertFalse(
            NativeEngineRegistry.hardwareEligibility(engine, capable.copy(architectures = setOf("x86_64"))).eligible,
        )
        assertFalse(NativeEngineRegistry.hardwareEligibility(engine, capable.copy(android = 27)).eligible)
    }

    @Test
    fun dolphinSystemsShareBinaryButNeverStateOrConfigurationIdentity() {
        val configurations = NativeGameSystems.configurations()
        val wii = NativeEngineRegistry.engines(configurations.first { it.id == SystemID.WII }).single()
        val gc = NativeEngineRegistry.engines(configurations.first { it.id == SystemID.GAMECUBE }).single()
        assertEquals(CoreID.DOLPHIN_MODERN, wii.core)
        assertEquals(wii.core, gc.core)
        assertNotEquals(wii.stateNamespace, gc.stateNamespace)
        assertNotEquals(wii.config?.controllerConfigs, gc.config?.controllerConfigs)
        assertEquals(0x00030000, wii.minimumGles)
    }

    @Test
    fun legacyAndModernPlaystationAndPspPathsRemainSeparate() {
        listOf(SystemID.PSX, SystemID.PSP).forEach { system ->
            val engines = NativeEngineRegistry.engines(GameSystem.findById(system.dbname))
            assertTrue(engines.size >= 2)
            assertEquals(engines.size, engines.map { it.stateNamespace }.distinct().size)
        }
    }
}
