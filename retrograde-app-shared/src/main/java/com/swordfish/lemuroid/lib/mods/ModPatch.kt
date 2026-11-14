package com.swordfish.lemuroid.lib.mods

import java.io.File
import java.io.RandomAccessFile
import java.util.UUID
import java.util.zip.CRC32

/** Verified, out-of-place patching. Both hashes are required even for IPS, which has no embedded checksum. */
class ModPatch(private val maxOutputBytes: Long = 8L * 1024 * 1024 * 1024) {
    fun apply(
        base: File,
        patch: File,
        destination: File,
        baseSha256: String,
        targetSha256: String,
        cancelled: () -> Boolean = { false },
    ) {
        require(maxOutputBytes > 0)
        require(baseSha256.matches(Regex("[0-9a-fA-F]{64}")) && targetSha256.matches(Regex("[0-9a-fA-F]{64}"))) {
            "The mod must specify SHA-256 checksums for the original and modified game"
        }
        require(base.isFile && patch.isFile && !destination.exists()) {
            "Keep the original game; choose a new edition destination"
        }
        require(destination.canonicalFile != base.canonicalFile && destination.canonicalFile != patch.canonicalFile)
        check(!cancelled()) { "Mod patch cancelled" }
        require(
            ModFiles.sha256(base, cancelled).equals(baseSha256, true),
        ) { "Wrong original game revision (SHA-256 mismatch)" }
        val parent = requireNotNull(destination.parentFile)
        check(parent.isDirectory || parent.mkdirs()) { "Cannot create edition storage" }
        val stage = File(parent, ".patch-${UUID.randomUUID()}")
        try {
            RandomAccessFile(patch, "r").use { input ->
                val header = ByteArray(4)
                input.readFully(header)
                val signature = String(header, Charsets.US_ASCII)
                when (signature) {
                    "PATC" -> {
                        require(input.readUnsignedByte() == 'H'.code) { "Invalid IPS header" }
                        require(base.length() <= maxOutputBytes && parent.usableSpace >= base.length() + RESERVE) {
                            "Not enough storage for the modified edition"
                        }
                        base.inputStream().use { ModFiles.copyBounded(it, stage, maxOutputBytes, cancelled) }
                        RandomAccessFile(stage, "rw").use { output ->
                            ips(input, output, parent, cancelled)
                        }
                    }
                    "BPS1" ->
                        RandomAccessFile(base, "r").use { original ->
                            RandomAccessFile(stage, "rw").use { output ->
                                bps(input, original, output, parent, cancelled)
                            }
                        }
                    else -> error("Unsupported patch format; only verified IPS and BPS patches are accepted")
                }
            }
            check(!cancelled()) { "Mod patch cancelled" }
            require(ModFiles.sha256(stage, cancelled).equals(targetSha256, true)) {
                "Modified game verification failed; original retained"
            }
            check(!destination.exists() && stage.renameTo(destination)) { "Cannot commit the modified edition" }
        } finally {
            stage.delete()
        }
    }

    private fun ips(
        input: RandomAccessFile,
        output: RandomAccessFile,
        parent: File,
        cancelled: () -> Boolean,
    ) {
        fun u16() = input.readUnsignedByte() * 256 + input.readUnsignedByte()

        fun u24() = input.readUnsignedByte().toLong() * 65536 + u16()
        while (true) {
            check(!cancelled()) { "Mod patch cancelled" }
            val offset = u24()
            if (offset == 0x454f46L) break
            val count = u16()
            val length = if (count == 0) u16() else count
            require(length > 0 && offset <= maxOutputBytes - length) { "IPS record exceeds permitted game size" }
            val growth = maxOf(0, offset + length - output.length())
            check(parent.usableSpace >= growth + RESERVE) { "Not enough storage for the modified edition" }
            output.seek(offset)
            if (count == 0) {
                val value = input.readUnsignedByte()
                output.write(ByteArray(length) { value.toByte() })
            } else {
                val bytes = ByteArray(length)
                input.readFully(bytes)
                output.write(bytes)
            }
        }
        when (input.length() - input.filePointer) {
            0L -> Unit
            3L -> {
                val length = u24()
                require(length in 1..maxOutputBytes) { "Invalid IPS truncation size" }
                check(parent.usableSpace >= maxOf(0, length - output.length()) + RESERVE) { "Not enough storage" }
                output.setLength(length)
            }
            else -> error("Trailing data after IPS patch")
        }
        output.fd.sync()
    }

