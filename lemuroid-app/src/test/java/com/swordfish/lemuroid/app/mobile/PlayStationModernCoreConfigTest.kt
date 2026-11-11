package com.swordfish.lemuroid.app.mobile

import com.swordfish.lemuroid.app.shared.game.PlayStationModernCoreConfig
import com.swordfish.lemuroid.lib.core.CoreVariable
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.ExposedSetting
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PlayStationModernCoreConfigTest {
    private fun profile(core: CoreID) =
        SystemCoreConfig(
            coreID = core,
            controllerConfigs = hashMapOf(),
            exposedSettings = listOf(ExposedSetting("pcsx_rearmed_frameskip", 123)),
            defaultSettings = listOf(CoreVariable("pcsx_rearmed_drc", "disabled")),
        )

    @Test
    fun upstreamUsesCurrentFrameskipOptionAndArm64DynarecDefault() {
        val config = PlayStationModernCoreConfig.configure(profile(CoreID.PCSX_REARMED_MODERN))
        assertEquals("pcsx_rearmed_frameskip_type", config.exposedSettings.single().key)
        assertEquals(123, config.exposedSettings.single().titleId)
        assertEquals("enabled", config.defaultSettings.single { it.key == "pcsx_rearmed_drc" }.value)
        assertEquals("disabled", config.defaultSettings.single { it.key == "pcsx_rearmed_frameskip_type" }.value)
    }

    @Test
    fun fallbackAndOtherSystemsRetainTheirOriginalSettings() {
        listOf(CoreID.PCSX_REARMED, CoreID.PPSSPP_MODERN).forEach {
            val config = profile(it)
            assertSame(config, PlayStationModernCoreConfig.configure(config))
        }
    }
}
