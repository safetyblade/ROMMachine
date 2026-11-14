package com.swordfish.lemuroid.app.shared.game.xbox360

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.swordfish.lemuroid.lib.storage.local.CachedRomStager
import com.swordfish.lemuroid.lib.storage.scanner.Xbox360ContentIdentity
import java.io.File
import java.security.MessageDigest

/** Read-only SAF import. Only disposable staging changes; guest content and saves are never touched. */
internal object Xbox360Content {
    fun stage(
        context: Context,
        uri: Uri,
        checkCancelled: () -> Unit,
    ): File {
        if (uri.scheme == "file") {
            val file = File(requireNotNull(uri.path))
            file.inputStream().channel.use {
                val result = Xbox360ContentIdentity.probe(file.name, it)
                check(result.valid) { result.reason }
            }
            return file
        }
        val document = DocumentFile.fromSingleUri(context, uri) ?: error("Xbox 360 content is no longer accessible")
        val name = component(document.name ?: error("Xbox 360 content has no filename"))
        val extension = name.substringAfterLast('.', "").lowercase()
        check(extension in Xbox360ContentIdentity.formats) {
            "Xbox 360 supports verified ISO, XEX game folders, ZAR and XCP content; M3U is not supported by this engine"
        }
        val key =
            MessageDigest.getInstance("SHA-256").digest(uri.toString().toByteArray())
                .joinToString("") { "%02x".format(it.toInt() and 255) }
        val root = File(Xbox360Engine.data(context.filesDir), "staging/$key")
        val target = File(root, name)
        var count = 0

        fun copy(
            source: DocumentFile,
            destination: File,
            depth: Int,
        ) {
            checkCancelled()
            check(++count <= 50000 && depth <= 32) { "Xbox 360 game folder exceeds safe import limits" }
            if (source.isDirectory) {
                check(destination.isDirectory || destination.mkdirs()) { "Cannot stage Xbox 360 game folder" }
                source.listFiles().forEach {
                    copy(
                        it,
                        File(destination, component(it.name ?: error("Unnamed game file"))),
                        depth + 1,
                    )
                }
            } else {
                CachedRomStager.stage(destination, source.length(), source.lastModified()) {
                    context.contentResolver.openInputStream(source.uri) ?: error("Xbox 360 game file is inaccessible")
                }
            }
        }
        if (extension == "xex") {
            check(DocumentsContract.isTreeUri(uri)) {
                "Import extracted Xbox 360 games from a permitted ROM folder so companion files remain accessible"
            }
            val id = DocumentsContract.getDocumentId(uri)
            check('/' in id) { "Select the extracted game's folder when granting ROM access" }
            val parentUri = DocumentsContract.buildDocumentUriUsingTree(uri, id.substringBeforeLast('/'))
            val children =
                DocumentsContract.buildChildDocumentsUriUsingTree(
                    parentUri,
                    DocumentsContract.getDocumentId(parentUri),
                )
            context.contentResolver.query(
                children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null,
            )?.use {
                    cursor ->
                while (cursor.moveToNext()) {
                    val childUri = DocumentsContract.buildDocumentUriUsingTree(uri, cursor.getString(0))
                    val child =
                        DocumentFile.fromSingleUri(context, childUri)
                            ?: error("Xbox 360 companion file is inaccessible")
                    copyTree(
                        context, child, File(root, component(child.name ?: error("Unnamed game file"))),
                        0, checkCancelled,
                    ) {
                        ++count <= 50000
                    }
                }
            } ?: error("Cannot read the extracted Xbox 360 game folder")
        } else {
            copy(document, target, 0)
            if (extension == "xcp") {
                val volume =
                    java.io.RandomAccessFile(target, "r").use { file ->
                        check(file.length() >= 0x3AD) { "Xbox 360 package header is truncated" }
                        file.seek(0x3A9)
                        file.readInt()
                    }
                if (volume == 1) {
                    check(
                        DocumentsContract.isTreeUri(uri),
                    ) { "Xbox 360 SVOD requires its sibling .data folder permission" }
                    val id = DocumentsContract.getDocumentId(uri)
                    check('/' in id) { "Select the Xbox 360 package folder when granting ROM access" }
                    val children = DocumentsContract.buildChildDocumentsUriUsingTree(uri, id.substringBeforeLast('/'))
                    var found = false
                    context.contentResolver.query(
                        children,
                        arrayOf(
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        ),
                        null,
                        null,
                        null,
                    )?.use { cursor ->
                        while (cursor.moveToNext()) {
                            if (cursor.getString(1) != "$name.data") continue
                            val siblingUri = DocumentsContract.buildDocumentUriUsingTree(uri, cursor.getString(0))
                            val sibling =
                                DocumentFile.fromSingleUri(context, siblingUri)
                                    ?: error("Xbox 360 SVOD data folder is inaccessible")
                            check(sibling.isDirectory) { "Xbox 360 SVOD requires a .data directory" }
                            copyTree(context, sibling, File(root, "$name.data"), 0, checkCancelled) { ++count <= 50000 }
                            found = true
                        }
                    }
                    check(found) { "Xbox 360 SVOD package is incomplete: sibling .data folder missing" }
                }
            }
        }
        checkCancelled()
        target.inputStream().channel.use {
            val result = Xbox360ContentIdentity.probe(name, it)
            check(result.valid) { result.reason }
        }
        return target
    }

    private fun copyTree(
        context: Context,
        source: DocumentFile,
        destination: File,
        depth: Int,
        checkCancelled: () -> Unit,
        count: () -> Boolean,
    ) {
        checkCancelled()
        check(depth <= 32 && count()) { "Xbox 360 game folder exceeds safe import limits" }
        if (!source.isDirectory) {
            CachedRomStager.stage(destination, source.length(), source.lastModified()) {
                context.contentResolver.openInputStream(source.uri) ?: error("Xbox 360 companion file is inaccessible")
            }
            return
        }
        check(destination.isDirectory || destination.mkdirs())
        val children =
            DocumentsContract.buildChildDocumentsUriUsingTree(
                source.uri,
                DocumentsContract.getDocumentId(source.uri),
            )
        context.contentResolver.query(
            children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null,
        )?.use {
                cursor ->
            while (cursor.moveToNext()) {
                val childUri = DocumentsContract.buildDocumentUriUsingTree(source.uri, cursor.getString(0))
                val child =
                    DocumentFile.fromSingleUri(context, childUri)
                        ?: error("Missing Xbox 360 companion file")
                copyTree(
                    context, child, File(destination, component(child.name ?: error("Unnamed game file"))),
                    depth + 1, checkCancelled, count,
                )
            }
        } ?: error("Cannot read Xbox 360 companion folder")
    }

    private fun component(name: String): String {
        require(
            name.isNotBlank() && name !in setOf(".", "..") && name.none { it == '/' || it == '\\' || it.code < 32 },
        ) {
            "Xbox 360 game folder contains an unsafe filename"
        }
        return name
    }
}
