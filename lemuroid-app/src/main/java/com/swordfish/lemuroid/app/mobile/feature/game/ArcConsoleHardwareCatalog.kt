package com.swordfish.lemuroid.app.mobile.feature.game

import com.swordfish.lemuroid.lib.library.SystemID

/**
 * Every CARTDECK system receives an explicit physical controller design lane.
 *
 * This is a design/implementation inventory, NOT a claim that all consoles
 * already have working PivotArc or standalone GripArc gameplay. No native
 * controller inputs are inferred from these visual families.
 */
internal enum class ArcHardwareFamily {
    TWO_BUTTON,
    FOUR_BUTTON_SHOULDERS,
    SIX_BUTTON,
    HANDHELD_DUAL_DIRECTION,
    TOUCH_DUAL_SCREEN,
    SINGLE_ANALOG,
    DUAL_ANALOG,
    ARCADE,
    ATARI,
    WII_REMOTE,
    DESKTOP,
    HANDHELD_OPTIONS,
    WII_U_GAMEPAD,
}

/**
 * Explicit mapping must be maintained whenever a new SystemID is registered.
 * The two native GameCube interpretations remain separate: NGC is Neo Geo
 * Pocket Color, while GAMECUBE means Nintendo GameCube.
 */
internal object ArcConsoleHardwareCatalog {
    fun family(system: SystemID): ArcHardwareFamily =
        when (system) {
            SystemID.GB, SystemID.GBC, SystemID.NES, SystemID.SMS, SystemID.GG,
            SystemID.NGP, SystemID.NGC, SystemID.PC_ENGINE,
            -> ArcHardwareFamily.TWO_BUTTON

            SystemID.GBA, SystemID.SNES -> ArcHardwareFamily.FOUR_BUTTON_SHOULDERS
            SystemID.GENESIS, SystemID.SEGACD -> ArcHardwareFamily.SIX_BUTTON
            SystemID.WS, SystemID.WSC -> ArcHardwareFamily.HANDHELD_DUAL_DIRECTION
            SystemID.NDS, SystemID.NINTENDO_3DS -> ArcHardwareFamily.TOUCH_DUAL_SCREEN
            SystemID.PSP, SystemID.N64 -> ArcHardwareFamily.SINGLE_ANALOG
            SystemID.PSX, SystemID.PLAYSTATION_2, SystemID.PLAYSTATION_3,
            SystemID.DREAMCAST, SystemID.GAMECUBE, SystemID.XBOX, SystemID.XBOX360,
            ->
                ArcHardwareFamily.DUAL_ANALOG

            SystemID.FBNEO, SystemID.MAME2003PLUS -> ArcHardwareFamily.ARCADE
            SystemID.ATARI2600, SystemID.ATARI7800 -> ArcHardwareFamily.ATARI
            SystemID.WII -> ArcHardwareFamily.WII_REMOTE
            SystemID.DOS -> ArcHardwareFamily.DESKTOP
            SystemID.LYNX -> ArcHardwareFamily.HANDHELD_OPTIONS
            SystemID.WII_U -> ArcHardwareFamily.WII_U_GAMEPAD
        }

    /** Never mistake an intended family for an available runtime layout. */
    fun pivotRuntimeMapped(system: SystemID): Boolean = PivotArcLayoutProfile.activeFor(system) != null

    fun gripInputMapped(system: SystemID): Boolean = GripArcInputCatalog.forSystem(system).isNotEmpty()
}
