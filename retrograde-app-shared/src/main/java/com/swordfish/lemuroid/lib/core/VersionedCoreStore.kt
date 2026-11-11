package com.swordfish.lemuroid.lib.core

import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * Local core-version store shared by normal gameplay and developer runtime probes.
 * Does not download or package any emulator.
 * SHA-256 integrity is NOT a licence or authenticity check. Existing saves are outside
 * this directory and are never deleted during core installation or rollback.
 */
class VersionedCoreStore(
    private val root: File,
) {
    data class Selection(
        val version: String,
        val coreFile: File,
        val sha256: String,
        val usedFallback: Boolean,
    )

    @Synchronized
    fun install(
        platform: String,
        version: String,
        source: File,
        expectedSha256: String,
    ): File {
        validateSegment(platform)
        validateSegment(version)
        require(expectedSha256.matches(Regex("[0-9a-fA-F]{64}"))) { "Invalid SHA-256" }
        require(source.isFile) { "Core input is missing" }
        val sha = expectedSha256.lowercase()
        require(hash(source) == sha) { "Source SHA-256 mismatch" }
        val directory = File(File(File(root, platform), "versions"), version)
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create core version directory" }
        val target = File(directory, "core.so")
        if (target.exists()) {
            require(hash(target) == sha) { "Core version is immutable; checksum mismatch" }
        } else {
            val temporary = File(directory, ".core-pending")
            try {
                source.inputStream().use { input ->
                    temporary.outputStream().use { output -> input.copyTo(output) }
                }
                require(hash(temporary) == sha) { "Staged SHA-256 mismatch" }
                if (!temporary.renameTo(target)) throw IOException("Core install rename failed")
            } finally {
                temporary.delete()
            }
        }
        File(directory, "sha256.txt").writeText("$sha\n")
        return target
    }

    @Synchronized
    fun activate(
        platform: String,
        version: String,
    ): Boolean {
        validateSegment(platform)
        validateSegment(version)
        if (verified(platform, version) == null) return false
        val previous = pointer(platform, "active.version")
        if (previous == version) return true
        if (previous != null && verified(platform, previous) != null) {
            writePointer(platform, "previous.version", previous)
        }
        writePointer(platform, "active.version", version)
        return true
    }

    @Synchronized
    fun select(platform: String): Selection? {
        validateSegment(platform)
        val active = pointer(platform, "active.version")
        val candidates = listOfNotNull(active, pointer(platform, "previous.version")).distinct()
        for (candidate in candidates) {
            val found = verified(platform, candidate) ?: continue
            return found.copy(usedFallback = candidate != active)
        }
        return null
    }

    @Synchronized
    fun rollback(platform: String): Boolean {
        validateSegment(platform)
        val previous = pointer(platform, "previous.version") ?: return false
        if (verified(platform, previous) == null) return false
        val current = pointer(platform, "active.version")
        if (current != null && verified(platform, current) != null) {
            writePointer(platform, "previous.version", current)
        }
        writePointer(platform, "active.version", previous)
        return true
    }

    private fun verified(
        platform: String,
        version: String,
    ): Selection? {
        if (!validSegment(version)) return null
        val dir = File(File(File(root, platform), "versions"), version)
        val core = File(dir, "core.so")
        val expected = File(dir, "sha256.txt").takeIf { it.isFile }?.readText()?.trim()
        if (expected == null || !expected.matches(Regex("[0-9a-f]{64}")) || !core.isFile) {
            return null
        }
        return if (hash(core) == expected) {
            Selection(version, core, expected, false)
        } else {
            null
        }
    }

    private fun pointer(
        platform: String,
        name: String,
    ): String? {
        val file = File(File(root, platform), name)
        val value = file.takeIf { it.isFile }?.readText()?.trim()
        return value?.takeIf(::validSegment)
    }

    private fun writePointer(
        platform: String,
        name: String,
        value: String,
    ) {
        val dir = File(root, platform)
        check(dir.isDirectory || dir.mkdirs()) { "Cannot create core pointer directory" }
        val temp = File(dir, ".$name.pending")
        try {
            temp.writeText("$value\n")
            if (!temp.renameTo(File(dir, name))) throw IOException("Core pointer update failed")
        } finally {
            temp.delete()
        }
    }

    private fun validateSegment(value: String) {
        require(validSegment(value)) { "Invalid core version/platform identifier" }
    }

    private fun validSegment(value: String): Boolean =
        value.length <= 64 && value.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]*"))

    private fun hash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
