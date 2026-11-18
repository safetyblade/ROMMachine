package com.swordfish.lemuroid.app.gemrating

import kotlinx.serialization.Serializable
import java.util.UUID

/** Profile authority is supplied by the host; GEM never creates or switches player sessions. */
class GemRatingRepository(
    private val activeSession: () -> Session?,
    private val storage: Storage,
    private val now: () -> Long = System::currentTimeMillis,
) {
    enum class Role { MANAGER, STANDARD, JUNIOR }

    data class Session(val profileId: String, val role: Role)

    @Serializable
    data class Rating(
        val cartridgeId: String,
        val score: Int?,
        val revision: Long,
        val updatedAt: Long,
        val displayTitle: String? = null,
        val platformId: String? = null,
    )

    @Serializable
    data class Snapshot(
        val profileId: String,
        val schemaVersion: Int = 1,
        val records: List<Rating> = emptyList(),
    )

    interface Storage {
        fun read(profileId: String): Snapshot

        fun write(snapshot: Snapshot)
    }

    fun snapshot(expected: Session): Snapshot =
        synchronized(lock) {
            verifyAccess(expected)
            readValidated(expected.profileId)
        }

    fun score(
        expected: Session,
        cartridgeId: String,
    ): Int? {
        requireUuid(cartridgeId)
        return snapshot(expected).records.firstOrNull { it.cartridgeId == cartridgeId }?.score
    }

    /** Null is an explicit clear/tombstone, not a zero rating or deletion of the cartridge. */
    fun set(
        expected: Session,
        cartridgeId: String,
        score: Int?,
        displayTitle: String? = null,
        platformId: String? = null,
    ): Snapshot =
        synchronized(lock) {
            verifyAccess(expected)
            requireUuid(cartridgeId)
            require(score == null || score in 1..10)
            require(displayTitle == null || displayTitle.length in 1..512)
            require(platformId == null || platformId.length in 1..64)
            val existing = readValidated(expected.profileId)
            val previous = existing.records.firstOrNull { it.cartridgeId == cartridgeId }
            if (previous?.score == score) return@synchronized existing
            val revision = previous?.revision ?: 0L
            require(revision < Long.MAX_VALUE)
            val timestamp = now()
            require(timestamp >= 0)
            val rating =
                Rating(
                    cartridgeId = cartridgeId,
                    score = score,
                    revision = revision + 1,
                    updatedAt = timestamp,
                    displayTitle = displayTitle ?: previous?.displayTitle,
                    platformId = platformId ?: previous?.platformId,
                )
            val next =
                existing.copy(
                    records =
                        (existing.records.filterNot { it.cartridgeId == cartridgeId } + rating)
                            .sortedBy { it.cartridgeId },
                )
            verifyAccess(expected)
            storage.write(next)
            next
        }

    private fun verifyAccess(expected: Session) {
        requireUuid(expected.profileId)
        check(expected.role != Role.JUNIOR && expected == activeSession()) { "GEM unavailable for this player" }
    }

    private fun readValidated(profileId: String): Snapshot {
        val snapshot = storage.read(profileId)
        require(snapshot.profileId == profileId && snapshot.schemaVersion == 1)
        require(snapshot.records.map { it.cartridgeId }.distinct().size == snapshot.records.size)
        snapshot.records.forEach {
            requireUuid(it.cartridgeId)
            require(it.score == null || it.score in 1..10)
            require(it.revision > 0 && it.updatedAt >= 0)
            require(it.displayTitle == null || it.displayTitle.length in 1..512)
            require(it.platformId == null || it.platformId.length in 1..64)
        }
        return snapshot
    }

    companion object {
        private val lock = Any()

        internal fun requireUuid(value: String) {
            require(UUID.fromString(value).toString() == value) { "Expected canonical UUID" }
        }
    }
}
