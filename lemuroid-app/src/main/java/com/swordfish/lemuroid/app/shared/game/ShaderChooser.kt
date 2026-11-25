package com.swordfish.lemuroid.app.shared.game

import com.swordfish.lemuroid.app.shared.settings.HDModeQuality
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.libretrodroid.ShaderConfig
import timber.log.Timber

object ShaderChooser {
    fun getShaderForSystem(
        glEsMajor: Int,
        choices: DisplaySettingsStore.Choices,
        system: GameSystem,
    ): ShaderConfig {
        val selection = SmartVisual.resolve(choices, system.id, libretro = true, glEsMajor = glEsMajor)
        Timber.i("SmartVisual: ${system.id} uses $selection")
        return when (selection.shader) {
            // LibretroDroid's default fragment directly samples the source texture and outputs opaque RGB.
            SmartVisual.Shader.NATIVE, SmartVisual.Shader.UNFILTERED -> ShaderConfig.Default
            SmartVisual.Shader.SHARP -> ShaderConfig.Sharp
            SmartVisual.Shader.LCD -> ShaderConfig.LCD
            SmartVisual.Shader.CUT -> getHDShaderForSystem(system, selection.tier)
            SmartVisual.Shader.LEGACY_FILTER ->
                when (choices.filter) {
                    "crt" -> ShaderConfig.CRT
                    "lcd" -> ShaderConfig.LCD
                    "smooth" -> ShaderConfig.Default
                    "sharp" -> ShaderConfig.Sharp
                    else -> getDefaultShaderForSystem(system)
                }
        }
    }

    private fun getDefaultShaderForSystem(system: GameSystem): ShaderConfig {
        return when (system.id) {
            SystemID.GBA -> ShaderConfig.LCD
            SystemID.GBC -> ShaderConfig.LCD
            SystemID.GB -> ShaderConfig.LCD
            SystemID.N64 -> ShaderConfig.CRT
            SystemID.GENESIS -> ShaderConfig.CRT
            SystemID.SEGACD -> ShaderConfig.CRT
            SystemID.NES -> ShaderConfig.CRT
            SystemID.SNES -> ShaderConfig.CRT
            SystemID.FBNEO -> ShaderConfig.CRT
            SystemID.SMS -> ShaderConfig.CRT
            SystemID.PSP -> ShaderConfig.LCD
            SystemID.NDS -> ShaderConfig.LCD
            SystemID.GG -> ShaderConfig.LCD
            SystemID.ATARI2600 -> ShaderConfig.CRT
            SystemID.PSX -> ShaderConfig.CRT
            SystemID.MAME2003PLUS -> ShaderConfig.CRT
            SystemID.ATARI7800 -> ShaderConfig.CRT
            SystemID.PC_ENGINE -> ShaderConfig.CRT
            SystemID.LYNX -> ShaderConfig.LCD
            SystemID.DOS -> ShaderConfig.CRT
            SystemID.NGP -> ShaderConfig.LCD
            SystemID.NGC -> ShaderConfig.LCD
            SystemID.WS -> ShaderConfig.LCD
            SystemID.WSC -> ShaderConfig.LCD
            SystemID.NINTENDO_3DS -> ShaderConfig.LCD
            SystemID.PLAYSTATION_3, SystemID.WII_U -> ShaderConfig.Default
            SystemID.DREAMCAST, SystemID.GAMECUBE, SystemID.PLAYSTATION_2, SystemID.WII, SystemID.XBOX, SystemID.XBOX360 -> ShaderConfig.Default
        }
    }

    private fun getHDShaderForSystem(
        system: GameSystem,
        hdModeQuality: HDModeQuality,
    ): ShaderConfig {
        return when (hdModeQuality) {
            HDModeQuality.LOW -> getLowQualityHdMode(system)
            HDModeQuality.MEDIUM -> getMediumQualityHdMode(system)
            HDModeQuality.HIGH -> getHighQualityHdMode(system)
        }
    }

    private fun getLowQualityHdMode(system: GameSystem): ShaderConfig {
        val upscale8BitsMobile =
            ShaderConfig.CUT(
                blendMinContrastEdge = 0.00f,
                blendMaxContrastEdge = 0.50f,
                blendMaxSharpness = 0.85f,
            )

        val upscale8Bits =
            ShaderConfig.CUT(
                blendMinContrastEdge = 0.00f,
                blendMaxContrastEdge = 0.50f,
                blendMaxSharpness = 0.75f,
            )

        val upscale16BitsMobile =
            ShaderConfig.CUT(
                blendMinContrastEdge = 0.10f,
                blendMaxContrastEdge = 0.60f,
                blendMaxSharpness = 0.85f,
            )

        val upscale16Bits =
            ShaderConfig.CUT(
                blendMinContrastEdge = 0.10f,
                blendMaxContrastEdge = 0.60f,
                blendMaxSharpness = 0.75f,
            )

        val upscale32Bits =
            ShaderConfig.CUT(
                blendMinContrastEdge = 0.25f,
                blendMaxContrastEdge = 0.75f,
                blendMaxSharpness = 0.75f,
            )

        val modern =
            ShaderConfig.CUT(
                blendMinContrastEdge = 0.25f,
                blendMaxContrastEdge = 0.75f,
                blendMaxSharpness = 0.50f,
            )

        return getConfigForSystem(
            system,
            upscale16BitsMobile,
            upscale8BitsMobile,
            upscale32Bits,
            upscale16Bits,
            upscale8Bits,
            modern,
        )
    }

