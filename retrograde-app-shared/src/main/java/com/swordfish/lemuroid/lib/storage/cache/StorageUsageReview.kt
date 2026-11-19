package com.swordfish.lemuroid.lib.storage.cache

import java.io.File

/** Logical file bytes, not Android's allocated-storage accounting. Read-only and app-private. */
object StorageUsageReview {
    data class Row(val label: String, val bytes: Long, val reclaimableBytes: Long, val internal: Boolean)

    data class Report(val rows: List<Row>, val incompleteEntries: Int) {
        val internalBytes: Long get() = rows.filter { it.internal }.sumOf { it.bytes }
        val externalBytes: Long get() = rows.filterNot { it.internal }.sumOf { it.bytes }
        val reclaimableBytes: Long get() = rows.sumOf { it.reclaimableBytes }
    }

    fun inspect(
        dataDir: File,
        cacheDir: File,
        filesDir: File,
        externalDir: File?,
        checkCancelled: () -> Unit = {},
    ): Report {
        val data = dataDir.canonicalFile
        val managed = RomCachePruner.roots(cacheDir, filesDir)
        val nativeRoot = File(filesDir.canonicalFile, "native")
        val rows = mutableListOf<Row>()
        var incomplete = 0

        fun entries(directory: File): List<File> {
            val children = directory.listFiles()
            if (children == null) incomplete++
            return children.orEmpty().toList()
        }

        fun safe(file: File): Boolean = runCatching { file.canonicalFile == file.absoluteFile }.getOrDefault(false)

        fun within(
            file: File,
            directory: File,
        ): Boolean = file == directory || file.path.startsWith(directory.path + File.separator)

        fun size(file: File): Long {
            checkCancelled()
            if (!safe(file)) {
                incomplete++
                return 0L
            }
            return when {
                file.isDirectory -> entries(file).sumOf { size(it) }
                file.isFile -> file.length()
                else -> {
                    incomplete++
                    0L
                }
            }
        }

        fun visit(
            file: File,
            label: String,
            internal: Boolean,
            depth: Int,
        ) {
            checkCancelled()
            if (!safe(file)) {
                incomplete++
                return
            }
            val media = managed.firstOrNull { it.directory == file }
            if (media != null) {
                val bytes = size(file)
                val reclaimable = RomCachePruner.reclaimableBytes(media, checkCancelled).coerceAtMost(bytes)
                if (bytes > 0) rows += Row(media.label, bytes, reclaimable, internal)
                return
            }
            val containsManaged = managed.any { within(it.directory, file) }
            // Break down native console data by engine or immediate storage purpose.
            val native = within(file, nativeRoot)
            if (file.isDirectory && (containsManaged || depth < 2 || (native && depth < 5))) {
                for (child in entries(file)) visit(child, "$label/${child.name}", internal, depth + 1)
            } else {
                val bytes = size(file)
                if (bytes > 0) rows += Row(label, bytes, 0, internal)
            }
        }

        visit(data, "Internal", true, 0)
        externalDir?.canonicalFile?.takeUnless { within(it, data) }?.let {
            if (it.isDirectory) visit(it, "App-specific storage", false, 0)
        }
        return Report(rows.sortedByDescending { it.bytes }, incomplete)
    }
}
