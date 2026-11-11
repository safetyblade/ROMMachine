package com.swordfish.lemuroid.app.shared.game

import com.swordfish.lemuroid.lib.core.CoreVariable
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.SystemCoreConfig

/** Options for the pinned upstream ARM64 core, without changing the fallback's profile. */
object PlayStationModernCoreConfig {
    fun configure(config: SystemCoreConfig): SystemCoreConfig {
        if (config.coreID != CoreID.PCSX_REARMED_MODERN) return config
        return config.copy(
            // The old frontend used pcsx_rearmed_frameskip. Upstream now exposes
            // frameskip_type; passing the old key silently leaves that control inert.
            exposedSettings =
                config.exposedSettings.map {
                    if (it.key == "pcsx_rearmed_frameskip") it.copy(key = "pcsx_rearmed_frameskip_type") else it
                },
            defaultSettings =
                config.defaultSettings.filterNot {
                    it.key in
                        setOf(
                            "pcsx_rearmed_drc",
                            "pcsx_rearmed_frameskip",
                            "pcsx_rearmed_frameskip_type",
                            "pcsx_rearmed_memcard1",
                        )
                } +
                    listOf(
                        // Explicit user overrides still win in CoreVariablesManager.
                        CoreVariable("pcsx_rearmed_drc", "enabled"),
                        CoreVariable("pcsx_rearmed_frameskip_type", "disabled"),
                        CoreVariable("pcsx_rearmed_memcard1", "libretro"),
                    ),
        )
    }
}
