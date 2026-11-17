package com.swordfish.lemuroid.lib.library.pc

import java.io.File
import java.io.InputStream
import java.util.UUID

/** PC-private storage; no migration or writes to existing console save roots. */
class PcWorkspace(root: File, profile: UUID, cartridge: UUID) {
    private val base = File(root, "pc/$profile/$cartridge")
    val media = File(base, "media")
    val guest = File(base, "guest")
    val runtime = File(base, "runtime")
    val progress = File(base, "progress")

    /** Copies a user-selected stream; never opens the original for writing. */
    @Synchronized
    fun importMedia(extension: String, source: InputStream): File {
        require(extension in setOf("iso", "bin", "img"))
        check(media.mkdirs() || media.isDirectory)
        val temporary = File.createTempFile("import-", ".$extension", media)
        try {
            val maximum = minOf(8L * 1024 * 1024 * 1024, media.usableSpace - 128L * 1024 * 1024)
            require(maximum > 0) { "Insufficient PC media storage" }
            temporary.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                var written = 0L
                while (true) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    if (count == 0) continue
                    written += count
                    require(written <= maximum) { "PC media exceeds safe import limit" }
                    output.write(buffer, 0, count)
                }
            }
            require(PcMediaInspector.inspect(temporary) != PcMediaInspector.Kind.UNSUPPORTED)
            check(temporary.setReadOnly())
            val target = File(media, "${UUID.randomUUID()}.$extension")
            check(!target.exists() && temporary.renameTo(target))
            return target
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    fun prepareWritableDirectories() {
        for (directory in listOf(guest, runtime, progress)) {
            check(directory.mkdirs() || directory.isDirectory)
        }
    }
}
