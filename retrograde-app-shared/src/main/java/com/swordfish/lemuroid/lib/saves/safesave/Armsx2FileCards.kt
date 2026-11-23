package com.swordfish.lemuroid.lib.saves.safesave

import java.io.File

/** File-backed ARMSX2 cards under the native host's private cartridge root. */
object Armsx2FileCards {
    const val ENGINE_ID = "armsx2"
    const val FORMAT = "ps2-raw-ecc-528-v1"

    private const val ROOT = "native/ps2/armsx2/cartridges"
    private val cardNames = listOf("Mcd001.ps2", "Mcd002.ps2")
    private val magic = "Sony PS2 Memory Card Format ".toByteArray(Charsets.US_ASCII)
    private val cardSizes = setOf(8L, 16L, 32L, 64L).map { it * 1024 * 528 * 2 }.toSet()

    fun discover(
        filesDir: File,
        cartridgeId: String,
        includeMissing: Boolean = false,
    ): List<File> {
        val directory = directory(filesDir, requireCartridgeUuid(cartridgeId))
        if (!directory.isDirectory || directory != directory.canonicalFile) return emptyList()
        val ini = File(directory.parentFile, "PCSX2-Android.ini")
        if (ini.exists() && (ini != ini.canonicalFile || !ini.isFile || ini.length() > 1024 * 1024)) {
            return emptyList()
        }
        val settings = if (ini.isFile) cardSettings(ini) else emptyMap()
        val configuredFolder = settings["folders/memorycards"]
        if (configuredFolder != null && configuredFolder !in setOf("memcards", directory.path)) return emptyList()
        return cardNames.mapIndexedNotNull { index, name ->
            val slot = index + 1
            val enabled = settings["memorycards/slot${slot}_enable"]?.lowercase()
            if (enabled != null && enabled !in setOf("true", "1")) return@mapIndexedNotNull null
            val configuredName = settings["memorycards/slot${slot}_filename"]
            if (configuredName != null && configuredName != name) return@mapIndexedNotNull null
            val file = File(directory, name)
            file.takeIf { it == it.canonicalFile && (isFormattedCard(it) || (!it.exists() && includeMissing)) }
        }
    }

    fun isFormattedCard(file: File): Boolean {
        if (!file.isFile || file != file.canonicalFile || file.length() !in cardSizes) return false
        return file.inputStream().use { input ->
            val signature = ByteArray(magic.size)
            var offset = 0
            while (offset < signature.size) {
                val read = input.read(signature, offset, signature.size - offset)
                if (read < 1) return@use false
                offset += read
            }
            signature.contentEquals(magic)
        }
    }

    internal fun isPrivateCardPath(
        filesDir: File,
        file: File,
    ): Boolean {
        if (file.name !in cardNames) return false
        val gameRoot = file.parentFile?.parentFile ?: return false
        val cartridgeId = runCatching { requireCartridgeUuid(gameRoot.name) }.getOrNull() ?: return false
        return file == File(directory(filesDir, cartridgeId), file.name)
    }

    private fun directory(
        filesDir: File,
        cartridgeId: String,
    ) = File(filesDir.canonicalFile, "$ROOT/$cartridgeId/memcards")

    private fun cardSettings(file: File): Map<String, String> =
        buildMap {
            var section = ""
            file.forEachLine { raw ->
                val line = raw.trim()
                if (line.startsWith('[') && line.endsWith(']')) {
                    section = line.substring(1, line.length - 1).lowercase()
                } else if (section in setOf("folders", "memorycards") && !line.startsWith(';') && '=' in line) {
                    val key = line.substringBefore('=').trim().lowercase()
                    put("$section/$key", line.substringAfter('=').trim())
                }
            }
        }
}
