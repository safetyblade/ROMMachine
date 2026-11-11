package com.swordfish.lemuroid.app.shared.game

import com.swordfish.lemuroid.lib.core.CoreVariable
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.SystemCoreConfig

/** v1.20.4 option names verified against the pinned libretro_core_options.h. */
object PSPModernCoreConfig {
    fun configure(config: SystemCoreConfig): SystemCoreConfig {
        if (config.coreID != CoreID.PPSSPP_MODERN) return config
        val defaults =
            listOf(
                CoreVariable("ppsspp_backend", "opengl"),
                CoreVariable("ppsspp_cpu_core", "JIT"),
                CoreVariable("ppsspp_frameskip", "disabled"),
                CoreVariable("ppsspp_auto_frameskip", "disabled"),
                CoreVariable("ppsspp_frame_duplication", "enabled"),
            )
        return config.copy(
            // LibretroDroid supplies a GLES surface, not a Vulkan render interface.
            exposedSettings = config.exposedSettings.filterNot { it.key == "ppsspp_backend" },
            exposedAdvancedSettings = config.exposedAdvancedSettings.filterNot { it.key == "ppsspp_backend" },
            defaultSettings = config.defaultSettings.filterNot { old -> defaults.any { it.key == old.key } } + defaults,
        )
    }
}
