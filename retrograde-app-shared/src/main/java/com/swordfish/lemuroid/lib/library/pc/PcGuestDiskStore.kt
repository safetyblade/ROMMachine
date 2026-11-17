package com.swordfish.lemuroid.lib.library.pc

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.util.Locale
import java.util.UUID

/** Only mutates the selected cartridge's own guest directory, never its media or saves. */
class PcGuestDiskStore(private val workspace: PcWorkspace) {
    fun disks(): List<File> {
        if (!workspace.guest.exists()) return emptyList()
        return checkNotNull(workspace.guest.listFiles()).filter {
            !it.name.startsWith(".") && it.isFile &&
                it.extension.lowercase(Locale.ROOT) in setOf("img", "ima", "vhd") &&
                it.length() >= MIN_BYTES && it.length() % 512 == 0L &&
                it.canonicalFile == File(workspace.guest.canonicalFile, it.name)
        }.sortedBy { it.name }
    }

    /** Copies an existing, user-supplied guest image. It never overwrites an installed OS. */
    @Synchronized
    fun importPrepared(
        source: InputStream,
        checkCancellation: () -> Unit = {},
    ): File {
        workspace.prepareWritableDirectories()
        check(disks().isEmpty()) { "A guest disk already exists. Restore requires a separate cartridge." }
        val stage = File(workspace.guest, ".incoming-${UUID.randomUUID()}.img")
        val destination = File(workspace.guest, "cartdeck-os.img")
        try {
            stage.outputStream().use { output ->
                val bytes = ByteArray(128 * 1024)
                var total = 0L
                while (true) {
                    checkCancellation()
                    val count = source.read(bytes)
                    if (count < 0) break
                    if (count == 0) continue
                    total += count
                    require(total <= MAX_BYTES) { "Guest image exceeds 2 GiB" }
                    check(stage.usableSpace > RESERVE) { "Not enough storage for guest image" }
                    output.write(bytes, 0, count)
                }
                output.fd.sync()
            }
            require(
                stage.length() >= MIN_BYTES && PcMediaInspector.inspect(stage) == PcMediaInspector.Kind.DISK_IMAGE,
            ) {
                "Select a raw sector-aligned hard disk image"
            }
            checkCancellation()
            // No replace-existing flag: even another process cannot overwrite a guest.
            Files.move(stage.toPath(), destination.toPath())
            return destination
        } finally {
            if (stage.exists()) stage.delete()
        }
    }

    companion object {
        private const val MIN_BYTES = 7L * 1024 * 1024
        private const val MAX_BYTES = 2L * 1024 * 1024 * 1024
        private const val RESERVE = 512L * 1024 * 1024
    }
}
