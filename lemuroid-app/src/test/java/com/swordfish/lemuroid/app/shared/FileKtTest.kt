package com.swordfish.lemuroid.app.shared

import com.swordfish.lemuroid.common.kotlin.calculateMd5
import com.swordfish.lemuroid.common.kotlin.extractEntryToFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class FileKtTest {
    @Test
    fun calculateMd5MatchesKnownDigest() {
        val file = Files.createTempFile("md5-known-", ".bin").toFile()
        try {
            file.writeBytes("abc".toByteArray())
            assertEquals("900150983cd24fb0d6963f7d28e17f72", file.calculateMd5())
        } finally {
            file.delete()
        }
    }

    @Test
    fun calculateMd5MatchesWholeFileDigestForLargeContent() {
        val bytes = ByteArray(3 * 1024 * 1024) { (it % 251).toByte() }
        val file = Files.createTempFile("md5-large-", ".bin").toFile()
        try {
            file.writeBytes(bytes)
            val expected = MessageDigest.getInstance("MD5").digest(bytes).toHexString()
            assertEquals(expected, file.calculateMd5())
        } finally {
            file.delete()
        }
    }

    @Test
    fun extractEntryToFileExtractsNamedEntry() {
        val parent = Files.createTempDirectory("zip-entry-").toFile()
        try {
            val target = File(parent, "game.bin")
            archive("other.txt" to "other", "game.bin" to "game-bytes").use {
                it.extractEntryToFile("game.bin", target)
            }
            assertEquals("game-bytes", target.readText())
        } finally {
            parent.deleteRecursively()
        }
    }

    @Test
    fun extractEntryToFileThrowsIOExceptionForMissingEntry() {
        val parent = Files.createTempDirectory("zip-missing-").toFile()
        try {
            val target = File(parent, "game.bin")
            assertThrows(IOException::class.java) {
                archive("other.txt" to "other").use {
                    it.extractEntryToFile("game.bin", target)
                }
            }
        } finally {
            parent.deleteRecursively()
        }
    }

    @Test
    fun extractEntryToFileThrowsIOExceptionForEmptyArchive() {
        val parent = Files.createTempDirectory("zip-empty-").toFile()
        try {
            val target = File(parent, "game.bin")
            assertThrows(IOException::class.java) {
                ZipInputStream(ByteArrayInputStream(ByteArray(0))).use {
                    it.extractEntryToFile("game.bin", target)
                }
            }
        } finally {
            parent.deleteRecursively()
        }
    }

    private fun archive(vararg entries: Pair<String, String>): ZipInputStream {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            entries.forEach { (name, contents) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(contents.toByteArray())
                zip.closeEntry()
            }
        }
        return ZipInputStream(ByteArrayInputStream(bytes.toByteArray()))
    }
}
