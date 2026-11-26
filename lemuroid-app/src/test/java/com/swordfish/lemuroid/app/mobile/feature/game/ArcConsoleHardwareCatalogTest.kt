package com.swordfish.lemuroid.app.mobile.feature.game

import com.swordfish.lemuroid.lib.library.SystemID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArcConsoleHardwareCatalogTest {
    @Test
    fun everyRegisteredSystemHasAnExplicitPhysicalHardwareFamily() {
        val families = SystemID.entries.associateWith(ArcConsoleHardwareCatalog::family)
        assertEquals(SystemID.entries.size, families.size)
        assertTrue(families.values.all { it in ArcHardwareFamily.entries })
        for (system in SystemID.entries) assertNotNull(system.name, families[system])
    }

    @Test
    fun distinctInputArchitecturesCannotBeConflated() {
        assertEquals(ArcHardwareFamily.TWO_BUTTON, ArcConsoleHardwareCatalog.family(SystemID.NGC))
        assertEquals(ArcHardwareFamily.DUAL_ANALOG, ArcConsoleHardwareCatalog.family(SystemID.GAMECUBE))
        assertEquals(ArcHardwareFamily.WII_REMOTE, ArcConsoleHardwareCatalog.family(SystemID.WII))
        assertEquals(ArcHardwareFamily.TOUCH_DUAL_SCREEN, ArcConsoleHardwareCatalog.family(SystemID.NDS))
        assertEquals(ArcHardwareFamily.ARCADE, ArcConsoleHardwareCatalog.family(SystemID.FBNEO))
    }

    @Test
    fun plannedPhysicalFamilyDoesNotMisrepresentRuntimeAvailability() {
        for (system in listOf(SystemID.NES, SystemID.SMS, SystemID.PC_ENGINE)) {
            assertTrue(system.name, ArcConsoleHardwareCatalog.pivotRuntimeMapped(system))
            assertTrue(system.name, ArcConsoleHardwareCatalog.gripInputMapped(system))
        }
        for (system in listOf(SystemID.N64, SystemID.PSX, SystemID.WII_U)) {
            assertFalse(
                "Do not falsely claim support for $system",
                ArcConsoleHardwareCatalog.pivotRuntimeMapped(system),
            )
        }
    }
}
