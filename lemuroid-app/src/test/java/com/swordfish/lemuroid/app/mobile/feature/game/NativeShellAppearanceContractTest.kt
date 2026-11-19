package com.swordfish.lemuroid.app.mobile.feature.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NativeShellAppearanceContractTest {
    @Test
    fun standardAndNativeHostsUseTheSharedAppearanceAndForwardDecoration() {
        for (name in listOf("MobileGameScreen.kt", "XboxGameControls.kt")) {
            val relative = "src/main/java/com/swordfish/lemuroid/app/mobile/feature/game/$name"
            val source =
                listOf(File(relative), File("lemuroid-app", relative))
                    .firstOrNull { it.isFile }
                    ?: error("Gameplay source is unavailable: $name")
            val text = source.readText()
            assertTrue(text.contains("val resolved = rememberGameplayAppearance()"))
            assertFalse(text.contains("ShelfStore("))
            val shellCall = text.substringAfter("CartdeckConsoleShell(").substringBefore("\n                )")
            listOf(
                "patternId = resolved.patternId",
                "stampId = resolved.stampId",
                "decal = resolved.decal",
                "lighting = resolved.lighting",
                "highContrast = appearance.highContrast",
            ).forEach { argument ->
                assertTrue("$name must forward: $argument", shellCall.contains(argument))
            }
            assertTrue(text.contains("remember(resolved.controllerPalette)"))
        }
    }
}
