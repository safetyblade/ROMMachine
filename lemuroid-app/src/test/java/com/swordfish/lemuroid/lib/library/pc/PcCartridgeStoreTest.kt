package com.swordfish.lemuroid.lib.library.pc

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipFile

class PcCartridgeStoreTest {
    private fun iso(): ByteArray =
        ByteArray(40000).apply { "CD001".toByteArray(Charsets.US_ASCII).copyInto(this, 32769) }

    @Test
    fun explicitImportCreatesImmutableArchiveAndIsolatedGuest() {
        val root = Files.createTempDirectory("pc-cartridge").toFile()
        try {
            val store = PcCartridgeStore(root)
            val original = iso()
            val first = store.importIso("Shandalar", original.inputStream(), original.inputStream())
            val second = store.importIso("Shandalar", original.inputStream())
            assertEquals(setOf(first.id, second.id), store.entries().map { it.id }.toSet())
            assertEquals(first, store.resolve(first.content))
            assertNotEquals(first.id, second.id)
            assertNotEquals(store.workspace(first).guest, store.workspace(second).guest)
            ZipFile(first.content).use { zip ->
                assertEquals(listOf("00-windows.iso", "01-game.iso"), zip.entries().toList().map { it.name })
                assertArrayEquals(original, zip.getInputStream(zip.getEntry("01-game.iso")).use { it.readBytes() })
            }
            assertNull(store.resolve(File(root, "outside.iso")))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun invalidMediaAndCancelledImportNeverPublish() {
        val root = Files.createTempDirectory("pc-cartridge").toFile()
        try {
            val store = PcCartridgeStore(root)
            assertTrue(runCatching { store.importIso("Invalid", ByteArray(40000).inputStream()) }.isFailure)
            assertTrue(runCatching { store.importIso("Cancelled", iso().inputStream()) { error("cancel") } }.isFailure)
            assertTrue(store.entries().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }
}
