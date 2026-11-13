package com.swordfish.lemuroid.lib.library

import com.swordfish.lemuroid.lib.storage.scanner.NextGenDiscIdentity
import java.util.Locale

/** Same-directory references only. Never guess between case-colliding source files. */
object DiscReferences {
    fun safeName(name: String): Boolean =
        name.isNotBlank() && name.length <= 255 && name !in setOf(".", "..") &&
            name.none { it == '/' || it == '\\' || it == ':' || it.isISOControl() }

    fun resolve(
        requested: String,
        available: Collection<String>,
    ): String? {
        if (!safeName(requested)) return null
        if (requested in available) return requested
        return available.filter { it.equals(requested, ignoreCase = true) }.singleOrNull()
    }

    fun parse(
        name: String,
        text: String,
    ): List<String>? =
        when (name.substringAfterLast('.', "").lowercase(Locale.US)) {
            "gdi" -> NextGenDiscIdentity.probe(name, gdiText = text).takeIf { it.platform != null }?.companionTracks
            "m3u" -> DiscPlaylist.parse(text)
            "cue" -> cue(text)
            else -> null
        }

    fun cue(text: String): List<String>? {
        if (text.length !in 1..65536) return null
        val clean = text.removePrefix("\uFEFF")
        val lines = Regex("(?im)^\\s*FILE\\b[^\\r\\n]*").findAll(clean).toList()
        val pattern = Regex("""\s*FILE\s+(?:"([^"]+)"|(\S+))\s+\S+\s*""", RegexOption.IGNORE_CASE)
        val names =
            lines.map { line ->
                val match = pattern.matchEntire(line.value) ?: return null
                match.groupValues[1].ifEmpty { match.groupValues[2] }
            }
        return names.distinct().takeIf { it.isNotEmpty() && it.size <= 99 && it.all(::safeName) }
    }
}
