package com.swordfish.lemuroid.lib.saves

import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.UUID

/**
 * One-time, read-only snapshot of the previous engine's player data before
 * an opt-in alternate engine can start. Never replaces or moves source data.
 */
object EmulatorMigrationBackup {
    data class Source(val original: File, val relativePath: String)

    fun preserve(
        backupDirectory: File,
        sources: List<Source>,
    ): File {
        val marker = File(backupDirectory, ".complete")
        if (marker.isFile) return backupDirectory
        check(!backupDirectory.exists()) {
            "An incomplete emulator backup exists. Preserve it and use the old engine."
        }

        val parent = requireNotNull(backupDirectory.parentFile)
        check(parent.isDirectory || parent.mkdirs()) { "Could not create emulator backup parent" }
        val staging = File(parent, backupDirectory.name + ".staging-" + UUID.randomUUID())
        check(staging.mkdir()) { "Could not create emulator backup staging directory" }
        try {
            val destinationRoot = staging.canonicalFile
            val manifest = StringBuilder()
            var copiedFiles = 0

            for (source in sources) {
                val original = source.original
                if (!original.exists()) continue
                check(!source.relativePath.startsWith("/") && !source.relativePath.contains("..")) {
                    "Invalid emulator backup path"
                }
                val targetRoot = File(staging, source.relativePath).canonicalFile
                check(targetRoot.path.startsWith(destinationRoot.path + File.separator)) {
                    "Emulator backup attempted to leave staging root"
                }
                check(!Files.isSymbolicLink(original.toPath())) {
                    "Cannot safely back up a symbolic link"
                }
                val files =
                    if (original.isDirectory) {
                        original.walkTopDown().filter { it.isFile }.toList()
                    } else {
                        listOf(original)
                    }

                for (file in files) {
                    check(!Files.isSymbolicLink(file.toPath())) {
                        "Cannot safely back up a symbolic link"
                    }
                    val relative = if (original.isDirectory) file.relativeTo(original).path else ""
                    val destination = if (relative.isEmpty()) targetRoot else File(targetRoot, relative).canonicalFile
                    check(
                        destination.path.startsWith(destinationRoot.path + File.separator) ||
                            destination == targetRoot,
                    ) {
                        "Emulator backup destination escaped the allowed directory"
                    }
                    val destParent = requireNotNull(destination.parentFile)
                    check(destParent.isDirectory || destParent.mkdirs()) {
                        "Could not create backup subdirectory"
                    }
                    val digest = MessageDigest.getInstance("SHA-256")
                    file.inputStream().use { input ->
                        destination.outputStream().use { output ->
                            val buffer = ByteArray(32 * 1024)
                            while (true) {
                                val length = input.read(buffer)
                                if (length < 0) break
                                digest.update(buffer, 0, length)
                                output.write(buffer, 0, length)
                            }
                        }
                    }
                    val hash = digest.digest().joinToString("") { "%02x".format(it) }
                    manifest.append(hash).append("  ")
                        .append(destination.relativeTo(staging).invariantSeparatorsPath)
                        .append('\n')
                    copiedFiles++
                }
            }

            // An empty source set is a valid first-time player with no saves.
            File(staging, ".complete").writeText("files=$copiedFiles\n$manifest")
            check(staging.renameTo(backupDirectory)) {
                "Could not commit emulator baseline backup"
            }
            return backupDirectory
        } catch (error: Exception) {
            staging.deleteRecursively()
            throw error
        }
    }
}
