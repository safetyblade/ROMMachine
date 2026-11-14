package com.swordfish.lemuroid.lib.mods

import java.io.File
import java.io.RandomAccessFile
import java.util.Locale
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipFile

/** ZIP inspection finishes before extraction. Links, ZIP64, encrypted and split archives fail closed. */
class ModArchive(private val limits: Limits = Limits()) {
    data class Limits(
        val entries: Int = 20_000,
        val compressedBytes: Long = 512L * 1024 * 1024,
        val expandedBytes: Long = 2L * 1024 * 1024 * 1024,
        val ratio: Long = 200,
        val reserveBytes: Long = 64L * 1024 * 1024,
    ) {
        init {
            require(entries > 0 && compressedBytes > 0 && expandedBytes > 0 && ratio > 0 && reserveBytes >= 0)
        }
    }

    data class Entry(val path: String, val size: Long, val crc: Long, val directory: Boolean)

    fun inspect(archive: File): List<Entry> {
        require(archive.length() in 22..limits.compressedBytes) { "Empty or oversized mod ZIP" }
        val modes = inspectCentralDirectory(archive)
        ZipFile(archive).use { zip ->
            require(zip.size() in 1..limits.entries) { "Mod ZIP contains too many files or is empty" }
            val names = HashSet<String>()
            var expanded = 0L
            val result =
                zip.entries().asSequence().map { entry ->
                    val path = ModFiles.relativePath(entry.name)
                    require(names.add(path.lowercase(Locale.ROOT))) { "Duplicate or case-colliding mod path: $path" }
                    require(entry.method == 0 || entry.method == 8) { "Unsupported ZIP compression" }
                    require(entry.size >= 0 && entry.compressedSize >= 0 && entry.crc >= 0) { "Incomplete ZIP entry" }
                    require(entry.size <= limits.expandedBytes - expanded) { "Expanded mod ZIP exceeds storage limit" }
                    expanded += entry.size
                    require(
                        entry.size <=
                            maxOf(1, entry.compressedSize).let {
                                if (it > Long.MAX_VALUE / limits.ratio) Long.MAX_VALUE else it * limits.ratio
                            },
                    ) { "Excessive ZIP compression ratio" }
                    val mode = modes[entry.name] ?: error("ZIP directory mismatch")
                    require(mode == 0 || mode == 0x8000 || mode == 0x4000) { "Links and special files are not allowed" }
                    require((mode != 0x4000 || entry.isDirectory) && (mode != 0x8000 || !entry.isDirectory)) {
                        "ZIP file type mismatch"
                    }
                    Entry(path, entry.size, entry.crc, entry.isDirectory)
                }.toList()
            require(modes.size == result.size) { "ZIP entry count mismatch" }
            val filePaths = result.filterNot { it.directory }.map { it.path.lowercase(Locale.ROOT) }.toSet()
            result.forEach { entry ->
                var parent = entry.path.substringBeforeLast('/', "")
                while (parent.isNotEmpty()) {
                    require(parent.lowercase(Locale.ROOT) !in filePaths) { "ZIP file/directory collision" }
                    parent = parent.substringBeforeLast('/', "")
                }
            }
            return result
        }
    }

    /** Caller owns the returned staging directory; on any error no extracted data survives. */
    fun extract(
        archive: File,
        stagingParent: File,
        accepts: (String) -> Boolean,
        cancelled: () -> Boolean = { false },
    ): File {
        val entries = inspect(archive)
        entries.filterNot { it.directory }.forEach { require(accepts(it.path)) { "Unsupported mod file: ${it.path}" } }
        check(stagingParent.isDirectory || stagingParent.mkdirs()) { "Cannot create mod staging storage" }
        val total = entries.sumOf { it.size }
        check(stagingParent.usableSpace >= total && stagingParent.usableSpace - total >= limits.reserveBytes) {
            "Not enough storage for this mod; free space and retry"
        }
        val stage = File(stagingParent, "import-${UUID.randomUUID()}")
        check(stage.mkdir()) { "Cannot create isolated mod staging directory" }
        try {
            ZipFile(archive).use { zip ->
                for (entry in entries) {
                    check(!cancelled()) { "Mod import cancelled" }
                    val target = File(stage, entry.path)
                    if (entry.directory) {
                        check(target.isDirectory || target.mkdirs())
                    } else {
                        check(target.parentFile!!.isDirectory || target.parentFile!!.mkdirs())
                        val crc = CRC32()
                        zip.getInputStream(zip.getEntry(entry.path)).use { input ->
                            val checked = java.util.zip.CheckedInputStream(input, crc)
                            require(ModFiles.copyBounded(checked, target, entry.size, cancelled) == entry.size) {
                                "Incomplete mod file: ${entry.path}"
                            }
                        }
                        require(crc.value == entry.crc) { "Damaged mod file: ${entry.path}" }
                    }
                }
            }
            return stage
        } catch (failure: Throwable) {
            stage.deleteRecursively()
            throw failure
        }
    }

