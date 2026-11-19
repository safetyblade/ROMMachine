package com.swordfish.lemuroid.app.mobile.feature.game

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NativeShellAppearanceContractTest {
    @Test
    fun nativeShellForwardsSavedDecorationInsteadOfUsingShellDefaults() {
        val relative = "src/main/java/com/swordfish/lemuroid/app/mobile/feature/game/XboxGameControls.kt"
        val source =
            listOf(File(relative), File("lemuroid-app", relative))
                .firstOrNull { it.isFile }
                ?: error("Native gameplay source is unavailable")
        val text = source.readText()
        // The native overlay must supply the same decoration arguments as the
        // standard gameplay screen, rather than silently taking empty defaults.
        val shellCall = text.substringAfter("CartdeckConsoleShell(").substringBefore("\n                )")
        listOf(
            "patternId = if (appearance.highContrast) \"none\" else appearance.patternId",
            "stampId = if (appearance.highContrast) \"none\" else appearance.stampId",
            "decal = if (appearance.highContrast) null else appearance.decal",
            "lighting = appearance.lighting",
            "highContrast = appearance.highContrast",
        ).forEach { argument ->
            assertTrue("Native shell must forward: $argument", shellCall.contains(argument))
        }
        assertTrue(
            "Controller palette must invalidate when high contrast changes",
            text.contains("remember(tokens, appearance.highContrast)"),
        )
    }
}
