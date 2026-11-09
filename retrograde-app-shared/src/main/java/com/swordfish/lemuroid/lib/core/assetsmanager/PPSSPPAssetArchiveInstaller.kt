package com.swordfish.lemuroid.lib.core.assetsmanager

import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.zip.ZipInputStream

/**
 * Installs core assets without removing the last usable copy.
 *
 * A separate rollback directory is deliberately retained after successful updates.
 * Only the owner-controlled recovery process may retire it.
 */
object PPSSPPAssetArchiveInstaller {
    private const val MAX_EXTRACTED_BYTES = 256L * 1024 * 1024
    private const val MAX_ENTRIES = 20_000
    private const val ROLLBACK_MARKER = ".rollback-"

    fun recoverInterruptedInstallation(destination: File) {
        if (destination.exists()) return
        val parent = destination.parentFile ?: return
        val previous =
            parent.listFiles()
                ?.filter { it.isDirectory && it.name.startsWith(destination.name + ROLLBACK_MARKER) }
                ?.maxByOrNull { it.lastModified() }
                ?: return
        check(previous.renameTo(destination)) { "Unable to recover previous PPSSPP assets" }
    }

    /**
     * Extract to a new directory, validate before replacing, and retain previous assets.
     * Failed downloads or unsafe archive entries leave the old directory unchanged.
     */
    fun install(
        archive: ZipInputStream,
        destination: File,
    ) {
        val parent = requireNotNull(destination.parentFile) { "Missing PPSSPP assets parent" }
        check(parent.isDirectory || parent.mkdirs()) { "Unable to create PPSSPP assets parent" }
        recoverInterruptedInstallation(destination)

        val staging = File(parent, destination.name + ".staging-" + UUID.randomUUID())
        check(staging.mkdir()) { "Unable to stage PPSSPP assets" }

        var previous: File? = null
        try {
            val stagingRoot = staging.canonicalFile
            var extractedBytes = 0L
            var entries = 0
            val buffer = ByteArray(8192)

            while (true) {
                val entry = archive.nextEntry ?: break
                entries++
                check(entries <= MAX_ENTRIES) { "PPSSPP asset archive has too many entries" }
                val relativeName = entry.name
                check(relativeName.isNotBlank() && !relativeName.contains('\\')) {
                    "Invalid PPSSPP asset entry"
                }
                val target = File(staging, relativeName).canonicalFile
                check(target.path.startsWith(stagingRoot.path + File.separator)) {
                    "PPSSPP asset archive attempted to escape staging directory"
                }
                if (entry.isDirectory) {
                    check(target.isDirectory || target.mkdirs()) { "Unable to create asset directory" }
                } else {
                    val targetParent = requireNotNull(target.parentFile)
                    check(targetParent.isDirectory || targetParent.mkdirs()) { "Unable to create asset parent" }
                    target.outputStream().use { output ->
                        while (true) {
                            val read = archive.read(buffer)
                            if (read < 0) break
                            extractedBytes += read
                            check(extractedBytes <= MAX_EXTRACTED_BYTES) {
                                "PPSSPP asset archive exceeds permitted extracted size"
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                }
                archive.closeEntry()
            }

            check(entries > 0 && extractedBytes > 0) { "Empty PPSSPP assets archive" }
            if (destination.exists()) {
                previous = File(parent, destination.name + ROLLBACK_MARKER + UUID.randomUUID())
                check(destination.renameTo(previous)) { "Unable to retain previous PPSSPP assets" }
            }
            if (!staging.renameTo(destination)) {
                previous?.let { old ->
                    check(old.renameTo(destination)) { "Unable to restore previous PPSSPP assets" }
                }
                throw IOException("Unable to activate staged PPSSPP assets")
            }
        } catch (error: Exception) {
            if (!destination.exists()) {
                previous?.let { old ->
                    check(old.renameTo(destination)) { "Unable to restore previous PPSSPP assets" }
                }
            }
            throw error
        } finally {
            staging.deleteRecursively()
        }
    }
}
