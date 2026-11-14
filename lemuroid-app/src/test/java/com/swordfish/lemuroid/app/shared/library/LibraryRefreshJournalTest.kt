package com.swordfish.lemuroid.app.shared.library

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfStore
import com.swordfish.lemuroid.app.mobile.feature.shelf.shelfGameKey
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.library.db.entity.RetiredCartridge
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
class LibraryRefreshJournalTest {
    private fun game(uri: String) =
        Game(
            fileName = "test.gba",
            fileUri = uri,
            title = "Test",
            systemId = "gba",
            developer = null,
            coverFrontUrl = null,
            lastIndexedAt = 1,
        )

    @Test fun missingCartridgeStaysInOriginalSlotThenReimportRekeysTheSameCell() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Application>()
            File(context.filesDir, "cartdeck").deleteRecursively()
            val original = game("content://com.android.externalstorage.documents/document/primary%3Aold")
            val originalKey = shelfGameKey(original.systemId, original.fileUri)
            val shelf = ShelfStore(context)
            shelf.update { it.reconcile(mapOf(originalKey to "Original cartridge")) }
            val baseline = shelf.read()
            val originalCell = baseline.sections.flatMap { it.cells }.single()
            val retired = RetiredCartridge.fromGame(original, 10L)

            LibraryRefreshJournal(context).prepare(listOf(original))
            LibraryRefreshJournal(context).reconcile(emptyList(), shelf, listOf(retired))
            val missing = shelf.read()
            assertEquals(originalCell, missing.sections.flatMap { it.cells }.single())
            assertFalse(LibraryRefreshJournal(context).pending())

            LibraryRefreshJournal(context).prepare(emptyList(), listOf(retired))
            val reimported =
                original.copy(fileUri = "content://com.android.externalstorage.documents/tree/new/document/primary%3Aold")
            LibraryRefreshJournal(context).reconcile(listOf(reimported), shelf)
            val restored = shelf.read().sections.flatMap { it.cells }.single()
            assertEquals(originalCell.id, restored.id)
            assertEquals(shelfGameKey(reimported.systemId, reimported.fileUri), restored.gameKey)
            assertEquals(baseline.appearance, shelf.read().appearance)
            assertFalse(LibraryRefreshJournal(context).pending())
        }

    @Test fun processRestartReplaysCommittedMoveAndPreservesCellAndAppearance() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Application>()
            File(context.filesDir, "cartdeck").deleteRecursively()
            val original = game("content://test/old")
            val key = shelfGameKey(original.systemId, original.fileUri)
            val shelf = ShelfStore(context)
            shelf.update { it.reconcile(mapOf(key to original.title)) }
            val before = shelf.read()
            LibraryRefreshJournal(context).prepare(listOf(original))
            val moved = original.copy(fileUri = "content://test/new")
            // A newly constructed journal represents a restarted process after Room commit.
            LibraryRefreshJournal(context).reconcile(listOf(moved), ShelfStore(context))
            val after = shelf.read()
            assertEquals(before.appearance, after.appearance)
            assertEquals(
                before.sections.flatMap { it.cells }.map { it.id },
                after.sections.flatMap { it.cells }.map { it.id },
            )
            assertEquals(
                listOf(shelfGameKey(moved.systemId, moved.fileUri)),
                after.sections.flatMap {
                    it.cells
                }.mapNotNull { it.gameKey },
            )
            assertFalse(LibraryRefreshJournal(context).pending())
            LibraryRefreshJournal(context).reconcile(listOf(moved), shelf)
            assertEquals(after, shelf.read())
        }

    @Test
    fun crashAfterShelfWriteBeforeIntentDeletionReplaysWithoutChangingRevision() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Application>()
            File(context.filesDir, "cartdeck").deleteRecursively()
            val original = game("content://test/old")
            val moved = original.copy(fileUri = "content://test/new")
            val shelf = ShelfStore(context)
            shelf.update { it.reconcile(mapOf(shelfGameKey(original.systemId, original.fileUri) to original.title)) }
            val journal = LibraryRefreshJournal(context)
            journal.prepare(listOf(original))
            val intent = File(context.filesDir, "cartdeck/library-refresh-v1.json")
            val bytes = intent.readBytes()
            journal.reconcile(listOf(moved), shelf)
            val committed = shelf.read()
            // Restore the intent to model death after shelf fsync but before delete.
            intent.writeBytes(bytes)
            LibraryRefreshJournal(context).reconcile(listOf(moved), ShelfStore(context))
            assertEquals(committed, shelf.read())
            assertFalse(journal.pending())
        }

    @Test fun rollbackBeforeRoomCommitKeepsOriginalShelfAndClearsIntent() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Application>()
            File(context.filesDir, "cartdeck").deleteRecursively()
            val original = game("content://test/old")
            val shelf = ShelfStore(context)
            shelf.update { it.reconcile(mapOf(shelfGameKey(original.systemId, original.fileUri) to original.title)) }
            val before = shelf.read()
            LibraryRefreshJournal(context).prepare(listOf(original))
            LibraryRefreshJournal(context).reconcile(listOf(original), shelf)
            assertEquals(before, shelf.read())
            assertFalse(LibraryRefreshJournal(context).pending())
        }

    @Test fun shelfConflictRetainsJournalAndLayoutForRecovery() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Application>()
            File(context.filesDir, "cartdeck").deleteRecursively()
            val original = game("content://test/old")
            val moved = original.copy(fileUri = "content://test/new")
            val shelf = ShelfStore(context)
            shelf.update {
                it.reconcile(
                    listOf(original, moved).associate {
                            game ->
                        shelfGameKey(game.systemId, game.fileUri) to game.title
                    },
                )
            }
            val before = shelf.read()
            val journal = LibraryRefreshJournal(context)
            journal.prepare(listOf(original))
            assertThrows(IllegalStateException::class.java) { runBlocking { journal.reconcile(listOf(moved), shelf) } }
            assertEquals(before, shelf.read())
            assertTrue(journal.pending())
        }
}
