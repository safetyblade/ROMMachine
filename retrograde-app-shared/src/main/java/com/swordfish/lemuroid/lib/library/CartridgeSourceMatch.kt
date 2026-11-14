package com.swordfish.lemuroid.lib.library

import android.net.Uri
import com.swordfish.lemuroid.lib.library.db.entity.Game

/**
 * High-confidence source relocation only. A SAF document ID is provider-specific;
 * the same opaque document under a different granted tree can retain its cartridge.
 * Do not infer identity from ROM titles, basenames, file sizes, regions or short hashes.
 */
object CartridgeSourceMatch {
    data class Relocation(
        val gameId: Int,
        val cartridgeId: String,
        val systemId: String,
        val originalUri: String,
        val currentUri: String,
    )

    private fun documentIdentity(source: String): Pair<String, String>? {
        val uri = runCatching { Uri.parse(source) }.getOrNull() ?: return null
        if (uri.scheme != "content") return null
        val authority = uri.authority ?: return null
        if (!authority.endsWith(".documents")) return null
        val parts = uri.pathSegments
        val documentIndex = parts.indexOf("document")
        if (documentIndex < 0 || parts.size <= documentIndex + 1) return null
        val documentId = parts[documentIndex + 1]
        if (documentId.isBlank()) return null
        return authority to documentId
    }

    /** Return only unique old-to-new matches after a fully enumerated provider scan. */
    fun uniqueRelocations(
        before: List<Game>,
        after: List<Game>,
        scanMarker: Long,
    ): List<Relocation> {
        val existingIds = before.map { it.id }.toSet()
        val seenIds = after.filter { it.lastIndexedAt == scanMarker }.map { it.id }.toSet()
        val vanished = before.filter { it.id !in seenIds }
        val arrivals = after.filter { it.lastIndexedAt == scanMarker && it.id !in existingIds }
        val candidates =
            vanished.flatMap { previous ->
                val document = documentIdentity(previous.fileUri) ?: return@flatMap emptyList()
                arrivals.filter { next ->
                    next.systemId == previous.systemId && documentIdentity(next.fileUri) == document
                }.map { previous to it }
            }
        val uniqueOldIds = candidates.groupingBy { it.first.id }.eachCount()
        val uniqueNewIds = candidates.groupingBy { it.second.id }.eachCount()
        return candidates
            .filter { (old, new) -> uniqueOldIds[old.id] == 1 && uniqueNewIds[new.id] == 1 }
            .map { (old, new) ->
                Relocation(old.id, old.cartridgeId, old.systemId, old.fileUri, new.fileUri)
            }
    }
}
