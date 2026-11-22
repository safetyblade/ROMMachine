package com.swordfish.lemuroid.app.gemrating

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Small offline connector/Roxy contribution boundary. This file never modifies a
 * release ID, installed cartridge, player GemRating or save binding. Invalid rows
 * are reported without discarding valid editorial additions.
 */
internal object GemFeatureContributions {
    private const val MAX_BYTES = 1024 * 1024
    private const val MAX_ROWS = 2000
    private val json = Json { ignoreUnknownKeys = false }

    @Serializable
    data class Record(
        val releaseId: String,
        val title: String,
        val primaryGenre: String? = null,
        val crossoverGenre: String? = null,
        val playstyle: String? = null,
        val mechanics: List<String>? = null,
        val sourceId: String? = null,
    )

    @Serializable
    data class Batch(
        val schemaVersion: Int,
        val records: List<Record>,
    )

    data class Rejected(val releaseId: String, val reason: String)

    data class Report(
        val accepted: Map<String, Record>,
        val rejected: List<Rejected>,
    )

    fun load(
        input: InputStream,
        releases: List<GemCataloguePackage.Release>,
    ): Report {
        val bytes = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val size = input.read(buffer)
            if (size < 0) break
            if (size == 0) continue
            require(bytes.size() + size <= MAX_BYTES) { "Feature pack exceeds size limit" }
            bytes.write(buffer, 0, size)
        }
        return parse(bytes.toString(Charsets.UTF_8.name()), releases)
    }

    fun parse(
        raw: String,
        releases: List<GemCataloguePackage.Release>,
    ): Report {
        val batch = json.decodeFromString(Batch.serializer(), raw)
        require(batch.schemaVersion == 1) { "Unsupported feature pack schema" }
        require(batch.records.size <= MAX_ROWS) { "Feature pack exceeds row limit" }
        val known = releases.associateBy { it.releaseId }
        val seen = mutableSetOf<String>()
        val accepted = linkedMapOf<String, Record>()
        val rejected = mutableListOf<Rejected>()
        for (row in batch.records) {
            val release = known[row.releaseId]
            val values = listOfNotNull(
                row.primaryGenre, row.crossoverGenre, row.playstyle, row.sourceId,
            ) + row.mechanics.orEmpty()
            val reason =
                when {
                    !seen.add(row.releaseId) -> "Duplicate release ID in contribution"
                    release == null -> "Release ID not in active catalogue"
                    release.title != row.title -> "Title does not match exact release ID"
                    values.any { it.isBlank() || it.length > 96 } -> "Invalid feature field"
                    row.mechanics.orEmpty().size > 16 -> "Too many mechanics"
                    values.isEmpty() -> "No gameplay features supplied"
                    else -> null
                }
            if (reason != null) {
                rejected += Rejected(row.releaseId, reason)
            } else {
                accepted[row.releaseId] =
                    row.copy(
                        primaryGenre = row.primaryGenre?.trim(),
                        crossoverGenre = row.crossoverGenre?.trim(),
                        playstyle = row.playstyle?.trim(),
                        mechanics = row.mechanics?.map { it.trim() }?.distinct(),
                        sourceId = row.sourceId?.trim(),
                    )
            }
        }
        return Report(accepted, rejected)
    }
}