    private fun getMediumQualityHdMode(system: GameSystem): ShaderConfig {
        val upscale8BitsMobile =
            ShaderConfig.CUT2(
                blendMinContrastEdge = 0.00f,
                blendMaxContrastEdge = 0.30f,
                blendMaxSharpness = 0.75f,
                hardEdgesSearchMaxError = 0.50f,
            )

        val upscale8Bits =
            ShaderConfig.CUT2(
                blendMinContrastEdge = 0.00f,
                blendMaxContrastEdge = 0.30f,
                blendMaxSharpness = 0.75f,
                hardEdgesSearchMaxError = 0.50f,
            )

        val upscale16BitsMobile =
            ShaderConfig.CUT2(
                blendMinContrastEdge = 0.10f,
                blendMaxContrastEdge = 0.50f,
                blendMaxSharpness = 0.75f,
                hardEdgesSearchMaxError = 0.75f,
            )

        val upscale16Bits =
            ShaderConfig.CUT2(
                blendMinContrastEdge = 0.10f,
                blendMaxContrastEdge = 0.50f,
                blendMaxSharpness = 0.75f,
                hardEdgesSearchMaxError = 0.25f,
            )

        val upscale32Bits =
            ShaderConfig.CUT2(
                blendMinContrastEdge = 0.10f,
                blendMaxContrastEdge = 0.50f,
                blendMaxSharpness = 0.75f,
                hardEdgesSearchMaxError = 0.25f,
            )

        val modern =
            ShaderConfig.CUT2(
                blendMinContrastEdge = 0.10f,
                blendMaxContrastEdge = 0.50f,
                blendMaxSharpness = 0.50f,
                hardEdgesSearchMaxError = 0.25f,
            )

        return getConfigForSystem(
            system,
            upscale16BitsMobile,
            upscale8BitsMobile,
            upscale32Bits,
            upscale16Bits,
            upscale8Bits,
            modern,
        )
    }

    private fun getHighQualityHdMode(system: GameSystem): ShaderConfig {
        val upscale8BitsMobile =
            ShaderConfig.CUT3(
                blendMinContrastEdge = 0.00f,
                blendMaxContrastEdge = 0.30f,
                blendMaxSharpness = 0.75f,
                hardEdgesSearchMaxError = 0.50f,
            )

        val upscale8Bits =
            ShaderConfig.CUT3(
                blendMinContrastEdge = 0.00f,
                blendMaxContrastEdge = 0.30f,
                blendMaxSharpness = 0.75f,
                hardEdgesSearchMaxError = 0.50f,
            )

        val upscale16BitsMobile =
            ShaderConfig.CUT3(
                blendMinContrastEdge = 0.10f,
                blendMaxContrastEdge = 0.50f,
                blendMaxSharpness = 0.75f,
                hardEdgesSearchMaxError = 0.25f,
            )

        val upscale16Bits =
            ShaderConfig.CUT3(
                blendMinContrastEdge = 0.10f,
                blendMaxContrastEdge = 0.50f,
                blendMaxSharpness = 0.75f,
                hardEdgesSearchMaxError = 0.25f,
            )

        val upscale32Bits =
            ShaderConfig.CUT3(
                blendMinContrastEdge = 0.10f,
                blendMaxContrastEdge = 0.50f,
                blendMaxSharpness = 0.75f,
                hardEdgesSearchMaxError = 0.25f,
            )

        val modern =
            ShaderConfig.CUT3(
                blendMinContrastEdge = 0.10f,
                blendMaxContrastEdge = 0.50f,
                blendMaxSharpness = 0.75f,
                hardEdgesSearchMaxError = 0.25f,
            )

        return getConfigForSystem(
            system,
            upscale16BitsMobile,
            upscale8BitsMobile,
            upscale32Bits,
            upscale16Bits,
            upscale8Bits,
            modern,
        )
    }

    private fun getConfigForSystem(
        system: GameSystem,
        upscale16BitsMobile: ShaderConfig,
        upscale8BitsMobile: ShaderConfig,
        upscale32Bits: ShaderConfig,
        upscale16Bits: ShaderConfig,
        upscale8Bits: ShaderConfig,
        modern: ShaderConfig,
    ): ShaderConfig {
        return when (system.id) {
            SystemID.GBA -> upscale16BitsMobile
            SystemID.GBC -> upscale8BitsMobile
            SystemID.GB -> upscale8BitsMobile
            SystemID.N64 -> upscale32Bits
            SystemID.GENESIS -> upscale16Bits
            SystemID.SEGACD -> upscale16Bits
            SystemID.NES -> upscale8Bits
            SystemID.SNES -> upscale16Bits
            SystemID.FBNEO -> upscale32Bits
            SystemID.SMS -> upscale8Bits
            SystemID.PSP -> modern
            SystemID.NDS -> upscale32Bits
            SystemID.GG -> upscale8BitsMobile
            SystemID.ATARI2600 -> upscale8Bits
            SystemID.PSX -> upscale32Bits
            SystemID.MAME2003PLUS -> upscale32Bits
            SystemID.ATARI7800 -> upscale8Bits
            SystemID.PC_ENGINE -> upscale16Bits
            SystemID.LYNX -> upscale8BitsMobile
            SystemID.DOS -> upscale32Bits
            SystemID.NGP -> upscale8BitsMobile
            SystemID.NGC -> upscale8BitsMobile
            SystemID.WS -> upscale16BitsMobile
            SystemID.WSC -> upscale16BitsMobile
            SystemID.NINTENDO_3DS -> modern
            // Wii U's experimental Vulkan path must not enable Libretro GLES shaders.
            SystemID.PLAYSTATION_3, SystemID.WII_U -> ShaderConfig.Default
            SystemID.DREAMCAST, SystemID.GAMECUBE, SystemID.PLAYSTATION_2, SystemID.WII, SystemID.XBOX, SystemID.XBOX360 -> modern
        }
    }
}
