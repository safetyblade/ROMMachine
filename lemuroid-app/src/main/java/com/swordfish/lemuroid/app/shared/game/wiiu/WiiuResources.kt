package com.swordfish.lemuroid.app.shared.game.wiiu

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.swordfish.lemuroid.lib.storage.local.CachedRomStager
import java.io.File
import java.io.FilterInputStream

/** Import owner-selected Cemu resources into system storage; never replace the cartridge NAND. */
object WiiuResources {
    fun import(
        context: Context,
        tree: Uri,
        cartridgeRoot: File,
        cancelled: () -> Unit,
    ) {
        val source = DocumentFile.fromTreeUri(context, tree) ?: error("Cannot open the selected resources folder")
        val destination = File(cartridgeRoot, "system/Cemu")
        var files = 0

        fun copy(
            document: DocumentFile,
            output: File,
            depth: Int,
        ) {
            cancelled()
            check(++files <= 50000 && depth <= 32) { "Wii U resource import exceeds limits" }
            if (document.isDirectory) {
                check(output.isDirectory || output.mkdirs()) { "Cannot create Wii U resources directory" }
                document.listFiles().forEach { child ->
                    val name = WiiuStorage.component(child.name ?: error("Unnamed resource"))
                    check(depth != 0 || !name.equals("mlc01", true)) {
                        "Select resources without mlc01; cartridge NAND is protected."
                    }
                    copy(child, File(output, name), depth + 1)
                }
            } else {
                CachedRomStager.stage(output, document.length(), document.lastModified()) {
                    val input =
                        context.contentResolver.openInputStream(document.uri)
                            ?: error("Cannot read Wii U resource")
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
        copy(source, destination, 0)
    }
}
