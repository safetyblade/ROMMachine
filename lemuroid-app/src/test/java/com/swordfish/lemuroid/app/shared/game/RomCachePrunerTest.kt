package com.swordfish.lemuroid.app.shared.game

import com.swordfish.lemuroid.lib.storage.cache.RomCachePruner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class RomCachePrunerTest {
    private fun sample(
        root: File,
        path: String,
        bytes: Int,
        modified: Long,
    ): File {
        val file = File(root, path)
        check(file.parentFile!!.isDirectory || file.parentFile!!.mkdirs())
        file.writeBytes(ByteArray(bytes))
        file.setLastModified(modified)
        var parent = file.parentFile
        while (parent != null && parent != root) {
            parent.setLastModified(modified)
            parent = parent.parentFile
        }
        return file
    }

    @Test
    fun removesCompleteOldDiscFamilyAcrossNativeAndStandardRoots() {
        val root = Files.createTempDirectory("cartdeck-rom-prune").toFile()
        try {
            val cache = File(root, "cache")
            val files = File(root, "files")
            val cue = sample(cache, "storage-framework-games/psx/old/game.cue", 10, 1000)
            val bin = sample(cache, "storage-framework-games/psx/old/track.bin", 60, 1000)
            val wiiu = sample(cache, "native/wiiu/cartridge/source/disc.wua", 80, 2000)
            val xbox = sample(files, "native/xbox360/staging/key/game.iso", 100, 3000)
            val save = sample(files, "native/xbox360/content/profile/save.dat", 100, 1000)
            val shaders = sample(files, "native/xbox360/cache/shader.bin", 100, 1000)
            val result = RomCachePruner.prune(cache, files, 180)
            assertEquals(250L, result.beforeBytes)
            assertEquals(180L, result.remainingBytes)
            assertFalse(cue.exists())
            assertFalse(bin.exists())
            listOf(wiiu, xbox, save, shaders).forEach { assertTrue(it.exists()) }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun reloadModeClearsOnlyKnownStagingRoots() {
        val root = Files.createTempDirectory("cartdeck-rom-clear").toFile()
        try {
            val cache = File(root, "cache")
            val files = File(root, "files")
            val ps2 = sample(cache, "native/ps2/armsx2/id/disc.chd", 10, 1000)
            val ps3 = sample(files, "native/ps3/rpcs3-libretro/content/id/disc.iso", 10, 1000)
            val save = sample(files, "native/ps3/rpcs3-libretro/system/rpcs3/dev_hdd0/home/save", 10, 1000)
            val backup = sample(files, "native/ps3/rpcs3-libretro/backups/save", 10, 1000)
            val art = sample(cache, "artwork/image.png", 10, 1000)
            val original = sample(files, "roms/game.iso", 10, 1000)
            val result = RomCachePruner.prune(cache, files, 0)
            assertEquals(0L, result.remainingBytes)
            assertFalse(ps2.exists())
            assertFalse(ps3.exists())
            listOf(save, backup, art, original).forEach { assertTrue(it.exists()) }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun symlinkNeverAllowsDeletionOutsideStaging() {
        val root = Files.createTempDirectory("cartdeck-rom-link").toFile()
        try {
            val cache = File(root, "cache")
            val files = File(root, "files")
            val save = sample(files, "saves/progress", 10, 1000)
            val staged = sample(cache, "native/wiiu/id/source/disc.wua", 10, 1000)
            val link = File(staged.parentFile, "unsafe")
            Files.createSymbolicLink(link.toPath(), save.parentFile.toPath())
            try {
                RomCachePruner.prune(cache, files, 0)
                assertTrue(save.exists())
                assertTrue(staged.exists())
            } finally {
                Files.deleteIfExists(link.toPath())
            }
        } finally {
            root.deleteRecursively()
        }
    }
}
