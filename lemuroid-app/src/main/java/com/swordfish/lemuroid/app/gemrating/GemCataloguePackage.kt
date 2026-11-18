package com.swordfish.lemuroid.app.gemrating

import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.security.MessageDigest

/**
 * Offline package boundary. Approval is supplied by trusted application configuration,
 * never by a package's self-declared licence or checksum. A successful load does not
 * change the active catalogue on disk, player data, or the scanner.
 */
object GemCataloguePackage {
    const val MAX_BYTES = 32 * 1024 * 1024
    const val MAX_RELEASES = 100_000

    @Serializable
    data class Source(
        val id: String,
        val name: String,
        val url: String,
        val licence: String,
        val attribution: String,
        val revision: String,
    )

    @Serializable
    data class Release(
        val releaseId: String,
        val systemId: String,
        val title: String,
        val sourceId: String,
        val sourceRecordId: String,
        val primaryGenre: String? = null,
        val verifiedSha256: Set<String> = emptySet(),
        val verifiedSerials: Set<String> = emptySet(),
    )

    @Serializable
    data class Manifest(
        val schemaVersion: Int,
        val catalogueVersion: String,
        val releaseCount: Int,
        val sources: List<Source>,
        val releases: List<Release>,
    )

    /** Trusted digest and pinned source declarations, supplied independently of import bytes. */
    data class Approval(
        val version: String,
        val sha256: String,
        val sources: List<Source>,
    )

    class Loaded internal constructor(
        val manifest: Manifest,
        val index: GemCatalogueIndex,
    ) {
        fun match(game: Game, evidence: GemCatalogueIndex.Evidence = GemCatalogueIndex.Evidence()): GemCatalogueIndex.Match =
            index.match(game, evidence)
    }

    private val json = Json { ignoreUnknownKeys = false }
    private val systems = SystemID.values().map { it.dbname }.toSet()

    fun load(input: InputStream, approval: Approval): Loaded {
        val bytes = input.readNBytesCompat(MAX_BYTES + 1)
        require(bytes.size <= MAX_BYTES) { "Catalogue exceeds size limit" }
        require(approval.sha256.matches(Regex("[a-f0-9]{64}"))) { "Invalid trusted digest" }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
        require(digest == approval.sha256) { "Catalogue digest is not approved" }
        val manifest = json.decodeFromString(Manifest.serializer(), bytes.decodeToString(throwOnInvalidSequence = true))
        require(manifest.schemaVersion == 1) { "Unsupported schema" }
        require(manifest.catalogueVersion == approval.version && manifest.catalogueVersion.isNotBlank())
        require(manifest.releaseCount == manifest.releases.size && manifest.releaseCount in 0..MAX_RELEASES)
        require(manifest.sources.map { it.id }.distinct().size == manifest.sources.size)
        require(manifest.sources.sortedBy { it.id } == approval.sources.sortedBy { it.id }) { "Unapproved source declaration" }
        require(manifest.sources.all {
            it.id.isNotBlank() && it.name.isNotBlank() && it.url.startsWith("https://") &&
                it.licence.isNotBlank() && it.attribution.isNotBlank() && it.revision.isNotBlank()
        })
        val sourceIds = manifest.sources.map { it.id }.toSet()
        require(manifest.releases.all {
            it.sourceId in sourceIds && it.sourceRecordId.isNotBlank() &&
                it.systemId in systems && it.title.length in 1..512 && it.releaseId.length in 1..256 &&
                (it.primaryGenre?.length ?: 0) <= 128 &&
                it.verifiedSerials.size <= 32 && it.verifiedSha256.size <= 32 &&
                it.verifiedSerials.all { serial -> serial.length in 1..128 }
        }) { "Invalid release or source" }
        require(manifest.releases.map { it.sourceId to it.sourceRecordId }.distinct().size == manifest.releaseCount)
        val index = GemCatalogueIndex(
            manifest.catalogueVersion,
            manifest.releases.map {
                GemCatalogueIndex.Release(it.releaseId, it.systemId, it.title, it.verifiedSha256, it.verifiedSerials, it.primaryGenre)
            },
        )
        return Loaded(manifest, index)
    }

    private fun InputStream.readNBytesCompat(limit: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (output.size() < limit) {
            val count = read(buffer, 0, minOf(buffer.size, limit - output.size()))
            if (count < 0) break
            if (count == 0) {
                val byte = read()
                if (byte < 0) break
                output.write(byte)
            } else {
                output.write(buffer, 0, count)
            }
        }
        return output.toByteArray()
    }
}
