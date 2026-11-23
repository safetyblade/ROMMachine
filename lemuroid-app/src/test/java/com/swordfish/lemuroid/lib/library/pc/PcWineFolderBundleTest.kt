package com.swordfish.lemuroid.lib.library.pc

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.util.UUID

class PcWineFolderBundleTest {
    private fun exe(machine: Int = 0x14c): ByteArray =
        ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN).apply {
            putShort(0, 0x5a4d.toShort())
            putInt(60, 64)
            putInt(64, 0x00004550)
            putShort(68, machine.toShort())
        }.array()

    private fun source(path: String, bytes: ByteArray): PcWineFolderBundle.Source =
        PcWineFolderBundle.Source(path) { bytes.inputStream() }

    @Test
    fun folderPreservesNestedDependenciesAndGameModificationsAcrossReopen() {
        val root = Files.createTempDirectory("pc-wine-folder").toFile()
        try {
            val binary = exe()
            val assets = "original music".toByteArray()
            val inputs =
                listOf(
                    source("bin/Game.exe", binary),
                    source("bin/graphics.dll", byteArrayOf(1, 2, 3)),
                    source("data/audio/theme.ogg", assets),
                )
            val cartridge = PcWineFolderBundle.importFolder(root, "Folder test game", inputs, "bin/Game.exe")
            assertTrue(PcWineCartridges.isWine(cartridge))
            assertEquals(cartridge, PcCartridgeStore(root).resolve(cartridge.content))
            val store = PcCartridgeStore(root)
            val workspace = store.workspace(cartridge)
            val executable = PcWineCartridges.prepareExecutable(cartridge, workspace)
            assertArrayEquals(binary, executable.readBytes())
            val content = File(workspace.runtime, "content")
            assertArrayEquals(assets, File(content, "data/audio/theme.ogg").readBytes())
            val save = File(content, "savegame.sav").apply { writeText("Saved level 2") }
            File(content, "data/audio/theme.ogg").writeText("customised")
            assertEquals(executable, PcWineCartridges.prepareExecutable(cartridge, workspace))
            assertEquals("Saved level 2", save.readText())
            assertEquals("customised", File(content, "data/audio/theme.ogg").readText())
            val alternate = store.workspace(cartridge, UUID.randomUUID())
            val alternateExe = PcWineCartridges.prepareExecutable(cartridge, alternate)
            assertNotEquals(executable, alternateExe)
            assertArrayEquals(assets, File(alternate.runtime, "content/data/audio/theme.ogg").readBytes())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectTraversalCaseCollisionsUnsupportedPEAndCancellationWithoutPublishing() {
        val root = Files.createTempDirectory("pc-wine-denial").toFile()
        try {
            val valid = source("game.exe", exe())
            val invalids =
                listOf(
                    listOf(valid, source("../escape.dll", byteArrayOf(1))),
                    listOf(valid, source("GAME.EXE", exe())),
                    listOf(valid, source("CON.DLL", byteArrayOf(1))),
                    listOf(source("game.exe", exe(0x8664))),
                )
            for (bad in invalids) {
                assertTrue(
                    runCatching {
                        PcWineFolderBundle.importFolder(root, "Invalid game", bad, "game.exe")
                    }.isFailure,
                )
            }
            assertTrue(
                runCatching {
                    PcWineFolderBundle.importFolder(root, "Cancelled", listOf(valid), "game.exe") {
                        error("cancelled")
                    }
                }.isFailure,
            )
            assertTrue(PcCartridgeStore(root).entries().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }
}
