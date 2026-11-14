package com.swordfish.lemuroid.app.shared.library

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfStore
import com.swordfish.lemuroid.app.mobile.feature.shelf.retiredShelfKey
import com.swordfish.lemuroid.app.mobile.feature.shelf.shelfGameKey
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.library.db.entity.RetiredCartridge
import com.swordfish.lemuroid.lib.saves.CartridgeSaveBinding
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class CartridgeRecoveryManagerTest {
    private fun cartridge(
        uri: String,
        name: String,
    ) =
        Game(
            fileName = "$name.gba",
            fileUri = uri,
            title = name,
            systemId = "gba",
            developer = null,
            coverFrontUrl = null,
            lastIndexedAt = 10,
        )

    private suspend fun withInventory(
        block: suspend (Application, RetrogradeDatabase, Game, Game, RetiredCartridge, ShelfStore) -> Unit,
    ) {
        val context = ApplicationProvider.getApplicationContext<Application>()
        File(context.filesDir, "cartdeck").deleteRecursively()
        val db =
            Room.inMemoryDatabaseBuilder(context, RetrogradeDatabase::class.java)
                .allowMainThreadQueries().build()
        val former =
            cartridge("content://fixture/old", "Original").copy(
                customName = "My cartridge",
                customCoverUri = "file:///cover.jpg",
                isFavorite = true,
                lastPlayedAt = 23L,
            )
        val retired = RetiredCartridge.fromGame(former, 50L)
        val incoming = cartridge("content://fixture/new", "Replacement")
        db.retiredCartridgeDao().insert(listOf(retired))
        db.gameDao().insert(listOf(incoming))
        val active = db.gameDao().selectShelfGames().first().single()
        val shelf = ShelfStore(context)
        shelf.update {
            it.reconcile(
                mapOf(
                    retiredShelfKey(retired.cartridgeId) to former.displayName(),
                    shelfGameKey(active.systemId, active.fileUri) to active.displayName(),
                ),
            )
        }
        try {
            block(context, db, former, active, retired, shelf)
        } finally {
            db.close()
        }
    }

    @Test fun explicitRecoveryKeepsRetiredCellAndPlayerMetadataAndRemovesOnlyFreshCandidateSlot() =
        runBlocking {
            withInventory { context, db, original, incoming, retired, shelf ->
                val originalKey = retiredShelfKey(retired.cartridgeId)
                val originalCell = shelf.read().sections.flatMap { it.cells }.single { it.gameKey == originalKey }
                CartridgeRecoveryManager(context, db).restore(retired.cartridgeId, incoming.id)
                val result = db.gameDao().selectShelfGames().first().single()
                assertEquals(retired.cartridgeId, result.cartridgeId)
                assertEquals(original.customName, result.customName)
                assertEquals(original.customCoverUri, result.customCoverUri)
                assertEquals(original.lastPlayedAt, result.lastPlayedAt)
                assertEquals(original.legacyFileName, result.legacyFileName)
                assertEquals(0, db.retiredCartridgeDao().selectAll().size)
                val directories = DirectoriesManager(context)
                val guard =
                    File(
                        directories.getSaveBindingsDirectory(),
                        "${retired.cartridgeId}/manual-source-binding.block",
                    )
                assertTrue(guard.isFile)
                val binding =
                    CartridgeSaveBinding(
                        directories,
                        { db.gameDao().selectShelfGames().first() },
                        { db.retiredCartridgeDao().selectAll() },
                    )
                assertFalse(binding.mayImportLegacy(result, true))
                val cells = shelf.read().sections.flatMap { it.cells }
                assertEquals(1, cells.size)
                assertEquals(originalCell.id, cells.single().id)
                assertEquals(shelfGameKey(result.systemId, result.fileUri), cells.single().gameKey)
                assertFalse(LibraryRefreshJournal(context).pending())
            }
        }

    @Test fun playerProgressOrModifiedCandidateBlocksRecoveryWithoutMutation() =
        runBlocking {
            withInventory { context, db, _, incoming, retired, shelf ->
                val modified = incoming.copy(lastPlayedAt = 99L)
                db.gameDao().update(modified)
                val beforeShelf = shelf.read()
                assertThrows(IllegalStateException::class.java) {
                    runBlocking { CartridgeRecoveryManager(context, db).restore(retired.cartridgeId, incoming.id) }
                }
                assertEquals(listOf(modified), db.gameDao().selectShelfGames().first())
                assertEquals(listOf(retired), db.retiredCartridgeDao().selectAll())
                assertEquals(beforeShelf, shelf.read())
                assertFalse(LibraryRefreshJournal(context).pending())
            }
        }

    @Test fun manualJournalCanReplayAfterCommitBeforeShelfUpdateWithoutGuessingOwnership() =
        runBlocking {
            withInventory { context, db, _, incoming, retired, shelf ->
                val originalCell =
                    shelf.read().sections.flatMap { it.cells }
                        .single { it.gameKey == retiredShelfKey(retired.cartridgeId) }
                val journal = LibraryRefreshJournal(context)
                journal.prepareManualRecovery(
                    db.gameDao().selectShelfGames().first(),
                    db.retiredCartridgeDao().selectAll(),
                    retired.cartridgeId,
                    shelfGameKey(incoming.systemId, incoming.fileUri),
                )
                db.gameDao().update(retired.restore(incoming))
                db.retiredCartridgeDao().delete(listOf(retired))
                val restart = LibraryRefreshJournal(context)
                assertTrue(restart.pending())
                restart.reconcile(db.gameDao().selectShelfGames().first(), shelf, emptyList())
                val cells = shelf.read().sections.flatMap { it.cells }
                assertEquals(1, cells.size)
                assertEquals(originalCell.id, cells.single().id)
                assertFalse(restart.pending())
                restart.reconcile(db.gameDao().selectShelfGames().first(), shelf, emptyList())
                assertEquals(cells, shelf.read().sections.flatMap { it.cells })
            }
        }
}
