package com.swordfish.lemuroid.app.shared.game

import com.swordfish.lemuroid.lib.core.CoreVariable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CitraCompatibilityModeTest {
    @Test
    fun comparisonsChangeOnlyOneVariableOrHostDrawPolicy() {
        val original =
            listOf(
                CoreVariable("citra_use_acc_geo_shaders", "enabled"),
                CoreVariable("citra_use_acc_mul", "disabled"),
                CoreVariable("citra_use_hw_shader_cache", "disabled"),
            )
        assertEquals(original, CitraCompatibilityMode.ORIGINAL.applyOptions(original))
        assertEquals(original, CitraCompatibilityMode.REDRAW.applyOptions(original))
        assertFalse(CitraCompatibilityMode.REDRAW.skipDuplicates(true))
        assertTrue(CitraCompatibilityMode.ORIGINAL.skipDuplicates(true))
        assertFalse(CitraCompatibilityMode.ORIGINAL.skipDuplicates(false))
        CitraCompatibilityMode.entries.filter { it.optionKey != null }.forEach { mode ->
            val key = requireNotNull(mode.optionKey)
            val changed = mode.applyOptions(original)
            assertEquals(original.filterNot { it.key == key }, changed.filterNot { it.key == key })
            assertEquals(mode.optionValue, changed.single { it.key == key }.value)
            assertTrue(mode.skipDuplicates(true))
        }
    }
}
