package com.swordfish.lemuroid.app.shared.game.ps2next

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.swordfish.lemuroid.lib.storage.local.CachedRomStager
import java.io.File
import java.io.FilterInputStream
import java.security.MessageDigest

object Armsx2Content {
    fun stage(
        context: Context,
        uri: Uri,
        cartridgeId: String,
        cancelled: () -> Unit,
    ): File {
        Armsx2Storage.paths(context.filesDir, cartridgeId) // Validate immutable identity before using it in a path.
        if (uri.scheme == "file") {
            return File(requireNotNull(uri.path)).also {
                check(it.isFile && it.length() > 0 && it.extension.lowercase() in setOf("iso", "chd"))
            }
        }
        check(uri.scheme == "content") { "Select a local ISO or CHD" }
        val document = DocumentFile.fromSingleUri(context, uri) ?: error("PS2 media is inaccessible")
        val extension = document.name?.substringAfterLast('.', "")?.lowercase()
        check(extension in setOf("iso", "chd")) { "ARMSX2 supports ISO and CHD cartridges" }
        val hash =
            MessageDigest.getInstance("SHA-256").digest(uri.toString().toByteArray())
                .joinToString("") { "%02x".format(it.toInt() and 255) }
        val destination = File(context.cacheDir, "native/ps2/armsx2/$cartridgeId/$hash.$extension")
        return CachedRomStager.stage(destination, document.length(), document.lastModified()) {
            val input = context.contentResolver.openInputStream(uri) ?: error("PS2 media is inaccessible")
            object : FilterInputStream(input) {
                override fun read(): Int {
                    cancelled()
                    return super.read()
                }

                override fun read(
                    buffer: ByteArray,
                    offset: Int,
                    length: Int,
                ): Int {
                    cancelled()
                    return super.read(buffer, offset, length)
                }
            }
        }
    }
}
