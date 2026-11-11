package com.swordfish.lemuroid.lib.saves

import java.io.File
import java.security.MessageDigest
import java.util.UUID

/** Copies standard PSP savedata once, without importing engine-specific states or modifying the fallback. */
object PSPSavedataSeed {
    fun seedIfAbsent(
        legacySaveRoot: File,
        modernSaveRoot: File,
    ) {
        val source = File(legacySaveRoot, "PSP/SAVEDATA")
        val destination = File(modernSaveRoot, "PSP/SAVEDATA")
        if (destination.exists() || !source.isDirectory) return
        val parent = requireNotNull(destination.parentFile)
        check(parent.isDirectory || parent.mkdirs()) { "Unable to create PSP savedata directory" }
        val staging = File(parent, ".savedata-staging-${UUID.randomUUID()}")
        check(staging.mkdir()) { "Unable to stage PSP savedata" }
        try {
            val root = source.canonicalFile
            source.walkTopDown().forEach { original ->
                val relative = original.relativeTo(source).path
                check(
                    original.canonicalFile == File(root, relative).absoluteFile,
                ) { "PSP savedata symlinks are not supported" }
                val copy = File(staging, relative)
                if (original.isDirectory) {
                    check(copy.isDirectory || copy.mkdirs()) { "Unable to stage PSP savedata directory" }
                } else {
                    original.copyTo(copy, overwrite = false)
                    check(digest(original).contentEquals(digest(copy))) { "PSP savedata copy verification failed" }
                }
            }
            // Never merge into or replace candidate progress, including a concurrent first launch.
            check(!destination.exists()) { "Candidate PSP savedata already exists" }
            check(staging.renameTo(destination)) { "Unable to activate copied PSP savedata" }
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun digest(file: File): ByteArray {
        val result = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                result.update(buffer, 0, count)
            }
        }
        return result.digest()
    }
}
