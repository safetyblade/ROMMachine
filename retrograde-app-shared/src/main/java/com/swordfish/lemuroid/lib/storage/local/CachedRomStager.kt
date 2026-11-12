package com.swordfish.lemuroid.lib.storage.local

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/** Atomically stages disposable ROM caches; original games and saves are never modified. */
object CachedRomStager {
    @Synchronized
    fun stage(
        destination: File,
        expectedBytes: Long,
        expectedModifiedAt: Long = 0,
        sourceBytes: Long = expectedBytes,
        openSource: () -> InputStream,
    ): File {
        require(expectedBytes >= 0 && expectedModifiedAt >= 0 && sourceBytes >= 0) { "Invalid reported ROM metadata" }
        val directory = destination.parentFile ?: throw IOException("Missing ROM staging directory")
        val key =
            MessageDigest.getInstance("SHA-256").digest(destination.name.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val marker = File(directory, ".cartdeck-$key.source")

        fun fingerprint(length: Long) = "$sourceBytes:$expectedModifiedAt:$expectedBytes:$length\n"
        val knownSourceVersion = sourceBytes > 0 && expectedModifiedAt > 0
        if (knownSourceVersion && destination.isFile && destination.length() > 0 &&
            (expectedBytes == 0L || destination.length() == expectedBytes) &&
            runCatching { marker.readText() == fingerprint(destination.length()) }.getOrDefault(false)
        ) {
            return destination
        }

        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create ROM staging directory")
        val freeBytes = directory.usableSpace
        if (expectedBytes > 0 && freeBytes > 0 && freeBytes < expectedBytes) {
            throw IOException("Insufficient free space to stage ROM content")
        }

        val pending = File.createTempFile("cartdeck-rom-", ".partial", directory)
        val pendingMarker = File.createTempFile("cartdeck-source-", ".partial", directory)
        try {
            val copiedBytes =
                openSource().use { input ->
                    FileOutputStream(pending).use { output ->
                        val copied = input.copyTo(output)
                        output.fd.sync()
                        copied
                    }
                }
            if (copiedBytes <= 0) throw IOException("ROM file is empty")
            if (expectedBytes > 0 && copiedBytes != expectedBytes) {
                throw IOException("Incomplete ROM copy; cached game was not replaced")
            }
            FileOutputStream(pendingMarker).use { output ->
                output.write(fingerprint(copiedBytes).toByteArray(Charsets.UTF_8))
                output.fd.sync()
            }
            // Publish the marker last. If publication is interrupted, an absent marker
            // forces a fresh copy instead of certifying new bytes with old metadata.
            if (marker.exists() && !marker.delete()) throw IOException("Cannot invalidate ROM source metadata")
            if (!pending.renameTo(destination)) throw IOException("Cannot finalise cached ROM")
            if (!pendingMarker.renameTo(marker)) throw IOException("Cannot finalise ROM source metadata")
            return destination
        } finally {
            pending.delete()
            pendingMarker.delete()
        }
    }

    fun stageZip(
        destination: File,
        entryName: String,
        sourceBytes: Long,
        sourceModifiedAt: Long,
        openArchive: () -> InputStream,
    ): File =
        stage(destination, 0, sourceModifiedAt, sourceBytes) {
            val archive = ZipInputStream(openArchive())
            try {
                while (true) {
                    val entry = archive.nextEntry ?: throw IOException("Selected ROM is no longer in its ZIP archive")
                    if (!entry.isDirectory && entry.name == entryName) break
                }
                archive
            } catch (error: Exception) {
                archive.close()
                throw error
            }
        }
}
