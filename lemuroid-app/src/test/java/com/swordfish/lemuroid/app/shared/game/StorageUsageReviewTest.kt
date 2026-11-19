package com.swordfish.lemuroid.app.shared.game

import com.swordfish.lemuroid.lib.storage.cache.StorageUsageReview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class StorageUsageReviewTest {
    @Test
    fun countsDisjointAppStorageAndIdentifiesOnlyDisposableCopies() {
        val root = Files.createTempDirectory("cartdeck-storage-review").toFile()
        try {
            val data = File(root, "data")
            val cache = File(data, "cache")
            val files = File(data, "files")
            val external = File(root, "external")

            fun file(
                directory: File,
                path: String,
                bytes: Int,
            ) {
                File(directory, path).apply {
                    check(parentFile!!.isDirectory || parentFile!!.mkdirs())
                    writeBytes(ByteArray(bytes))
                }
            }
            file(cache, "native/wiiu/id/source/disc.wua", 100)
            file(files, "native/xbox360/staging/key/game.iso", 200)
            file(files, "native/xbox360/content/save.dat", 30)
            file(files, "native-engines/xbox360/core.so", 40)
            file(data, "databases/library.db", 10)
            file(external, "saves/progress.sav", 20)
            val report = StorageUsageReview.inspect(data, cache, files, external)
            assertEquals(380L, report.internalBytes)
            assertEquals(20L, report.externalBytes)
            assertEquals(300L, report.reclaimableBytes)
            assertEquals(0, report.incompleteEntries)
            assertTrue(report.rows.any { it.label == "Xbox 360 media" && it.reclaimableBytes == 200L })
        } finally {
            root.deleteRecursively()
        }
    }
}
