package com.swordfish.lemuroid.lib.storage.local

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Atomically stages a ContentResolver ROM for cores that require a real file.
 *
 * The destination is a disposable game cache, never a save file or original ROM.
 * An interrupted copy must never be passed to an emulator on the next launch.
 */
object CachedRomStager {
    fun stage(
        destination: File,
        expectedBytes: Long,
        openSource: () -> InputStream,
    ): File {
        require(expectedBytes >= 0) { "Invalid reported ROM size" }
        if (expectedBytes > 0 && destination.isFile && destination.length() == expectedBytes) {
            return destination
        }

        val directory = destination.parentFile ?: throw IOException("Missing ROM staging directory")
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("Cannot create ROM staging directory")
        }

        val freeBytes = directory.usableSpace
        if (expectedBytes > 0 && freeBytes > 0 && freeBytes < expectedBytes) {
            throw IOException("Insufficient free space to stage ROM content")
        }

        val pending = File.createTempFile("cartdeck-rom-", ".partial", directory)
        try {
            val copiedBytes =
                openSource().use { input ->
                    FileOutputStream(pending).use { output ->
                        input.copyTo(output)
                    }
                }
            if (copiedBytes <= 0) throw IOException("ROM file is empty")
            if (expectedBytes > 0 && copiedBytes != expectedBytes) {
                throw IOException("Incomplete ROM copy; cached game was not replaced")
            }

            // Only a fully copied cache can replace an older incomplete entry.
            // A failed rename does not delete the original ROM or any save.
            if (destination.exists() && !destination.delete()) {
                throw IOException("Cannot replace incomplete cached ROM")
            }
            if (!pending.renameTo(destination)) {
                throw IOException("Cannot finalise cached ROM")
            }
            return destination
        } finally {
            pending.delete()
        }
    }
}
