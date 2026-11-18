package com.swordfish.lemuroid.app.gemrating

import com.swordfish.lemuroid.lib.library.db.entity.Game
import java.util.Locale

/**
 * GEM associations supplement the installed cartridge; they never define its identity.
 * No scanner, Room, save-binding, network or filesystem mutations are performed here.
 */
class GemCatalogueIndex(
    val version: String,
    releases: List<Release>,
) {
    data class Release(
        val releaseId: String,
        val systemId: String,
        val title: String,
        val verifiedSha256: Set<String> = emptySet(),
        val verifiedSerials: Set<String> = emptySet(),
        val primaryGenre: String? = null,
    )

    data class Evidence(
        val sha256: String? = null,
        val serial: String? = null,
    )

    sealed interface Match {
        data class Confirmed(val release: Release) : Match

        data class Review(val candidates: List<Release>) : Match

        data object Unmatched : Match
    }

    private fun normalized(value: String): String = value.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")

    private fun validHash(value: String): Boolean = value.matches(Regex("[a-fA-F0-9]{64}"))

    private val records =
        releases.map {
            it.copy(
                verifiedSha256 = it.verifiedSha256.map { hash -> hash.lowercase(Locale.ROOT) }.toSet(),
                verifiedSerials = it.verifiedSerials.map(::normalized).toSet(),
            )
        }

    init {
        require(version.isNotBlank())
        require(records.map { it.releaseId }.distinct().size == records.size)
        require(records.all { it.releaseId.isNotBlank() && it.systemId.isNotBlank() && it.title.isNotBlank() })
        require(records.all { release -> release.verifiedSha256.all(::validHash) })
        require(records.all { release -> release.verifiedSerials.none { it.isBlank() } })
    }

    private val byId = records.associateBy { it.releaseId }
    private val bySystem = records.groupBy { it.systemId }

    fun find(releaseId: String): Release? = byId[releaseId]

    /**
     * Only verified, unique, platform-scoped fingerprints/serials may confirm automatically.
     * Contradictory evidence requires review. Title matching always remains a suggestion.
     * Existing local corrections are resolved separately and take precedence.
     */
    fun match(
        game: Game,
        evidence: Evidence = Evidence(),
    ): Match {
        val pool = bySystem[game.systemId].orEmpty()
        val hash = evidence.sha256?.takeIf(::validHash)?.lowercase(Locale.ROOT)
        val serial = evidence.serial?.let(::normalized)?.takeIf { it.isNotEmpty() }
        val hashes = if (hash == null) emptyList() else pool.filter { hash in it.verifiedSha256 }
        val serials = if (serial == null) emptyList() else pool.filter { serial in it.verifiedSerials }
        val strong = (hashes + serials).distinctBy { it.releaseId }
        // Where both evidence types are supplied, an unresolved or contradictory one
        // cannot silently be ignored. A known candidate remains reviewable.
        if (strong.isNotEmpty()) {
            val conflict =
                (evidence.sha256 != null && hash == null) ||
                    (hash != null && serial != null &&
                        (hashes.size != 1 || serials.size != 1 || hashes.single().releaseId != serials.single().releaseId))
            return if (strong.size == 1 && !conflict) {
                Match.Confirmed(strong.single())
            } else {
                Match.Review(strong.sortedBy { it.releaseId })
            }
        }
        val titles = pool.filter { normalized(it.title) == normalized(game.title) }.sortedBy { it.releaseId }
        return if (titles.isEmpty()) Match.Unmatched else Match.Review(titles)
    }
}