    private fun bps(
        input: RandomAccessFile,
        base: RandomAccessFile,
        output: RandomAccessFile,
        parent: File,
        cancelled: () -> Boolean,
    ) {
        require(input.length() >= 19) { "Incomplete BPS patch" }
        val end = input.length() - 12

        fun number(): Long {
            var result = 0L
            var shift = 1L
            repeat(9) {
                require(input.filePointer < end) { "Truncated BPS command" }
                val value = input.readUnsignedByte()
                require((value and 127).toLong() <= (Long.MAX_VALUE - result) / shift) { "BPS integer overflow" }
                result += (value and 127) * shift
                if (value and 128 != 0) return result
                require(shift <= Long.MAX_VALUE / 128) { "BPS integer overflow" }
                shift *= 128
                require(result <= Long.MAX_VALUE - shift) { "BPS integer overflow" }
                result += shift
            }
            error("BPS integer too long")
        }
        require(number() == base.length()) { "Wrong BPS source size" }
        val targetLength = number()
        require(targetLength in 1..maxOutputBytes) { "BPS target exceeds permitted game size" }
        check(parent.usableSpace >= targetLength + RESERVE) { "Not enough storage for the modified edition" }
        val metadataSize = number()
        require(metadataSize <= end - input.filePointer) { "Truncated BPS metadata" }
        input.seek(input.filePointer + metadataSize)
        var written = 0L
        var sourceRelative = 0L
        var targetRelative = 0L
        val buffer = ByteArray(64 * 1024)

        fun copy(
            source: RandomAccessFile,
            offset: Long,
            size: Long,
            overlapping: Boolean,
        ) {
            require(offset >= 0)
            var readAt = offset
            var remaining = size
            while (remaining > 0) {
                check(!cancelled()) { "Mod patch cancelled" }
                val available = if (overlapping) written - readAt else source.length() - readAt
                require(available > 0) { "BPS copy references unavailable data" }
                val count = minOf(remaining, available, buffer.size.toLong()).toInt()
                source.seek(readAt)
                source.readFully(buffer, 0, count)
                output.seek(written)
                output.write(buffer, 0, count)
                written += count
                readAt += count
                remaining -= count
            }
        }

        fun relative(current: Long): Long {
            val encoded = number()
            val magnitude = encoded ushr 1
            return if (encoded and 1 == 0L) {
                require(current <= Long.MAX_VALUE - magnitude) { "BPS relative offset overflow" }
                current + magnitude
            } else {
                require(current >= magnitude) { "BPS negative copy offset" }
                current - magnitude
            }
        }
        while (written < targetLength) {
            check(!cancelled()) { "Mod patch cancelled" }
            val command = number()
            val length = (command ushr 2) + 1
            require(length <= targetLength - written) { "BPS command exceeds target size" }
            when ((command and 3).toInt()) {
                0 -> {
                    require(written <= base.length() - length) { "BPS source read exceeds original game" }
                    copy(base, written, length, false)
                }
                1 -> {
                    require(length <= end - input.filePointer) { "BPS literal exceeds patch size" }
                    var remaining = length
                    while (remaining > 0) {
                        check(!cancelled()) { "Mod patch cancelled" }
                        val count = minOf(remaining, buffer.size.toLong()).toInt()
                        input.readFully(buffer, 0, count)
                        output.seek(written)
                        output.write(buffer, 0, count)
                        written += count
                        remaining -= count
                    }
                }
                2 -> {
                    sourceRelative = relative(sourceRelative)
                    require(sourceRelative <= base.length() - length) { "BPS source copy exceeds original game" }
                    copy(base, sourceRelative, length, false)
                    sourceRelative += length
                }
                3 -> {
                    targetRelative = relative(targetRelative)
                    require(targetRelative < written) { "BPS target copy references unwritten data" }
                    copy(output, targetRelative, length, true)
                    targetRelative += length
                }
            }
        }
        require(input.filePointer == end) { "Trailing BPS commands" }

        fun u32(): Long {
            var value = 0L
            repeat(4) { value = value or (input.readUnsignedByte().toLong() shl (it * 8)) }
            return value
        }
        val sourceCrc = u32()
        val targetCrc = u32()
        val patchCrc = u32()
        require(crc(base, base.length(), cancelled) == sourceCrc) { "BPS original game checksum mismatch" }
        require(crc(output, output.length(), cancelled) == targetCrc) { "BPS modified game checksum mismatch" }
        require(crc(input, input.length() - 4, cancelled) == patchCrc) { "BPS patch checksum mismatch" }
        output.fd.sync()
    }

    private fun crc(
        file: RandomAccessFile,
        size: Long,
        cancelled: () -> Boolean,
    ): Long {
        file.seek(0)
        var remaining = size
        val crc = CRC32()
        val buffer = ByteArray(64 * 1024)
        while (remaining > 0) {
            check(!cancelled()) { "Mod patch cancelled" }
            val count = minOf(remaining, buffer.size.toLong()).toInt()
            file.readFully(buffer, 0, count)
            crc.update(buffer, 0, count)
            remaining -= count
        }
        return crc.value
    }

    private companion object {
        const val RESERVE = 64L * 1024 * 1024
    }
}
