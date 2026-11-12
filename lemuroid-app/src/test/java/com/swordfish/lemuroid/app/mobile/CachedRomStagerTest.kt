package com.swordfish.lemuroid.app.mobile

import com.swordfish.lemuroid.lib.storage.local.CachedRomStager
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CachedRomStagerTest {
    @Test
    fun aVerifiedCacheIsReusedWithoutReopeningSource() {
        val folder = Files.createTempDirectory("cartdeck-rom-cache").toFile()
        try {
            val cached = File(folder, "disc.iso")
            CachedRomStager.stage(cached, 3, 100) { ByteArrayInputStream(byteArrayOf(1, 2, 3)) }
            val actual =
                CachedRomStager.stage(cached, 3, 100) {
                    error("Should not reopen a verified cached ROM")
                }
            assertEquals(cached, actual)
            assertArrayEquals(byteArrayOf(1, 2, 3), actual.readBytes())
        } finally {
            folder.deleteRecursively()
        }
    }

    @Test
    fun truncatedCacheIsReplacedOnlyAfterCompleteCopy() {
        val folder = Files.createTempDirectory("cartdeck-rom-cache").toFile()
        try {
            val cached = File(folder, "disc.iso").apply { writeBytes(byteArrayOf(7)) }
            val actual =
                CachedRomStager.stage(cached, 4) {
                    ByteArrayInputStream(byteArrayOf(1, 2, 3, 4))
                }
            assertEquals(4L, actual.length())
            assertArrayEquals(byteArrayOf(1, 2, 3, 4), actual.readBytes())
            assertFalse(folder.listFiles().orEmpty().any { it.extension == "partial" })
        } finally {
            folder.deleteRecursively()
        }
    }

    @Test
    fun interruptedCopyNeverPoisonsFinalCache() {
        val folder = Files.createTempDirectory("cartdeck-rom-cache").toFile()
        try {
            val cached = File(folder, "disc.gcm").apply { writeBytes(byteArrayOf(7)) }
            assertThrows(IOException::class.java) {
                CachedRomStager.stage(cached, 10) {
                    object : InputStream() {
                        private var reads = 0

                        override fun read(): Int {
                            if (reads++ >= 2) throw IOException("Interrupted disc")
                            return 5
                        }
                    }
                }
            }
            assertArrayEquals(byteArrayOf(7), cached.readBytes())
            assertEquals(1, folder.listFiles()?.size)
        } finally {
            folder.deleteRecursively()
        }
    }

    @Test
    fun reportedLengthMismatchCannotBeLaunchedAsComplete() {
        val folder = Files.createTempDirectory("cartdeck-rom-cache").toFile()
        try {
            val cached = File(folder, "disc.iso")
            assertThrows(IOException::class.java) {
                CachedRomStager.stage(cached, 6) {
                    ByteArrayInputStream(byteArrayOf(1, 2))
                }
            }
            assertFalse(cached.exists())
            assertTrue(folder.listFiles().isNullOrEmpty())
        } finally {
            folder.deleteRecursively()
        }
    }

    @Test
    fun unknownContentLengthRequiresARealNonEmptyCopy() {
        val folder = Files.createTempDirectory("cartdeck-rom-cache").toFile()
        try {
            val cached = File(folder, "disc.iso")
            CachedRomStager.stage(cached, 0) { ByteArrayInputStream(byteArrayOf(1, 2)) }
            CachedRomStager.stage(cached, 0) { ByteArrayInputStream(byteArrayOf(3, 4, 5)) }
            assertArrayEquals(byteArrayOf(3, 4, 5), cached.readBytes())
        } finally {
            folder.deleteRecursively()
        }
    }

    @Test
    fun sameSizeReplacementAtSameUriDoesNotReuseOldBytes() {
        val folder = Files.createTempDirectory("cartdeck-rom-cache").toFile()
        try {
            val cached = File(folder, "disc.iso")
            CachedRomStager.stage(cached, 3, 100) { ByteArrayInputStream(byteArrayOf(1, 2, 3)) }
            CachedRomStager.stage(cached, 3, 200) { ByteArrayInputStream(byteArrayOf(4, 5, 6)) }
            assertArrayEquals(byteArrayOf(4, 5, 6), cached.readBytes())
        } finally {
            folder.deleteRecursively()
        }
    }

    @Test
    fun legacySameSizeCacheWithoutSourceMarkerMustBeVerifiedAgain() {
        val folder = Files.createTempDirectory("cartdeck-rom-cache").toFile()
        try {
            val cached = File(folder, "disc.iso").apply { writeBytes(byteArrayOf(1, 2, 3)) }
            CachedRomStager.stage(cached, 3, 100) { ByteArrayInputStream(byteArrayOf(4, 5, 6)) }
            assertArrayEquals(byteArrayOf(4, 5, 6), cached.readBytes())
        } finally {
            folder.deleteRecursively()
        }
    }

    @Test
    fun knownLengthWithoutModificationTimeMustReopenTheSource() {
        val folder = Files.createTempDirectory("cartdeck-rom-cache").toFile()
        try {
            val cached = File(folder, "disc.iso")
            CachedRomStager.stage(cached, 3) { ByteArrayInputStream(byteArrayOf(1, 2, 3)) }
            CachedRomStager.stage(cached, 3) { ByteArrayInputStream(byteArrayOf(4, 5, 6)) }
            assertArrayEquals(byteArrayOf(4, 5, 6), cached.readBytes())
        } finally {
            folder.deleteRecursively()
        }
    }

    @Test
    fun failedUpdatedSourceKeepsPreviousVerifiedCacheAndCanRetry() {
        val folder = Files.createTempDirectory("cartdeck-rom-cache").toFile()
        try {
            val cached = File(folder, "disc.iso")
            CachedRomStager.stage(cached, 3, 100) { ByteArrayInputStream(byteArrayOf(1, 2, 3)) }
            assertThrows(IOException::class.java) {
                CachedRomStager.stage(cached, 3, 200) { ByteArrayInputStream(byteArrayOf(4)) }
            }
            assertArrayEquals(byteArrayOf(1, 2, 3), cached.readBytes())
            CachedRomStager.stage(cached, 3, 200) { ByteArrayInputStream(byteArrayOf(4, 5, 6)) }
            assertArrayEquals(byteArrayOf(4, 5, 6), cached.readBytes())
        } finally {
            folder.deleteRecursively()
        }
    }

    @Test
    fun zipReplacementAndMissingEntryNeverReuseStaleExtractedRom() {
        val folder = Files.createTempDirectory("cartdeck-rom-cache").toFile()
        try {
            val archive = File(folder, "games.zip")
            val cached = File(folder, "game.gba")

            fun writeArchive(
                entryName: String,
                bytes: ByteArray,
            ) {
                ZipOutputStream(archive.outputStream()).use { zip ->
                    zip.putNextEntry(ZipEntry(entryName))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
            writeArchive("game.gba", byteArrayOf(1, 2, 3))
            CachedRomStager.stageZip(cached, "game.gba", archive.length(), 100) { archive.inputStream() }
            writeArchive("game.gba", byteArrayOf(4, 5, 6))
            CachedRomStager.stageZip(cached, "game.gba", archive.length(), 200) { archive.inputStream() }
            assertArrayEquals(byteArrayOf(4, 5, 6), cached.readBytes())
            writeArchive("other.gba", byteArrayOf(7, 8, 9))
            assertThrows(IOException::class.java) {
                CachedRomStager.stageZip(cached, "game.gba", archive.length(), 300) { archive.inputStream() }
            }
            assertArrayEquals(byteArrayOf(4, 5, 6), cached.readBytes())
        } finally {
            folder.deleteRecursively()
        }
    }
}
