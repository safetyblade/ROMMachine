package com.swordfish.lemuroid.lib.storage.cache

import java.io.File
import java.nio.file.Files

/** Only explicit, reconstructible media roots are eligible. Caller holds the game-process lock. */
object RomCachePruner {
    data class Root(val label: String, val directory: File, val groupDepth: Int)

    private data class Group(val directory: File, val bytes: Long, val newest: Long)

    data class Result(val beforeBytes: Long, val remainingBytes: Long, val removedGroups: Int)

    fun roots(
        cacheDir: File,
        filesDir: File,
    ): List<Root> {
        val cache = cacheDir.canonicalFile
        val files = filesDir.canonicalFile
        return listOf(
            Root("SAF ROM copies", File(cache, "storage-framework-games"), 2),
            Root("Local archive copies", File(cache, "local-storage-games"), 2),
            Root("Wii U media", File(cache, "native/wiiu"), 2),
            Root("ARMSX2 media", File(cache, "native/ps2/armsx2"), 1),
            Root("Xbox 360 media", File(files, "native/xbox360/staging"), 1),
            Root("PS3 ARMSX3 media", File(files, "native/ps3/armsx3/content"), 1),
            Root("PS3 RPCS3 media", File(files, "native/ps3/rpcs3-libretro/content"), 1),
        )
    }

    private fun safe(file: File): Boolean =
        runCatching { file.canonicalFile == file.absoluteFile }.getOrDefault(false)

    private fun groups(
        root: Root,
        checkCancelled: () -> Unit,
    ): List<File> {
        fun collect(
            directory: File,
            depth: Int,
        ): List<File> {
            checkCancelled()
            if (!safe(directory) || !directory.isDirectory) return emptyList()
            val children = directory.listFiles() ?: return emptyList()
            if (children.isEmpty()) return emptyList()
            // Legacy layouts may have a whole disc family directly under a system directory.
            if (depth == 0 || children.any { !it.isDirectory }) return listOf(directory)
            return children.flatMap { collect(it, depth - 1) }
        }
        return collect(root.directory, root.groupDepth)
    }

    private fun inspect(
        directory: File,
        checkCancelled: () -> Unit,
    ): Group? {
        if (!safe(directory)) return null
        var bytes = 0L
        var newest = 0L
        fun visit(file: File): Boolean {
            checkCancelled()
            if (Files.isSymbolicLink(file.toPath())) return false
            newest = maxOf(newest, file.lastModified())
            if (file.isDirectory) {
                val children = file.listFiles() ?: return false
                return children.all(::visit)
            }
            if (!file.isFile) return false
            bytes += file.length()
            return true
        }
        return if (visit(directory)) Group(directory, bytes, newest) else null
    }

    fun reclaimableBytes(
        root: Root,
        checkCancelled: () -> Unit = {},
    ): Long = groups(root, checkCancelled).sumOf { inspect(it, checkCancelled)?.bytes ?: 0L }

    fun prune(
        cacheDir: File,
        filesDir: File,
        limitBytes: Long,
        checkCancelled: () -> Unit = {},
    ): Result {
        require(limitBytes >= 0)
        val candidates =
            roots(cacheDir, filesDir).flatMap { groups(it, checkCancelled) }
                .mapNotNull { inspect(it, checkCancelled) }
                .sortedWith(compareBy<Group> { it.newest }.thenBy { it.directory.path })
        val before = candidates.sumOf { it.bytes }
        var remaining = before
        var removed = 0
        for (group in candidates) {
            checkCancelled()
            if (remaining <= limitBytes && limitBytes != 0L) break
            // Delete complete disc families, never individual tracks or active media.
            if (inspect(group.directory, checkCancelled) != null && group.directory.deleteRecursively()) {
                remaining -= group.bytes
                removed++
            }
        }
        // A partial filesystem failure must not be reported as successful reclamation.
        val actual = roots(cacheDir, filesDir).sumOf { reclaimableBytes(it, checkCancelled) }
        return Result(before, actual, removed)
    }
}
