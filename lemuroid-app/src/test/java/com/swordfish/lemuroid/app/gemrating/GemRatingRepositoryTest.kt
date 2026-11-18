package com.swordfish.lemuroid.app.gemrating

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GemRatingRepositoryTest {
    private val manager =
        GemRatingRepository.Session("00000000-0000-4000-8000-000000000001", GemRatingRepository.Role.MANAGER)
    private val standard =
        GemRatingRepository.Session("00000000-0000-4000-8000-000000000002", GemRatingRepository.Role.STANDARD)
    private val junior =
        GemRatingRepository.Session("00000000-0000-4000-8000-000000000003", GemRatingRepository.Role.JUNIOR)
    private val cartridge = "00000000-0000-4000-8000-000000000011"
    private val otherCartridge = "00000000-0000-4000-8000-000000000012"
    private var active: GemRatingRepository.Session? = manager
    private val files = mutableMapOf<String, GemRatingRepository.Snapshot>()
    private val storage =
        object : GemRatingRepository.Storage {
            override fun read(profileId: String) = files[profileId] ?: GemRatingRepository.Snapshot(profileId)

            override fun write(snapshot: GemRatingRepository.Snapshot) {
                files[snapshot.profileId] = snapshot
            }
        }

    private fun repository() = GemRatingRepository({ active }, storage) { 123L }

    @Test
    fun explicitRatingPersistsAcrossRepositoryRecreation() {
        assertNull(repository().score(manager, cartridge))
        repository().set(manager, cartridge, 8)
        assertEquals(8, repository().score(manager, cartridge))
    }

    @Test
    fun clearIsUnratedAndRetainsDeletionRevision() {
        repository().set(manager, cartridge, 10)
        repository().set(manager, cartridge, null)
        assertNull(repository().score(manager, cartridge))
        val cleared = repository().snapshot(manager).records.single()
        assertEquals(2L, cleared.revision)
        repository().set(manager, cartridge, 5)
        assertEquals(3L, repository().snapshot(manager).records.single().revision)
    }

    @Test
    fun profilesAreIndependentAndStaleSessionCannotAccess() {
        repository().set(manager, cartridge, 9)
        active = standard
        assertNull(repository().score(standard, cartridge))
        repository().set(standard, cartridge, 3)
        assertTrue(runCatching { repository().score(manager, cartridge) }.isFailure)
        assertTrue(runCatching { repository().set(manager, cartridge, 2) }.isFailure)
        active = manager
        assertEquals(9, repository().score(manager, cartridge))
        active = standard
        assertEquals(3, repository().score(standard, cartridge))
    }

    @Test
    fun juniorCannotReadOrWriteEvenWhenActive() {
        active = junior
        assertTrue(runCatching { repository().snapshot(junior) }.isFailure)
        assertTrue(runCatching { repository().set(junior, cartridge, 7) }.isFailure)
        assertTrue(files.isEmpty())
    }

    @Test
    fun noSessionCannotReadOrWrite() {
        active = null
        assertTrue(runCatching { repository().snapshot(manager) }.isFailure)
        assertTrue(runCatching { repository().set(manager, cartridge, 7) }.isFailure)
    }

    @Test
    fun cartridgeIdentityNotCatalogueOrTitleOwnsRating() {
        repository().set(manager, cartridge, 9)
        assertNull(repository().score(manager, otherCartridge))
        repository().set(manager, otherCartridge, 2)
        assertEquals(9, repository().score(manager, cartridge))
        assertEquals(2, repository().score(manager, otherCartridge))
        assertEquals(2, repository().snapshot(manager).records.size)
    }

    @Test
    fun removedCartridgeRetainsScoreAndDisplayMetadata() {
        repository().set(manager, cartridge, 8, "Example game", "gba")
        val retained = repository().snapshot(manager).records.single()
        assertEquals(cartridge, retained.cartridgeId)
        assertEquals(8, retained.score)
        assertEquals("Example game", retained.displayTitle)
        assertEquals("gba", retained.platformId)
        repository().set(manager, cartridge, 6)
        assertEquals("Example game", repository().snapshot(manager).records.single().displayTitle)
    }

    @Test
    fun scoreBoundsAndNoSyntheticRating() {
        for (score in listOf(0, -1, 11)) {
            assertTrue(runCatching { repository().set(manager, cartridge, score) }.isFailure)
        }
        assertTrue(repository().snapshot(manager).records.isEmpty())
        repository().set(manager, cartridge, null)
        assertTrue(repository().snapshot(manager).records.isEmpty())
    }

    @Test
    fun corruptProfileSnapshotIsNotOverwritten() {
        val corrupt = GemRatingRepository.Snapshot(standard.profileId)
        files[manager.profileId] = corrupt
        assertTrue(runCatching { repository().set(manager, cartridge, 7) }.isFailure)
        assertEquals(corrupt, files[manager.profileId])
    }
}