    private fun inspectCentralDirectory(archive: File): Map<String, Int> =
        RandomAccessFile(archive, "r").use { file ->
            fun u16(): Int = file.readUnsignedByte() or (file.readUnsignedByte() shl 8)

            fun u32(): Long = u16().toLong() or (u16().toLong() shl 16)
            var end = file.length() - 22
            val first = maxOf(0, file.length() - 65557)
            var found = false
            while (end >= first) {
                file.seek(end)
                if (u32() == 0x06054b50L) {
                    file.seek(end + 20)
                    if (end + 22 + u16() == file.length()) {
                        found = true
                        break
                    }
                }
                end--
            }
            require(found) { "ZIP end directory is missing or damaged" }
            file.seek(end + 4)
            require(u16() == 0 && u16() == 0) { "Split ZIP archives are not supported" }
            val diskEntries = u16()
            val count = u16()
            require(count == diskEntries && count in 1..minOf(limits.entries, 65534)) { "Unsupported ZIP entry count" }
            val size = u32()
            val start = u32()
            require(start <= end && size == end - start) { "Invalid or ZIP64 central directory" }
            file.seek(start)
            val modes = mutableMapOf<String, Int>()
            val ranges = mutableListOf<LongRange>()
            repeat(count) {
                val position = file.filePointer
                require(position + 46 <= end && u32() == 0x02014b50L) { "Damaged ZIP central directory" }
                val madeBy = u16()
                u16()
                val flags = u16()
                require(flags and 0x2041 == 0) { "Encrypted ZIP archives are not supported" }
                val method = u16()
                file.seek(position + 20)
                val compressedSize = u32()
                val uncompressedSize = u32()
                require(compressedSize != 0xffffffffL && uncompressedSize != 0xffffffffL) { "ZIP64 is not supported" }
                file.seek(position + 28)
                val nameLength = u16()
                val extraLength = u16()
                val commentLength = u16()
                require(u16() == 0) { "Split ZIP archive" }
                u16()
                val attributes = u32()
                val localOffset = u32()
                val next = position + 46 + nameLength + extraLength + commentLength
                require(nameLength in 1..1024 && next <= end && localOffset < start) { "Invalid ZIP entry bounds" }
                val nameBytes = ByteArray(nameLength)
                file.readFully(nameBytes)
                val name = String(nameBytes, if (flags and 0x800 != 0) Charsets.UTF_8 else charset("Cp437"))
                ModFiles.relativePath(name)
                require(
                    modes.put(name, if (madeBy shr 8 == 3) ((attributes shr 16).toInt() and 0xf000) else 0) == null,
                ) {
                    "Duplicate ZIP entry"
                }
                file.seek(localOffset)
                require(u32() == 0x04034b50L) { "Missing ZIP local header" }
                file.seek(localOffset + 6)
                require(u16() == flags && u16() == method) { "ZIP local header flags or compression mismatch" }
                file.seek(localOffset + 26)
                val localNameLength = u16()
                val localExtraLength = u16()
                require(
                    localNameLength == nameLength && localOffset + 30 + localNameLength + localExtraLength <= start,
                ) {
                    "ZIP local header bounds mismatch"
                }
                val dataEnd = localOffset + 30 + localNameLength + localExtraLength + compressedSize
                require(dataEnd <= start) { "ZIP data overlaps central directory" }
                ranges += localOffset until dataEnd
                val localName = ByteArray(localNameLength)
                file.readFully(localName)
                require(localName.contentEquals(nameBytes)) { "ZIP local header name mismatch" }
                file.seek(next)
            }
            require(file.filePointer == end) { "ZIP central directory size mismatch" }
            ranges.sortedBy { it.first }.zipWithNext().forEach { (a, b) ->
                require(a.last < b.first) { "ZIP entries overlap" }
            }
            modes
        }
}
