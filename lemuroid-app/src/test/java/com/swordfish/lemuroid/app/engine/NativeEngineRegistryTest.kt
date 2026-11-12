package com.swordfish.lemuroid.app.engine

import com.swordfish.lemuroid.app.shared.game.engine.NativeEngineRegistry
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
    @Test
    fun everyRegisteredSystemHasOnlyCompatibleIntegratedEnginePaths() {
        val systems = (GameSystem.all() + NativeGameSystems.configurations()).distinctBy { it.id }
        systems.forEach { system ->
            val engines = NativeEngineRegistry.engines(system)
            assertTrue(system.id.dbname, engines.isNotEmpty())
            assertEquals(engines.size, engines.map { it.id }.distinct().size)
            engines.forEach { engine ->
                assertEquals(system.id, engine.system)
                assertEquals(system.supportedExtensions.toSet(), engine.formats)
                if (system.id == SystemID.XBOX) {
                    assertEquals(NativeEngineRegistry.Host.HAKUX_SDL3, engine.host)
                    assertEquals(null, engine.core)
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
            ).single()
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
