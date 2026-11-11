package com.swordfish.lemuroid.app.mobile

import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.storage.scanner.NativeDiscFolderIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NativeDiscFolderIdentityTest {
    private val enabled = setOf(SystemID.DREAMCAST, SystemID.GAMECUBE, SystemID.PLAYSTATION_2)

    @Test
    fun allThreeConsolesIndexFromUnambiguousFolders() {
        assertEquals(
            SystemID.DREAMCAST,
            NativeDiscFolderIdentity.infer("disc.GDI", "/ROMs/Dreamcast/disc.GDI", enabled),
        )
        assertEquals(
            SystemID.DREAMCAST,
            NativeDiscFolderIdentity.infer("game.chd", "/ROMs/Sega Dreamcast/Games/game.chd", enabled),
        )
        assertEquals(
            SystemID.GAMECUBE,
            NativeDiscFolderIdentity.infer("game.iso", "/ROMs/GameCube/game.iso", enabled),
        )
        assertEquals(
            SystemID.GAMECUBE,
            NativeDiscFolderIdentity.infer("game.gcm", "/ROMs/Nintendo_GameCube/game.gcm", enabled),
        )
        assertEquals(
            SystemID.PLAYSTATION_2,
            NativeDiscFolderIdentity.infer("game.iso", "/ROMs/PS2/game.iso", enabled),
        )
    }

    @Test
    fun safelyHandlesAndroidSafUriAndCase() {
        assertEquals(
            SystemID.PLAYSTATION_2,
            NativeDiscFolderIdentity.infer(
                "game.ISO",
                "/tree/primary%3AROMs/document/primary%3AROMs%2FPlayStation%202%2Fgame.ISO",
                enabled,
            ),
        )
        assertEquals(
            SystemID.DREAMCAST,
            NativeDiscFolderIdentity.infer("list.m3u", "/ROMs/Dc/list.m3u", enabled),
        )
    }

    @Test
    fun refusesUnidentifiedAndAmbiguousDiscs() {
        assertNull(NativeDiscFolderIdentity.infer("game.iso", "/ROMs/game.iso", enabled))
        assertNull(NativeDiscFolderIdentity.infer("game.chd", "/ROMs/game.chd", enabled))
        assertNull(NativeDiscFolderIdentity.infer("game.iso", "/ROMs/PS2/GameCube/game.iso", enabled))
        assertNull(NativeDiscFolderIdentity.infer("game.iso", "/ROMs/PS2 Collection/game.iso", enabled))
        assertNull(NativeDiscFolderIdentity.infer("game.rvz", "/ROMs/GameCube/game.rvz", enabled))
        assertNull(NativeDiscFolderIdentity.infer("game.cdi", "/ROMs/Dreamcast/game.cdi", enabled))
        assertNull(NativeDiscFolderIdentity.infer("game.chd", "/ROMs/PS2/game.chd", enabled))
    }

    @Test
    fun doesNotEnableSystemsInBaselineBuild() {
        assertNull(NativeDiscFolderIdentity.infer("game.iso", "/ROMs/PS2/game.iso", emptySet()))
        assertNull(
            NativeDiscFolderIdentity.infer("game.gdi", "/ROMs/Dreamcast/game.gdi", setOf(SystemID.PSX)),
        )
    }

    @Test
    fun signatureAndLegacySystemOverrideFolderAndWiiIsNeverClaimed() {
        assertEquals(
            SystemID.PSX,
            NativeDiscFolderIdentity.resolve(
                "disc.iso",
                "/ROMs/PS2/disc.iso",
                enabled,
                legacy = SystemID.PSX,
            ),
        )
        assertEquals(
            SystemID.GAMECUBE,
            NativeDiscFolderIdentity.resolve(
                "disc.iso",
                "/ROMs/PS2/disc.iso",
                enabled,
                verified = SystemID.GAMECUBE,
            ),
        )
        assertNull(
            NativeDiscFolderIdentity.resolve(
                "wii.iso",
                "/ROMs/GameCube/wii.iso",
                enabled,
                foreignDisc = true,
            ),
        )
        assertEquals(
            SystemID.PSP,
            NativeDiscFolderIdentity.resolve(
                "game.iso",
                "/ROMs/PS2/game.iso",
                enabled,
                legacy = SystemID.PSP,
            ),
        )
    }
}
