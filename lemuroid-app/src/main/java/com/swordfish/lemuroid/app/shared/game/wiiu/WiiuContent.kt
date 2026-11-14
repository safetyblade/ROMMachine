package com.swordfish.lemuroid.app.shared.game.wiiu

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.swordfish.lemuroid.lib.storage.local.CachedRomStager
import java.io.File
import java.io.FilterInputStream

/** Read-only media import, including the content/meta siblings of an RPX code directory. */
object WiiuContent {
    val formats = setOf("wua", "wud", "wux", "rpx", "elf", "tmd")

    fun stage(
        context: Context,
        uri: Uri,
        cartridgeId: String,
        checkCancelled: () -> Unit,
    ): File {
        if (uri.scheme == "file") {
            val file = File(requireNotNull(uri.path))
            check(file.isFile && file.length() > 0 && file.extension.lowercase() in formats) {
                "Wii U media is unavailable or unsupported"
            }
            return file
        }
        check(uri.scheme == "content") { "Wii U requires a local file or a granted ROM folder" }
        val document = DocumentFile.fromSingleUri(context, uri) ?: error("Wii U media is inaccessible")
        val name = WiiuStorage.component(document.name ?: error("Wii U media has no filename"))
        val extension = name.substringAfterLast('.', "").lowercase()
        check(extension in formats) { "Unsupported Wii U media format" }
        val root = WiiuStorage.stagingRoot(context.cacheDir, cartridgeId, uri.toString())
        var count = 0

        fun copy(
            source: DocumentFile,
            target: File,
            depth: Int,
        ) {
            checkCancelled()
            check(++count <= 50000 && depth <= 32) { "Wii U content exceeds import limits" }
            if (!source.isDirectory) {
                CachedRomStager.stage(target, source.length(), source.lastModified()) {
                    val stream =
                        context.contentResolver.openInputStream(source.uri)
                            ?: error("Wii U content is inaccessible")
                    object : FilterInputStream(stream) {
                        override fun read(): Int {
                            checkCancelled()
                            return super.read()
                        }

                        override fun read(
                            buffer: ByteArray,
                            offset: Int,
                            length: Int,
                        ): Int {
                            checkCancelled()
                            return super.read(buffer, offset, length)
                        }
                    }
                }
                return
            }
            check(target.isDirectory || target.mkdirs()) { "Cannot stage Wii U directory" }
            val children =
                DocumentsContract.buildChildDocumentsUriUsingTree(
                    source.uri,
                    DocumentsContract.getDocumentId(source.uri),
                )
            context.contentResolver.query(
                children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null,
            )
                ?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val childUri = DocumentsContract.buildDocumentUriUsingTree(source.uri, cursor.getString(0))
                        val child =
                            DocumentFile.fromSingleUri(context, childUri)
                                ?: error("Wii U companion is inaccessible")
                        val childName = WiiuStorage.component(child.name ?: error("Unnamed Wii U companion"))
                        copy(child, File(target, childName), depth + 1)
                    }
                } ?: error("Cannot enumerate Wii U game companions")
        }

        if (extension in setOf("rpx", "elf", "tmd")) {
            check(DocumentsContract.isTreeUri(uri)) { "Grant the extracted game folder, including companion files" }
            val id = DocumentsContract.getDocumentId(uri)
            check('/' in id) { "Grant the extracted Wii U game folder" }
            var parentId = id.substringBeforeLast('/')
            val codeDirectory = parentId.substringAfterLast('/')
            val inCodeDirectory = codeDirectory.equals("code", ignoreCase = true)
            if (inCodeDirectory) {
                check('/' in parentId) { "Grant the game directory containing code/content/meta" }
                parentId = parentId.substringBeforeLast('/')
            }
            val parentUri = DocumentsContract.buildDocumentUriUsingTree(uri, parentId)
            val parent = DocumentFile.fromSingleUri(context, parentUri) ?: error("Wii U game folder is inaccessible")
            copy(parent, root, 0)
            val relative = if (inCodeDirectory) "$codeDirectory/$name" else name
            return File(
                root,
                relative,
            ).also { check(it.isFile && it.length() > 0) { "Wii U media copy is incomplete" } }
        }
        return File(root, name).also {
            copy(document, it, 0)
            checkCancelled()
        }
    }
}
