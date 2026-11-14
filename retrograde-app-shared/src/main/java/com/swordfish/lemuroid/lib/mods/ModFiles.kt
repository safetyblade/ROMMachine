package com.swordfish.lemuroid.lib.mods

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/** File primitives shared by import, patch and deployment. Never accept a user supplied destination. */
object ModFiles {
    fun sha256(
        file: File,
        cancelled: () -> Boolean = { false },
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                check(!cancelled()) { "Mod verification cancelled" }
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }

    fun relativePath(path: String): String {
        require(path.isNotEmpty() && path.length <= 1024) { "Invalid mod path length" }
        require(
            !path.startsWith('/') && !path.contains('\\') && !path.contains(':'),
        ) { "Absolute mod paths are not allowed" }
        require(path.none { it.code < 32 || it.code == 127 }) { "Control characters are not allowed in mod paths" }
        val parts = path.removeSuffix("/").split('/')
        require(parts.all { it.isNotBlank() && it != "." && it != ".." }) { "Unsafe mod path: $path" }
        return parts.joinToString("/")
    }

    fun copyBounded(
        source: InputStream,
        destination: File,
        limit: Long,
        cancelled: () -> Boolean = { false },
    ): Long {
        require(limit >= 0)
        var total = 0L
        destination.outputStream().use { output ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                check(!cancelled()) { "Mod import cancelled" }
                val count = source.read(buffer)
                if (count < 0) break
                require(count.toLong() <= limit - total) { "Mod exceeds the storage limit" }
                total += count
                output.write(buffer, 0, count)
            }
            output.fd.sync()
        }
        return total
    }
}
