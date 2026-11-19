package com.swordfish.lemuroid.app.shared.game.ps3

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.swordfish.lemuroid.lib.library.db.entity.Game
import java.io.File

object Ps3Content {
    /** Stages the complete selected disc folder, never only PARAM.SFO/EBOOT.BIN. */
    fun stage(
        context: Context,
        game: Game,
        engineRoot: File,
    ): File {
        val uri = Uri.parse(game.fileUri)
        if (uri.scheme == "file") {
            val f = File(requireNotNull(uri.path))
            require(f.isFile) { "Game path is missing. Repair the cartridge path." }
            return if (f.name.equals("PARAM.SFO", true)) f.parentFile!! else f
        }
        val root = File(engineRoot, "content/${game.cartridgeId}")
        require(root.canonicalPath.startsWith(File(engineRoot, "content").canonicalPath + File.separator))
        if (game.fileName.equals("PARAM.SFO", true)) {
            val id = DocumentsContract.getDocumentId(uri)
            require('/' in id) { "Select the complete PS3 game folder through Library scan." }
            var parentId = id.substringBeforeLast('/')
            if (parentId.substringAfterLast('/').equals("PS3_GAME", true)) parentId = parentId.substringBeforeLast('/')
            copyTree(context, uri, parentId, root)
            root.setLastModified(System.currentTimeMillis())
            return root
        }
        val target = File(root, game.fileName)
        require(target.canonicalFile.parentFile == root.canonicalFile)
        check(root.mkdirs() || root.isDirectory)
        val original = DocumentFile.fromSingleUri(context, uri) ?: error("Media is not accessible")
        val stamp = "${original.length()}:${original.lastModified()}:${game.fileUri}"
        val marker = File(root, ".source")
        if (target.isFile && marker.isFile && marker.readText() == stamp &&
            target.length() == original.length()
        ) {
            root.setLastModified(System.currentTimeMillis())
            return target
        }
        val pending = File(root, ".media.pending")
        context.contentResolver.openInputStream(uri)?.use { input -> pending.outputStream().use { input.copyTo(it) } }
            ?: error("Media cannot be read")
        require(original.length() <= 0 || pending.length() == original.length()) { "Incomplete PS3 media copy" }
        check(pending.renameTo(target))
        marker.writeText(stamp)
        root.setLastModified(System.currentTimeMillis())
        return target
    }

    private fun copyTree(
        context: Context,
        tree: Uri,
        directory: String,
        target: File,
    ) {
        check(target.mkdirs() || target.isDirectory)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, directory)
        val columns =
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            )
        val entries = mutableListOf<Array<String>>()
        context.contentResolver.query(children, columns, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) entries += Array(5) { cursor.getString(it).orEmpty() }
        } ?: error("PS3 game folder could not be enumerated")
        for (entry in entries) {
            val (id, name, mime) = entry
            require(name.isNotBlank() && name !in setOf(".", "..") && '/' !in name && '\\' !in name)
            val destination = File(target, name)
            require(destination.canonicalFile.parentFile == target.canonicalFile)
            if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                copyTree(context, tree, id, destination)
            } else {
                val length = entry[3].toLongOrNull() ?: 0
                val modified = entry[4].toLongOrNull() ?: 0
                if (destination.isFile && destination.length() == length && modified > 0 &&
                    destination.lastModified() == modified
                ) {
                    continue
                }
                val pending = File(target, ".$name.pending")
                val child = DocumentsContract.buildDocumentUriUsingTree(tree, id)
                context.contentResolver.openInputStream(child)?.use { input ->
                    pending.outputStream().use { input.copyTo(it) }
                }
                    ?: error("Missing member of PS3 disc folder")
                require(length <= 0 || pending.length() == length)
                check(pending.renameTo(destination))
                if (modified > 0) destination.setLastModified(modified)
            }
        }
    }
}
