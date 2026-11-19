package com.swordfish.lemuroid.app.gemrating

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GemProfileBackupTest {
    private val player = "00000000-0000-4000-8000-000000000001"
    private val other = "00000000-0000-4000-8000-000000000002"
    private val game = "00000000-0000-4000-8000-000000000011"
    private val second = "00000000-0000-4000-8000-000000000012"
    private val session = GemRatingRepository.Session(player, GemRatingRepository.Role.MANAGER)
    private var active: GemRatingRepository.Session? = session
    private var stored = GemRatingRepository.Snapshot(player)
    private var recovery: GemRatingRepository.Snapshot? = null
    private val storage =
        object : GemRatingRepository.Storage {
            override fun read(profileId: String) = stored

            override fun write(snapshot: GemRatingRepository.Snapshot) {
                stored = snapshot
            }

            override fun backup(snapshot: GemRatingRepository.Snapshot) {
                recovery = snapshot
            }
        }
    private val repository = GemRatingRepository({ active }, storage) { 123L }

    private fun rating(
        cartridgeId: String,
        score: Int?,
        revision: Long = 1L,
    ) = GemRatingRepository.Rating(cartridgeId, score, revision, 10L, "Fixture", "gba")

    @Test
    fun deckFitIsIndependentAndIncludedInBackup() {
        repository.set(session, game, 9)
        repository.set(session, game, 4, deckFitOnly = true)
        val both = repository.snapshot(session)
        assertEquals(9, both.records.single().score)
        assertEquals(4, both.records.single().deckFit)
        assertEquals(both, GemProfileBackup.decode(GemProfileBackup.encode(both)))
        repository.set(session, game, null, deckFitOnly = true)
        assertEquals(9, repository.snapshot(session).records.single().score)
        assertNull(repository.snapshot(session).records.single().deckFit)
        assertTrue(runCatching { repository.set(session, game, 6, deckFitOnly = true) }.isFailure)
    }

    @Test
    fun roundTripIncludesPreferencesAndExplicitClear() {
        val snapshot =
            GemRatingRepository.Snapshot(
                player,
                records = listOf(rating(game, 9), rating(second, null, 2)),
                preferences = GemRatingRepository.Preferences(45, listOf("release"), true),
            )
        val bytes = GemProfileBackup.encode(snapshot)
        assertEquals(snapshot, GemProfileBackup.decode(bytes))
        val bad = bytes.copyOf().apply { this[0] = 0 }
        assertTrue(runCatching { GemProfileBackup.decode(bad) }.isFailure)
    }

    @Test
    fun keepLocalDefaultDoesNotResurrectClearedRating() {
        stored = stored.copy(records = listOf(rating(game, null, 2)))
        val source = stored.copy(records = listOf(rating(game, 10), rating(second, 4)))
        val before = stored
        val preview = repository.previewRestore(session, source)
        assertEquals(1, preview.conflicts)
        assertEquals(1, preview.additions)
        val restored = repository.restore(session, preview, false, false, false)
        assertNull(restored.records.first { it.cartridgeId == game }.score)
        assertEquals(4, restored.records.first { it.cartridgeId == second }.score)
        assertEquals(before, recovery)
    }

    @Test
    fun explicitReplacementAndPreferencesAreOneWrite() {
        stored = stored.copy(records = listOf(rating(game, 2, 3)))
        val source =
            stored.copy(
                records = listOf(rating(game, 9, 5)),
                preferences = GemRatingRepository.Preferences(50, emptyList(), true),
            )
        val before = stored
        val result = repository.restore(session, repository.previewRestore(session, source), true, true, false)
        assertEquals(9, result.records.single().score)
        assertEquals(6L, result.records.single().revision)
        assertEquals(source.preferences, result.preferences)
        assertEquals(before, recovery)
    }

    @Test
    fun stalePreviewAndUnapprovedPlayerCopyAreRejected() {
        val source = GemRatingRepository.Snapshot(other, records = listOf(rating(game, 8)))
        val preview = repository.previewRestore(session, source)
        assertTrue(runCatching { repository.restore(session, preview, true, true, false) }.isFailure)
        repository.set(session, second, 7)
        assertTrue(runCatching { repository.restore(session, preview, false, false, true) }.isFailure)
        assertEquals(7, stored.records.single().score)
    }

    @Test
    fun safeSaveDescriptorBindsOwnerPathAndBytes() {
        repository.set(session, game, 8)
        val exported = GemSafeSaveComponent.capture(repository, session)
        val preview = GemSafeSaveComponent.prepare(repository, session, exported.descriptor, exported.bytes)
        assertEquals(1, preview.unchanged)
        val forged = exported.descriptor.copy(ownerPlayerId = other)
        assertTrue(
            runCatching { GemSafeSaveComponent.prepare(repository, session, forged, exported.bytes) }.isFailure,
        )
        active = GemRatingRepository.Session(other, GemRatingRepository.Role.JUNIOR)
        assertTrue(runCatching { GemSafeSaveComponent.capture(repository, session) }.isFailure)
    }

    @Test
    fun rejectsInvalidPreferencesAndDuplicateCartridges() {
        val duplicate = stored.copy(records = listOf(rating(game, 5), rating(game, 6)))
        assertTrue(runCatching { GemProfileBackup.encode(duplicate) }.isFailure)
        val bad = stored.copy(preferences = GemRatingRepository.Preferences(99))
        assertTrue(runCatching { GemProfileBackup.encode(bad) }.isFailure)
    }
}
