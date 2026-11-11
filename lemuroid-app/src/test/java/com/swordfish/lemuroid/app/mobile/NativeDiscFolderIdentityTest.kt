package com.swordfish.lemuroid.app.mobile

import com.swordfish.lemuroid.lib.library.NativeGameSystems
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.storage.scanner.NativeDiscFolderIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NativeDiscFolderIdentityTest {
    private val enabled = setOf(SystemID.DREAMCAST, SystemID.GAMECUBE, SystemID.PLAYSTATION_2)

    @Test
    fun uniqueNativeExtensionsAreRegisteredOnlyWhenUnambiguous() {
        val configurations = NativeGameSystems.configurations().associateBy { it.id }
        assertEquals(listOf("gdi", "cdi"), configurations.getValue(SystemID.DREAMCAST).uniqueExtensions)
        assertEquals(listOf("gcm"), configurations.getValue(SystemID.GAMECUBE).uniqueExtensions)
        assertEquals(emptyList<String>(), configurations.getValue(SystemID.PLAYSTATION_2).uniqueExtensions)
    }

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
    fun ownerPs2RomFoldersAndNestedGameNamesAreRecognised() {
        assertEquals(
            SystemID.PLAYSTATION_2,
            NativeDiscFolderIdentity.infer(
                "Shadow of the Colossus (USA).iso",
                "/storage/emulated/0/ROMS/PS2 ROM/Shadow of the Colossus (USA)/Shadow of the Colossus (USA).iso",
                enabled,
            ),
        )
        assertEquals(
            SystemID.PLAYSTATION_2,
            NativeDiscFolderIdentity.infer(
                "Dragon Ball Z - Budokai Tenkaichi 3 (USA).iso",
                "/tree/primary%3AROMS/document/primary%3AROMS%2FPS2%20ROM%2FDragon%20Ball%20Z%20-%20Budokai%20Tenkaichi%203%20(USA)%2FDragon%20Ball%20Z%20-%20Budokai%20Tenkaichi%203%20(USA).iso",
                enabled,
            ),
        )
        assertNull(
            NativeDiscFolderIdentity.infer(
                "Dragon Ball Z - Buu's Fury.gbc",
                "/ROMS/PS2 ROM/Dragon Ball Z - Buu's Fury/Dragon Ball Z - Buu's Fury.gbc",
                enabled,
            ),
        )
    }

    @Test
    fun folderQualifiesCoreAdvertisedCompressedDiscFormats() {
        assertEquals(
            SystemID.DREAMCAST,
            NativeDiscFolderIdentity.infer("homebrew.cdi", "/ROMs/Dreamcast/homebrew.cdi", enabled),
        )
        assertEquals(
            SystemID.GAMECUBE,
            NativeDiscFolderIdentity.infer("disc.rvz", "/ROMs/GameCube/disc.rvz", enabled),
        )
        assertEquals(
            SystemID.GAMECUBE,
            NativeDiscFolderIdentity.infer("disc.gcz", "/ROMs/GameCube/disc.gcz", enabled),
        )
        assertEquals(
            SystemID.PLAYSTATION_2,
            NativeDiscFolderIdentity.infer("disc.chd", "/ROMs/PS2/disc.chd", enabled),
        )
        assertEquals(
            SystemID.PLAYSTATION_2,
            NativeDiscFolderIdentity.infer("disc.cso", "/ROMs/PS2/disc.cso", enabled),
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
        assertNull(NativeDiscFolderIdentity.infer("game.rvz", "/ROMs/game.rvz", enabled))
        assertNull(NativeDiscFolderIdentity.infer("game.cdi", "/ROMs/game.cdi", enabled))
        assertNull(NativeDiscFolderIdentity.infer("game.chd", "/ROMs/game.chd", enabled))
        assertNull(NativeDiscFolderIdentity.infer("game.cso", "/ROMs/game.cso", enabled))
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
