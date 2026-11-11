package com.swordfish.lemuroid.app.psp

import com.swordfish.lemuroid.app.shared.game.PSPModernCoreConfig
import com.swordfish.lemuroid.lib.core.CoreVariable
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PSPModernCoreConfigTest {
    @Test
    fun legacyEngineKeepsItsExistingDefaults() {
        val legacy = SystemCoreConfig(CoreID.PPSSPP, hashMapOf())
        assertSame(legacy, PSPModernCoreConfig.configure(legacy))
    }

    @Test
    fun modernEngineUsesVerifiedGlesJitOptionsWithoutDuplicateDefaults() {
        val candidate =
            SystemCoreConfig(
                CoreID.PPSSPP_MODERN,
                hashMapOf(),
                defaultSettings = listOf(CoreVariable("ppsspp_cpu_core", "Interpreter")),
            )
        val options = PSPModernCoreConfig.configure(candidate).defaultSettings
        assertEquals(options.size, options.map { it.key }.distinct().size)
        assertEquals("opengl", options.single { it.key == "ppsspp_backend" }.value)
        assertEquals("JIT", options.single { it.key == "ppsspp_cpu_core" }.value)
        assertEquals("disabled", options.single { it.key == "ppsspp_frameskip" }.value)
    }
}
