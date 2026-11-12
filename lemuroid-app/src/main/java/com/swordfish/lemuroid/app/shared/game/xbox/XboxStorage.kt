package com.swordfish.lemuroid.app.shared.game.xbox

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Persistent console identity/HDD never lives in a disposable engine-version folder. */
object XboxStorage {
    fun prepare(
        filesDir: File,
        revision: String,
    ): File {
        require(revision.matches(Regex("[0-9a-f]{40}(-[0-9a-f]{12})?")))
        val root = File(filesDir, "native/xbox")
        check(root.isDirectory || root.mkdirs()) { "Cannot open Xbox data storage" }
        val hdd = File(root, "hdd")
        check(hdd.isDirectory || hdd.mkdirs()) { "Cannot open Xbox virtual HDD" }
        val marker = File(root, "engine-revision")
        val previous = marker.takeIf { it.isFile }?.readText()?.trim()
        if (previous != revision && hdd.walkTopDown().any { it.isFile }) {
            // An upgrade snapshots the complete console before a new engine opens it.
            // Failure leaves the old revision and original HDD intact and blocks launch.
            val backup = File(root, "backups").apply { check(isDirectory || mkdirs()) }
            val safePrevious = previous?.takeIf { it.matches(Regex("[0-9a-f]{40}(-[0-9a-f]{12})?")) } ?: "adoption"
            val target =
                File(
                    backup,
                    "before-$safePrevious-to-$revision-${java.util.UUID.randomUUID()}.zip",
                )
            if (!target.exists()) {
                val tmp = File(backup, target.name + ".tmp")
                try {
                    val bytes = hdd.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                    check(root.usableSpace > bytes + 64L * 1024 * 1024) {
                        "Not enough free space to protect Xbox saves before this engine upgrade"
                    }
                    FileOutputStream(tmp).use { output ->
                        ZipOutputStream(output).use { zip ->
                            hdd.walkTopDown().filter { it.isFile }.forEach { file ->
                                check(
                                    !java.nio.file.Files.isSymbolicLink(file.toPath()),
                                ) { "Unexpected link in Xbox data" }
                                zip.putNextEntry(ZipEntry(file.relativeTo(hdd).invariantSeparatorsPath))
                                file.inputStream().use { it.copyTo(zip) }
                                zip.closeEntry()
                            }
                        }
                    }
                    check(tmp.renameTo(target)) { "Cannot finish Xbox save backup" }
                } finally {
                    tmp.delete()
                }
            }
        }
        val pending = File(root, "engine-revision.tmp")
        pending.writeText(revision)
        check(pending.renameTo(marker)) { "Cannot record Xbox engine version" }
        return hdd
    }
}
