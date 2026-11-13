package com.swordfish.lemuroid.lib.library

/** Flat, same-directory disc references accepted by the scanner and game loader. */
object DiscPlaylist {
    const val MAX_BYTES = 65536L

    fun parse(text: String): List<String>? {
        if (text.length > MAX_BYTES) return null
        val entries =
            text.removePrefix("\uFEFF").lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .toList()
        if (entries.size !in 1..99) return null
        if (entries.any { name ->
                name in setOf(".", "..") || name.length > 255 ||
                    name.any { it == '/' || it == '\\' || it == ':' || it.isISOControl() } ||
                    name.substringAfterLast('.', "").lowercase(java.util.Locale.US) in setOf("m3u", "m3u8")
            }
        ) {
            return null
        }
        return entries
    }

    fun containsAllDiscs(
        text: String,
        availableNames: Set<String>,
    ): Boolean = parse(text)?.all { DiscReferences.resolve(it, availableNames) != null } == true
}
