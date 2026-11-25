package com.swordfish.lemuroid.app.shared.game

import com.swordfish.lemuroid.app.shared.settings.HDModeQuality
import com.swordfish.lemuroid.lib.library.SystemID

enum class ImageQuality { AUTOMATIC, ORIGINAL, ENHANCED }

/** Startup-only renderer policy. The core, viewport, timing and saved preferences are independent. */
internal object SmartVisual {
    enum class Shader { NATIVE, UNFILTERED, SHARP, LCD, LEGACY_FILTER, CUT }

    data class Selection(val shader: Shader, val tier: HDModeQuality = HDModeQuality.LOW)

    fun usesLegacyQuality(choices: DisplaySettingsStore.Choices): Boolean =
        choices.imageQuality == null && choices.hasLegacyQualityOverride()

    fun resolve(
        choices: DisplaySettingsStore.Choices,
        system: SystemID,
        libretro: Boolean,
        glEsMajor: Int,
    ): Selection {
        if (!libretro) return Selection(Shader.NATIVE)
        if (glEsMajor < 2 || choices.imageQuality == ImageQuality.ORIGINAL) return Selection(Shader.UNFILTERED)
        if (choices.imageQuality == ImageQuality.ENHANCED || (usesLegacyQuality(choices) && choices.hd == true)) {
            val tier =
                choices.quality?.let { HDModeQuality.parse(it) }
                    ?: if (usesLegacyQuality(choices)) HDModeQuality.MEDIUM else HDModeQuality.LOW
            return Selection(Shader.CUT, if (glEsMajor >= 3) tier else HDModeQuality.LOW)
        }
        if (usesLegacyQuality(choices)) return Selection(Shader.LEGACY_FILTER)
        return Selection(
            when (system) {
                SystemID.GB, SystemID.GBC, SystemID.GBA, SystemID.GG, SystemID.LYNX,
                SystemID.NDS, SystemID.NGP, SystemID.NGC, SystemID.WS, SystemID.WSC,
                -> Shader.LCD
                SystemID.NES, SystemID.SNES, SystemID.SMS, SystemID.GENESIS, SystemID.SEGACD,
                SystemID.PC_ENGINE, SystemID.FBNEO, SystemID.MAME2003PLUS,
                SystemID.ATARI2600, SystemID.ATARI7800, SystemID.PSX,
                -> Shader.SHARP
                else -> Shader.UNFILTERED
            },
        )
    }
}
