package com.swordfish.lemuroid.app.gemrating

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.MessageDigest

/** Portable personal metadata only. No ROM, emulator save, path or account credential. */
object GemProfileBackup {
    const val MAX_BYTES = 16 * 1024 * 1024
    private const val FORMAT = "cartdeck.gem.profile"
    private val json = Json { ignoreUnknownKeys = false }

    @Serializable
    private data class Envelope(
        val format: String,
        val schemaVersion: Int,
        val payload: String,
        val sha256: String,
    )

    data class Preview(
        val local: GemRatingRepository.Snapshot,
        val source: GemRatingRepository.Snapshot,
        val additions: Int,
        val conflicts: Int,
        val unchanged: Int,
    )

    fun encode(snapshot: GemRatingRepository.Snapshot): ByteArray {
        GemRatingRepository.validateSnapshot(snapshot)
        val payload = json.encodeToString(GemRatingRepository.Snapshot.serializer(), snapshot)
        val envelope = Envelope(FORMAT, 1, payload, digest(payload.encodeToByteArray()))
        return json.encodeToString(Envelope.serializer(), envelope).encodeToByteArray().also {
            require(it.size <= MAX_BYTES)
        }
    }

    fun decode(bytes: ByteArray): GemRatingRepository.Snapshot {
        require(bytes.size in 1..MAX_BYTES)
        val envelope = json.decodeFromString(Envelope.serializer(), bytes.decodeToString(throwOnInvalidSequence = true))
        require(envelope.format == FORMAT && envelope.schemaVersion == 1)
        require(envelope.sha256 == digest(envelope.payload.encodeToByteArray())) { "Backup integrity check failed" }
        return json.decodeFromString(GemRatingRepository.Snapshot.serializer(), envelope.payload).also {
            GemRatingRepository.validateSnapshot(it)
        }
    }

    fun read(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) {
                val byte = input.read()
                if (byte < 0) break
                output.write(byte)
            } else {
                output.write(buffer, 0, count)
            }
            require(output.size() <= MAX_BYTES)
        }
        return output.toByteArray()
    }

    fun preview(
        local: GemRatingRepository.Snapshot,
        source: GemRatingRepository.Snapshot,
    ): Preview {
        GemRatingRepository.validateSnapshot(local)
        GemRatingRepository.validateSnapshot(source)
        val existing = local.records.associateBy { it.cartridgeId }
        val additions = source.records.count { it.cartridgeId !in existing }
        val unchanged = source.records.count { existing[it.cartridgeId] == it }
        return Preview(local, source, additions, source.records.size - additions - unchanged, unchanged)
    }

    internal fun merge(
        local: GemRatingRepository.Snapshot,
        source: GemRatingRepository.Snapshot,
        replaceConflicts: Boolean,
        restorePreferences: Boolean,
        now: Long,
    ): GemRatingRepository.Snapshot {
        require(now >= 0)
        val records = local.records.associateBy { it.cartridgeId }.toMutableMap()
        source.records.forEach { incoming ->
            val current = records[incoming.cartridgeId]
            if (current == null) {
                records[incoming.cartridgeId] = incoming
            } else if (current != incoming && replaceConflicts) {
                val revision = maxOf(current.revision, incoming.revision)
                require(revision < Long.MAX_VALUE)
                records[incoming.cartridgeId] = incoming.copy(revision = revision + 1, updatedAt = now)
            }
        }
        return local.copy(
            records = records.values.sortedBy { it.cartridgeId },
            preferences = if (restorePreferences) source.preferences ?: local.preferences else local.preferences,
        )
    }

    internal fun digest(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
            "%02x".format(it.toInt() and 255)
        }
}
