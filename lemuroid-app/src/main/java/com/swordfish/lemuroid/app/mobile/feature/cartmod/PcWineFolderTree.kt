package com.swordfish.lemuroid.app.mobile.feature.cartmod

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.swordfish.lemuroid.lib.library.pc.PcWineFolderBundle

/** Read-only SAF snapshot; all files are copied before publishing the cartridge. */
internal object PcWineFolderTree {
    data class Selection(
        val title: String,
        val files: List<PcWineFolderBundle.Source>,
        val exePaths: List<String>,
    )

    fun scan(
        context: Context,
        uri: Uri,
    ): Selection {
        val root = checkNotNull(DocumentFile.fromTreeUri(context, uri)) { "Folder not accessible" }
        require(root.isDirectory)
        val candidates = mutableListOf<PcWineFolderBundle.Source>()
        val visited = mutableSetOf<Uri>()
        var nodes = 0

        fun walk(
            directory: DocumentFile,
            prefix: String,
            depth: Int,
        ) {
            check(depth <= 12 && visited.add(directory.uri)) { "Recursive or overly deep game folder" }
            for (child in directory.listFiles()) {
                nodes++
                check(nodes <= 4096) { "Game folder is too large to import" }
                val name = checkNotNull(child.name)
                val relative = PcWineFolderBundle.validPath(prefix + name)
                when {
                    child.isDirectory -> walk(child, relative + "/", depth + 1)
                    child.isFile -> {
                        val sourceUri = child.uri
                        candidates +=
                            PcWineFolderBundle.Source(relative) {
                                checkNotNull(context.contentResolver.openInputStream(sourceUri)) {
                                    "Game folder file no longer available"
                                }
                            }
                    }
                    else -> error("Unsupported game folder entry")
                }
            }
        }

        walk(root, "", 0)
        val exe = candidates.map { it.path }.filter { it.endsWith(".exe", ignoreCase = true) }.sorted()
        check(candidates.isNotEmpty() && exe.isNotEmpty()) { "Choose a folder containing a Windows EXE" }
        return Selection(root.name?.take(120)?.ifBlank { "Windows game" } ?: "Windows game", candidates, exe)
    }
}
