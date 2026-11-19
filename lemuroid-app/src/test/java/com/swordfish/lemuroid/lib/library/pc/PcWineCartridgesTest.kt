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

class PcWineCartridgesTest {
    private fun executable(machine: Int): ByteArray =
        ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN).apply {
            putShort(0, 0x5a4d.toShort())
            putInt(60, 64)
            putInt(64, 0x00004550)
            putShort(68, machine.toShort())
        }.array()

    @Test
    fun importedExeHasCanonicalIdentityAndNonDestructiveProfileWorkspaces() {
        val root = Files.createTempDirectory("wine-cartridge").toFile()
        try {
            val bytes = executable(0x014c)
            val entry = PcWineCartridges.importExecutable(root, "Wine game", bytes.inputStream())
            val store = PcCartridgeStore(root)
            assertEquals(entry, store.resolve(entry.content))
            assertTrue(PcWineCartridges.isWine(entry))
            val workspace = store.workspace(entry)
            val target = PcWineCartridges.prepareExecutable(entry, workspace)
            assertArrayEquals(bytes, target.readBytes())
            val save = File(target.parentFile, "save.txt").apply { writeText("retained") }
            assertEquals(target, PcWineCartridges.prepareExecutable(entry, workspace))
            assertEquals("retained", save.readText())
            assertNotEquals(
                target,
                PcWineCartridges.prepareExecutable(entry, store.workspace(entry, UUID.randomUUID())),
            )
            target.writeBytes(byteArrayOf(1, 2, 3))
            assertTrue(runCatching { PcWineCartridges.prepareExecutable(entry, workspace) }.isFailure)
            assertArrayEquals(byteArrayOf(1, 2, 3), target.readBytes())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsUnsupportedAndCancelledImports() {
        val root = Files.createTempDirectory("wine-cartridge").toFile()
        try {
            assertTrue(
                runCatching {
                    PcWineCartridges.importExecutable(root, "PE64", executable(0x8664).inputStream())
                }.isFailure,
            )
            assertTrue(
                runCatching {
                    PcWineCartridges.importExecutable(root, "cancel", executable(0x014c).inputStream()) {
                        error("cancel")
                    }
                }.isFailure,
            )
            assertTrue(PcCartridgeStore(root).entries().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }
}
