package com.swordfish.lemuroid.app.gemrating

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class GemPresentationPersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val game =
        Game(
            title = "Fixture",
            systemId = "gba",
            fileName = "fixture.gba",
            fileUri = "content://test/gem",
            developer = null,
            coverFrontUrl = null,
            lastIndexedAt = 0,
            cartridgeId = "00000000-0000-4000-8000-000000000042",
        )

    @Test
    fun shelfProjectionIsOptInAndClearingRatingRemovesOverlay() {
        assertFalse(GemShelfPresentation.enabled(context))
        val store = GemRatingLocalStore.open(context)
        store.repository.set(store.session, game.cartridgeId, 8, game.title, game.systemId)
        assertTrue(GemShelfPresentation.ratings(context).isEmpty())
        assertTrue(GemShelfPresentation.setEnabled(context, true))
        assertEquals(mapOf(game.cartridgeId to 8), GemShelfPresentation.ratings(context))
        store.repository.set(store.session, game.cartridgeId, null)
        assertTrue(GemShelfPresentation.ratings(context).isEmpty())
        assertTrue(GemShelfPresentation.setEnabled(context, false))
    }

    @Test
    fun existingDiscoveryPreferencesMigrateWithoutLosingRatings() {
        val store = GemRatingLocalStore.open(context)
        val owner = store.session.profileId
        val discovery = context.getSharedPreferences("gem_discovery_v1", Context.MODE_PRIVATE)
        assertTrue(
            discovery.edit().putInt("$owner:wildcard", 45).putString("$owner:recent", "old-release").commit(),
        )
        val shelf = context.getSharedPreferences("gem_shelf_presentation_v1", Context.MODE_PRIVATE)
        assertTrue(shelf.edit().putBoolean("$owner:showRatings", true).commit())
        store.repository.set(store.session, game.cartridgeId, 8)
        val snapshot = GemRatingLocalStore.open(context).repository.snapshot(store.session)
        assertEquals(8, snapshot.records.single().score)
        assertEquals(GemRatingRepository.Preferences(45, listOf("old-release"), true), snapshot.preferences)
        assertTrue(discovery.edit().putInt("$owner:wildcard", 20).commit())
        val reopened = GemRatingLocalStore.open(context).repository.snapshot(store.session)
        assertEquals(45, reopened.preferences?.wildcardPercent)
    }

    @Test
    fun customInformationIsLocalAndResettableWithoutChangingCartridgeIdentity() {
        val corrections = GemLocalCorrections(context)
        val saveAlias = game.saveFileName()
        assertTrue(corrections.saveCustom(game, "Custom title", "Strategy"))
        assertNull(corrections.read(game))
        assertEquals(GemLocalCorrections.Custom("Custom title", "Strategy"), GemLocalCorrections(context).custom(game))
        assertEquals("00000000-0000-4000-8000-000000000042", game.cartridgeId)
        assertEquals("Fixture", game.title)
        assertEquals(saveAlias, game.saveFileName())
        assertTrue(corrections.clear(game))
        assertNull(GemLocalCorrections(context).custom(game))
    }
}
