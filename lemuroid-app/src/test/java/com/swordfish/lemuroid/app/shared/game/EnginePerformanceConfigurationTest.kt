package com.swordfish.lemuroid.app.shared.game

import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.NativeGameSystems
import com.swordfish.lemuroid.lib.library.SystemID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnginePerformanceConfigurationTest {
    @Test
    fun pspRedrawsDuplicateFramesWithoutChangingCoreDuplication() {
        val config = GameSystem.findById(SystemID.PSP.dbname).systemCoreConfigs.single()
        assertFalse(config.skipDuplicateFrames)
        assertEquals("enabled", config.defaultSettings.single { it.key == "ppsspp_frame_duplication" }.value)
    }

    @Test
    fun preservePreviouslyAcceptedDefaults() {
        val psx = GameSystem.findById(SystemID.PSX.dbname).systemCoreConfigs.single()
        assertEquals("disabled", psx.defaultSettings.single { it.key == "pcsx_rearmed_drc" }.value)
        val ds = GameSystem.findById(SystemID.NDS.dbname).systemCoreConfigs
        val desmume = ds.single { it.coreID == CoreID.DESMUME }
        assertEquals("1", desmume.defaultSettings.single { it.key == "desmume_frameskip" }.value)
        val melonds = ds.single { it.coreID == CoreID.MELONDS }
        assertEquals("enabled", melonds.defaultSettings.single { it.key == "melonds_threaded_renderer" }.value)
    }

    @Test
    fun dolphinOptionsKeepAccuracyPreservingChoices() {
        val systems = NativeGameSystems.configurations().filter { it.id in setOf(SystemID.GAMECUBE, SystemID.WII) }
        assertEquals(2, systems.size)
        systems.forEach { system ->
            val config = system.systemCoreConfigs.single()
            val shaders = config.exposedAdvancedSettings.single { it.key == "dolphin_shader_compilation_mode" }
            assertEquals(listOf("0", "1", "2"), shaders.values.map { it.key })
            assertTrue(config.defaultSettings.any { it.key == "dolphin_renderer" && it.value == "Hardware" })
        }
    }
}
