package com.swordfish.lemuroid.app.shared.game

import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CitraRenderingDefaultsTest {
    @Test
    fun citraRetainsFramesAndExposesSupportedShaderOptions() {
        val config =
            GameSystem.findById(SystemID.NINTENDO_3DS.dbname).systemCoreConfigs
                .single { it.coreID == CoreID.CITRA }
        assertFalse(config.skipDuplicateFrames)
        assertFalse(config.statesSupported)
        assertTrue(config.supportsLibretroVFS)
        val keys = config.exposedSettings.map { it.key }
        assertTrue("citra_use_hw_shaders" in keys)
        assertTrue("citra_use_acc_geo_shaders" in keys)
        assertTrue("citra_use_acc_mul" in keys)
    }
}
