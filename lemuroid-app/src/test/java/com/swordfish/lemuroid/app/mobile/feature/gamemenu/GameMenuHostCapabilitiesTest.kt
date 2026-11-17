package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import com.swordfish.lemuroid.app.shared.game.engine.NativeEngineRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

/** Android integration contract; the standalone Xbox JVM harness has no engine registry. */
class GameMenuHostCapabilitiesTest {
    @Test fun playerPortsAreLimitedByTheSelectedEngineAttachmentContract() {
        val native = NativeEngineRegistry.Host.HAKUX_SDL3
        val libretro = NativeEngineRegistry.Host.LIBRETRO
        assertEquals(1, controllerPortCapacity(native, setOf(0, 1), true))
        assertEquals(2, controllerPortCapacity(libretro, setOf(0, 1), true))
        assertEquals(16, controllerPortCapacity(null, emptySet(), false))
        assertEquals(1, controllerPortCapacity(null, emptySet(), true))
        assertEquals("Atlas", playerMenuTitle("Atlas (USA).gba"))
        assertEquals("Xbox game", playerMenuTitle("Xbox game.xiso"))
        assertEquals("Ms. Pac-Man", playerMenuTitle("Ms. Pac-Man"))
    }
}
