package com.swordfish.lemuroid.app.shared.game

import com.swordfish.lemuroid.app.shared.game.DisplaySettingsStore.Choices
import com.swordfish.lemuroid.app.shared.game.SmartVisual.Shader
import com.swordfish.lemuroid.app.shared.settings.HDModeQuality
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.libretrodroid.ShaderConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartVisualTest {
    @Test fun rendererReceivesTheActualExistingShaderVariants() {
        val gba = GameSystem.findById(SystemID.GBA.dbname)
        val defaults = Choices()
        assertEquals(ShaderConfig.LCD, ShaderChooser.getShaderForSystem(3, defaults, gba))
        assertEquals(ShaderConfig.CRT, ShaderChooser.getShaderForSystem(3, defaults.copy(filter = "crt"), gba))
        assertEquals(
            ShaderConfig.Default,
            ShaderChooser.getShaderForSystem(3, defaults.copy(imageQuality = ImageQuality.ORIGINAL), gba),
        )
        val enhanced = defaults.copy(imageQuality = ImageQuality.ENHANCED)
        assertTrue(ShaderChooser.getShaderForSystem(3, enhanced, gba) is ShaderConfig.CUT)
        assertTrue(ShaderChooser.getShaderForSystem(3, enhanced.copy(quality = 1), gba) is ShaderConfig.CUT2)
        assertTrue(ShaderChooser.getShaderForSystem(3, enhanced.copy(quality = 2), gba) is ShaderConfig.CUT3)
        assertTrue(ShaderChooser.getShaderForSystem(2, enhanced.copy(quality = 2), gba) is ShaderConfig.CUT)
    }

    @Test fun conservativeDefaultsUseLCDForHandheldsSharpForPixelsAndPassThroughFor3d() {
        for ((system, expected) in mapOf(
            SystemID.GB to Shader.LCD,
            SystemID.GBC to Shader.LCD,
            SystemID.GBA to Shader.LCD,
            SystemID.NDS to Shader.LCD,
            SystemID.SNES to Shader.SHARP,
            SystemID.PSX to Shader.SHARP,
            SystemID.PSP to Shader.UNFILTERED,
            SystemID.DREAMCAST to Shader.UNFILTERED,
        )) {
            assertEquals(system.name, expected, SmartVisual.resolve(Choices(), system, true, 3).shader)
        }
    }

    @Test fun everySystemKeepsOriginalUnenhancedAndNativeRendererIndependent() {
        for (system in SystemID.entries) {
            val original = Choices(filter = "crt", hd = true, imageQuality = ImageQuality.ORIGINAL)
            assertEquals(Shader.UNFILTERED, SmartVisual.resolve(original, system, true, 3).shader)
            for (mode in ImageQuality.entries) {
                assertEquals(
                    Shader.NATIVE,
                    SmartVisual.resolve(original.copy(imageQuality = mode), system, false, 3).shader,
                )
            }
        }
    }

    @Test fun enhancedHonoursExplicitTiersAndDowngradesByCapability() {
        val enhanced = Choices(quality = 2, imageQuality = ImageQuality.ENHANCED)
        val legacy = enhanced.copy(imageQuality = null, hd = true)
        for (choices in listOf(enhanced, legacy)) {
            assertEquals(
                SmartVisual.Selection(Shader.CUT, HDModeQuality.HIGH),
                SmartVisual.resolve(choices, SystemID.SNES, true, 3),
            )
            assertEquals(
                SmartVisual.Selection(Shader.CUT, HDModeQuality.LOW),
                SmartVisual.resolve(choices, SystemID.SNES, true, 2),
            )
            assertEquals(Shader.UNFILTERED, SmartVisual.resolve(choices, SystemID.SNES, true, 1).shader)
        }
        assertEquals(Shader.LEGACY_FILTER, SmartVisual.resolve(Choices(filter = "crt"), SystemID.GBA, true, 3).shader)
        assertEquals(
            Shader.LCD,
            SmartVisual.resolve(legacy.copy(imageQuality = ImageQuality.AUTOMATIC), SystemID.GBA, true, 3).shader,
        )
    }
}
