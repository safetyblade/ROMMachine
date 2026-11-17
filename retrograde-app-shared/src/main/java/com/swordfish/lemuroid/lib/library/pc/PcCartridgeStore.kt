package com.swordfish.lemuroid.lib.library.pc

import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Explicit local PC imports only. An immutable DOSZ holds optical media; the core
 * receives it through the normal DOS launcher. Guest and save paths are separate.
 */
class PcCartridgeStore(private val filesRoot: File) {
    private val root = File(filesRoot, "pc-cartridges-v1")

    data class Entry(
        val id: UUID,
        val title: String,
        val content: File,
    )

    private data class Disc(
        val file: File,
        val crc: Long,
    )

    fun entries(): List<Entry> {
        if (!root.exists()) return emptyList()
        return checkNotNull(root.listFiles()).filterNot { it.name.startsWith(".") }.mapNotNull(::readEntry)
    }

    fun resolve(content: File): Entry? {
        val parent = content.parentFile ?: return null
        if (parent.parentFile?.canonicalFile != root.canonicalFile) return null
        val entry = readEntry(parent) ?: return null
        return entry.takeIf { it.content.canonicalFile == content.canonicalFile }
    }

    fun workspace(entry: Entry): PcWorkspace = PcWorkspace(filesRoot, LOCAL_PLAYER, entry.id)

    /**
     * The caller selects and opens streams read-only, and owns them. Nothing is
     * published to the library until the whole archive has been validated and synced.
     */
    fun importIso(
        title: String,
        game: InputStream,
        windows: InputStream? = null,
        checkCancellation: () -> Unit = {},
    ): Entry {
        val cleanTitle = title.trim().take(120)
        require(cleanTitle.isNotEmpty() && cleanTitle.none { it.isISOControl() })
        check(root.isDirectory || root.mkdirs())
        val id = UUID.randomUUID()
        val stage = File(root, ".$id.import")
        check(stage.mkdir())
        try {
            val gameDisc = copyIso(stage, "01-game.iso", game, checkCancellation)
            val osDisc = windows?.let { copyIso(stage, "00-windows.iso", it, checkCancellation) }
            val discs = listOfNotNull(osDisc, gameDisc)
            check(stage.usableSpace > discs.sumOf { it.file.length() } + RESERVE) { "Not enough storage for PC cartridge" }
            val archive = File(stage, "$id.dosz")
            archive.outputStream().use { output ->
                ZipOutputStream(output).use { zip ->
                    for (disc in discs) {
                        val entry =
                            ZipEntry(disc.file.name).apply {
                                method = ZipEntry.STORED
                                size = disc.file.length()
                                compressedSize = size
                                crc = disc.crc
                            }
                        zip.putNextEntry(entry)
                        disc.file.inputStream().use { copyBounded(it, zip, checkCancellation) }
                        zip.closeEntry()
                    }
                    zip.finish()
                    zip.flush()
                    output.fd.sync()
                }
            }
            discs.forEach { check(it.file.delete()) }
            File(stage, "title.txt").outputStream().use {
                it.write(cleanTitle.toByteArray(Charsets.UTF_8))
                it.fd.sync()
            }
            check(archive.setReadOnly())
            checkCancellation()
            val destination = File(root, id.toString())
            check(!destination.exists() && stage.renameTo(destination))
            return checkNotNull(readEntry(destination))
        } finally {
            if (stage.exists()) stage.deleteRecursively()
        }
    }

    private fun copyIso(
        stage: File,
        name: String,
        source: InputStream,
        checkCancellation: () -> Unit,
    ): Disc {
        val target = File(stage, name)
        val crc = CRC32()
        target.outputStream().use { output ->
            copyBounded(source, output, checkCancellation) { bytes, count ->
                check(stage.usableSpace > RESERVE) { "Not enough storage for PC media" }
                crc.update(bytes, 0, count)
            }
            output.fd.sync()
        }
        require(PcMediaInspector.inspect(target) == PcMediaInspector.Kind.ISO9660) { "Not a supported ISO image" }
        return Disc(target, crc.value)
    }

    private fun copyBounded(
        input: InputStream,
        output: java.io.OutputStream,
        checkCancellation: () -> Unit,
        onChunk: (ByteArray, Int) -> Unit = { _, _ -> },
    ) {
        val buffer = ByteArray(128 * 1024)
        var total = 0L
        while (true) {
            checkCancellation()
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) continue
            total += count
            require(total <= MAX_ISO) { "PC ISO exceeds the current 2 GiB import limit" }
            onChunk(buffer, count)
            output.write(buffer, 0, count)
        }
    }

    private fun readEntry(directory: File): Entry? {
        val id = runCatching { UUID.fromString(directory.name) }.getOrNull() ?: return null
        if (directory.canonicalFile != File(root.canonicalFile, id.toString())) return null
        val archive = File(directory, "$id.dosz")
        val titleFile = File(directory, "title.txt")
        if (!archive.isFile || !titleFile.isFile || titleFile.length() !in 1..512) return null
        if (archive.canonicalFile != archive.absoluteFile || titleFile.canonicalFile != titleFile.absoluteFile) return null
        val title = titleFile.readText(Charsets.UTF_8)
        if (title.isBlank() || title.length > 120 || title.any { it.isISOControl() }) return null
        return Entry(id, title, archive)
    }

    companion object {
        // Current app has no shared player-profile service. Keep the single local
        // player explicit; a future profile service must supply authenticated IDs.
        private val LOCAL_PLAYER = UUID.fromString("00000000-0000-4000-8000-000000000001")
        private const val MAX_ISO = 2L * 1024 * 1024 * 1024
        private const val RESERVE = 512L * 1024 * 1024
    }
}
