package com.swordfish.lemuroid.app.shared.game.ps2next

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.swordfish.lemuroid.lib.R
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

internal object Ps2BiosFolder {
    fun singleImportCandidate(context: Context): Uri? {
        val folder = find(context) ?: return null
        val id = DocumentsContract.getDocumentId(folder)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(folder, id)
        val projection =
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            )
        return context.contentResolver.query(children, projection, null, null, null)?.use { cursor ->
            var candidate: Uri? = null
            var scanned = 0
            while (cursor.moveToNext()) {
                if (++scanned > 64) return@use null
                val child = cursor.getString(0) ?: continue
                val extension = cursor.getString(1)?.substringAfterLast('.')?.lowercase()
                if (extension in setOf("bin", "rom", "bios") && cursor.getLong(2) in 1L..MAX_BYTES &&
                    cursor.getString(3) != DocumentsContract.Document.MIME_TYPE_DIR
                ) {
                    if (candidate != null) return@use null
                    candidate = DocumentsContract.buildDocumentUriUsingTree(folder, child)
                }
            }
            candidate
        }
    }

    private const val MAX_BYTES = 16L * 1024 * 1024

    fun find(
        context: Context,
        create: Boolean = false,
    ): Uri? {
        val path =
            SharedPreferencesHelper.getLegacySharedPreferences(context)
                .getString(context.getString(R.string.pref_key_extenral_folder), null)
                ?.takeIf { it.isNotBlank() } ?: return null
        val root = DocumentFile.fromTreeUri(context, Uri.parse(path)) ?: return null
        if (root.name?.equals("ROMs", ignoreCase = true) == true) return null
        val bios = root.findFile("BIOS") ?: if (create && root.canWrite()) root.createDirectory("BIOS") else null
        if (bios?.isDirectory != true) return null
        val ps2 = bios.findFile("PS2") ?: if (create && bios.canWrite()) bios.createDirectory("PS2") else null
        return ps2?.takeIf { it.isDirectory && it.canRead() }?.uri
    }
}
