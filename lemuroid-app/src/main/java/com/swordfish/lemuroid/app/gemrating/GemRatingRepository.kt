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
        val deckFit: Int? = null,
        val catalogueReleaseId: String? = null,
    )

    @Serializable
    data class Preferences(
        val wildcardPercent: Int = 30,
        val recentSurprises: List<String> = emptyList(),
        val showShelfRatings: Boolean = false,
    )

    @Serializable
    data class Snapshot(
        val profileId: String,
        val schemaVersion: Int = 1,
        val records: List<Rating> = emptyList(),
        val preferences: Preferences? = null,
    )

    interface Storage {
        fun read(profileId: String): Snapshot

        fun write(snapshot: Snapshot)

        fun backup(snapshot: Snapshot) {
            error("Recovery storage unavailable")
        }
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
        deckFitOnly: Boolean = false,
        catalogueReleaseId: String? = null,
    ): Snapshot =
        synchronized(lock) {
            verifyAccess(expected)
            requireUuid(cartridgeId)
            require(score == null || score in 1..(if (deckFitOnly) 5 else 10))
            require(displayTitle == null || displayTitle.length in 1..512)
            require(platformId == null || platformId.length in 1..64)
            val existing = readValidated(expected.profileId)
            val previous = existing.records.firstOrNull { it.cartridgeId == cartridgeId }
            if (catalogueReleaseId != null) require(catalogueRatingId(catalogueReleaseId) == cartridgeId)
            require(previous == null || catalogueReleaseId == previous.catalogueReleaseId) {
                "Rating identity cannot change"
            }
            val priorScore = if (deckFitOnly) previous?.deckFit else previous?.score
            if (priorScore == score) return@synchronized existing
            val revision = previous?.revision ?: 0L
            require(revision < Long.MAX_VALUE)
            val timestamp = now()
            require(timestamp >= 0)
            val rating =
                Rating(
                    cartridgeId = cartridgeId,
                    score = if (deckFitOnly) previous?.score else score,
                    revision = revision + 1,
                    updatedAt = timestamp,
                    displayTitle = displayTitle ?: previous?.displayTitle,
                    platformId = platformId ?: previous?.platformId,
                    deckFit = if (deckFitOnly) score else previous?.deckFit,
                    catalogueReleaseId = catalogueReleaseId,
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

    fun updatePreferences(
        expected: Session,
        transform: (Preferences) -> Preferences,
    ): Snapshot =
        synchronized(lock) {
            verifyAccess(expected)
            val existing = readValidated(expected.profileId)
            val updated = transform(existing.preferences ?: Preferences())
            validatePreferences(updated)
            val next = existing.copy(preferences = updated)
            verifyAccess(expected)
            if (next != existing) storage.write(next)
            next
        }

    /** Immutable preview and conflict-safe apply under the same repository lock. */
    fun previewRestore(
        expected: Session,
        source: Snapshot,
    ): GemProfileBackup.Preview =
        synchronized(lock) {
            verifyAccess(expected)
            validateSnapshot(source)
            GemProfileBackup.preview(readValidated(expected.profileId), source)
        }

    fun restore(
        expected: Session,
        preview: GemProfileBackup.Preview,
        replaceConflicts: Boolean,
        restorePreferences: Boolean,
        allowCopy: Boolean,
    ): Snapshot =
        synchronized(lock) {
            verifyAccess(expected)
            require(preview.local.profileId == expected.profileId)
            require(preview.source.profileId == expected.profileId || allowCopy) { "Explicit player copy required" }
            validateSnapshot(preview.source)
            val current = readValidated(expected.profileId)
            check(current == preview.local) { "Ratings changed; preview again" }
            val next = GemProfileBackup.merge(current, preview.source, replaceConflicts, restorePreferences, now())
            validateSnapshot(next)
            verifyAccess(expected)
            if (next != current) {
                storage.backup(current)
                storage.write(next)
            }
            next
        }

    private fun verifyAccess(expected: Session) {
        requireUuid(expected.profileId)
        check(expected.role != Role.JUNIOR && expected == activeSession()) { "GEM unavailable for this player" }
    }

    private fun readValidated(profileId: String): Snapshot {
        val snapshot = storage.read(profileId)
        require(snapshot.profileId == profileId)
        validateSnapshot(snapshot)
        return snapshot
    }

    companion object {
        private val lock = Any()

        internal fun validatePreferences(preferences: Preferences) {
            require(preferences.wildcardPercent in 20..50)
            require(preferences.recentSurprises.size <= 20)
            require(preferences.recentSurprises.distinct().size == preferences.recentSurprises.size)
            require(preferences.recentSurprises.all { it.length in 1..256 && it.none(Char::isISOControl) })
        }

        internal fun validateSnapshot(snapshot: Snapshot) {
            requireUuid(snapshot.profileId)
            require(snapshot.schemaVersion == 1 && snapshot.records.size <= 100_000)
            require(snapshot.records.map { it.cartridgeId }.distinct().size == snapshot.records.size)
            snapshot.preferences?.let(::validatePreferences)
            snapshot.records.forEach {
                requireUuid(it.cartridgeId)
                if (it.catalogueReleaseId != null) require(catalogueRatingId(it.catalogueReleaseId) == it.cartridgeId)
                require(it.score == null || it.score in 1..10)
                require(it.deckFit == null || it.deckFit in 1..5)
                require(it.revision > 0 && it.updatedAt >= 0)
                require(it.displayTitle == null || it.displayTitle.length in 1..512)
                require(it.platformId == null || it.platformId.length in 1..64)
            }
        }

        internal fun catalogueRatingId(releaseId: String): String {
            require(releaseId.length in 1..256 && releaseId.none(Char::isISOControl))
            return UUID.nameUUIDFromBytes("cartdeck.gem.release:$releaseId".encodeToByteArray()).toString()
        }

        internal fun requireUuid(value: String) {
            require(UUID.fromString(value).toString() == value) { "Expected canonical UUID" }
        }
    }
}
