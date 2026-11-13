package com.swordfish.lemuroid.lib.storage.local

import com.swordfish.lemuroid.lib.library.DiscReferences
import java.io.File
import java.io.IOException
import java.io.InputStream

/** Stages the reachable descriptor graph, preserving its exact references and all source bytes. */
object DiscFamilyStager {
    data class Source(val size: Long, val modifiedAt: Long, val open: () -> InputStream)

    fun stage(
        primary: String,
        sources: Map<String, Source>,
        directory: File,
    ): List<File> {
        val planned = linkedMapOf<String, Pair<String, Source>>()
        val active = mutableSetOf<String>()

        fun visit(
            requested: String,
            depth: Int,
        ) {
            if (depth > 2 || planned.size >= 512) {
                throw IOException(
                    "Disc family exceeds the supported flat playlist limits",
                )
            }
            val actual =
                DiscReferences.resolve(requested, sources.keys)
                    ?: throw IOException("Missing/ambiguous companion: $requested. Restore files and refresh.")
            if (requested in planned) return
            if (!active.add(actual)) throw IOException("Cyclic disc descriptor; restore the original playlist")
            if (planned.keys.any { it.equals(requested, true) && planned[it]?.first != actual }) {
                throw IOException("Conflicting disc companion names; use separate disc folders")
            }
            val source = sources.getValue(actual)
            val extension = requested.substringAfterLast('.', "").lowercase(java.util.Locale.US)
            if (extension in setOf("gdi", "cue", "m3u")) {
                val text =
                    source.open().use { input ->
                        val bytes = ByteArray(65537)
                        var count = 0
                        while (count < bytes.size) {
                            val read = input.read(bytes, count, bytes.size - count)
                            if (read <= 0) break
                            count += read
                        }
                        if (count > 65536) throw IOException("Disc descriptor is too large")
                        String(bytes, 0, count, Charsets.UTF_8)
                    }
                val references =
                    DiscReferences.parse(requested, text)
                        ?: throw IOException("Malformed disc descriptor; restore the original GDI/CUE/M3U and tracks")
                references.forEach { visit(it, depth + 1) }
            }
            active.remove(actual)
            planned[requested] = actual to source
        }
        visit(primary, 0)
        // Dependencies finish first. Never hand an incomplete primary to Flycast.
        planned.forEach { (name, entry) ->
            CachedRomStager.stage(
                File(directory, name),
                entry.second.size,
                entry.second.modifiedAt,
                openSource = entry.second.open,
            )
        }
        return listOf(File(directory, primary)) + planned.keys.filter { it != primary }.map { File(directory, it) }
    }
}
