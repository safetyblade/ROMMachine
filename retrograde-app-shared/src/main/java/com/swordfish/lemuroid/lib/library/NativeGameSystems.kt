package com.swordfish.lemuroid.lib.library

import com.swordfish.lemuroid.lib.BuildConfig
import com.swordfish.lemuroid.lib.R
import com.swordfish.lemuroid.lib.core.CoreVariable

/** Native expansions use the existing library/game screen. Enabled independently in engineering APKs. */
object NativeGameSystems {
    fun enabled(): List<GameSystem> {
        val requested = BuildConfig.NATIVE_SYSTEMS.split(",").toSet()
        return configurations().filter { it.id.dbname in requested }
    }

    fun configurations(): List<GameSystem> =
        listOf(
            GameSystem(
                SystemID.XBOX, "Microsoft - Xbox",
                R.string.game_system_title_xbox, R.string.game_system_abbr_xbox,
                systemCoreConfigs = emptyList(),
                uniqueExtensions = emptyList(),
                supportedExtensions = listOf("iso", "xiso"),
                fastForwardSupport = false,
                nativeBackend = "hakux",
                scanOptions =
                    GameSystem.Companion.ScanOptions(
                        scanByPathAndSupportedExtensions = false,
                        scanByFilename = false,
                        scanByPathAndFilename = false,
                    ),
            ),
            GameSystem(
                SystemID.DREAMCAST, "Sega - Dreamcast",
                R.string.game_system_title_dc, R.string.game_system_abbr_dc,
                listOf(
                    SystemCoreConfig(
                        CoreID.FLYCAST_MODERN,
                        controllerConfigs = hashMapOf(0 to arrayListOf(ControllerConfigs.NATIVE_ANALOG)),
                        defaultSettings =
                            listOf(
                                CoreVariable("reicast_hle_bios", "enabled"),
                                CoreVariable("reicast_internal_resolution", "640x480"),
                                CoreVariable("reicast_threaded_rendering", "disabled"),
                                CoreVariable("reicast_vblank_pacing", "enabled"),
                                CoreVariable("reicast_auto_skip_frame", "disabled"),
                                CoreVariable("reicast_enable_dsp", "enabled"),
                                CoreVariable("reicast_per_content_vmus", "All VMUs"),
                            ),
                        supportedOnlyArchitectures = setOf("arm64-v8a"),
                        skipDuplicateFrames = false,
                    ),
                ),
                uniqueExtensions = listOf("gdi", "cdi"),
                supportedExtensions = listOf("gdi", "chd", "m3u", "cdi"),
                hasMultiDiskSupport = true,
                scanOptions =
                    GameSystem.Companion.ScanOptions(
                        scanByPathAndSupportedExtensions = false,
                        scanByFilename = false,
                        scanByPathAndFilename = false,
                    ),
            ),
            GameSystem(
                SystemID.GAMECUBE,
                "Nintendo - GameCube",
                R.string.game_system_title_gc,
                R.string.game_system_abbr_gc,
                listOf(
                    SystemCoreConfig(
                        CoreID.DOLPHIN_MODERN,
                        controllerConfigs = hashMapOf(0 to arrayListOf(ControllerConfigs.GAMECUBE)),
                        defaultSettings =
                            listOf(
                                CoreVariable("dolphin_renderer", "Hardware"),
                                CoreVariable("dolphin_cpu_core", "4"),
                            ),
                        supportedOnlyArchitectures = setOf("arm64-v8a"),
                        skipDuplicateFrames = false,
                    ),
                ),
                uniqueExtensions = listOf("gcm"),
                supportedExtensions = listOf("gcm", "iso", "rvz", "gcz", "ciso", "tgc"),
                scanOptions =
                    GameSystem.Companion.ScanOptions(
                        scanByPathAndSupportedExtensions = false,
                        scanByFilename = false,
                        scanByPathAndFilename = false,
                    ),
            ),
            GameSystem(
                SystemID.WII,
                "Nintendo - Wii",
                R.string.game_system_title_wii,
                R.string.game_system_abbr_wii,
                listOf(
                    SystemCoreConfig(
                        CoreID.DOLPHIN_MODERN,
                        controllerConfigs =
                            hashMapOf(
                                0 to
                                    arrayListOf(
                                        ControllerConfigs.WII_CLASSIC,
                                        ControllerConfigs.WII_SIDEWAYS,
                                        ControllerConfigs.WII_NUNCHUK,
                                    ),
                            ),
                        supportedOnlyArchitectures = setOf("arm64-v8a"),
                        skipDuplicateFrames = false,
                        defaultSettings =
                            listOf(
                                CoreVariable("dolphin_renderer", "Hardware"),
                                CoreVariable("dolphin_cpu_core", "4"),
                                CoreVariable("dolphin_ir_mode", "2"),
                            ),
                        exposedSettings =
                            listOf(
                                ExposedSetting(
                                    "dolphin_ir_mode",
                                    R.string.setting_wii_pointer,
                                    arrayListOf(
                                        ExposedSetting.Value("2", R.string.value_wii_pointer_touch),
                                        ExposedSetting.Value("1", R.string.value_wii_pointer_stick),
                                        ExposedSetting.Value("0", R.string.value_wii_pointer_relative),
                                    ),
                                ),
                            ),
                    ),
                ),
                uniqueExtensions = emptyList(),
                supportedExtensions = listOf("iso", "wbfs", "rvz", "ciso"),
                hasTouchScreen = true,
                fastForwardSupport = false,
                scanOptions =
                    GameSystem.Companion.ScanOptions(
                        scanByPathAndSupportedExtensions = false,
                        scanByFilename = false,
                        scanByPathAndFilename = false,
                    ),
            ),
            GameSystem(
                SystemID.PLAYSTATION_2,
                "Sony - PlayStation 2",
                R.string.game_system_title_ps2,
                R.string.game_system_abbr_ps2,
                listOf(
                    SystemCoreConfig(
                        CoreID.PLAY_MODERN,
                        controllerConfigs = hashMapOf(0 to arrayListOf(ControllerConfigs.NATIVE_ANALOG)),
                        supportedOnlyArchitectures = setOf("arm64-v8a"),
                        statesSupported = false,
                        skipDuplicateFrames = false,
                        defaultSettings =
                            listOf(
                                CoreVariable("play_res_multi", "1x"),
                                CoreVariable("play_presentation_mode", "Fit Screen"),
                                CoreVariable("play_bilinear_filtering", "false"),
                            ),
                    ),
                ),
                uniqueExtensions = emptyList(),
                supportedExtensions = listOf("iso", "chd", "cso", "isz"),
                scanOptions =
                    GameSystem.Companion.ScanOptions(
                        scanByPathAndSupportedExtensions = false,
                        scanByFilename = false,
                        scanByPathAndFilename = false,
                    ),
            ),
        )
}
