package com.swordfish.lemuroid.app.shared.library

import android.content.Context
import androidx.room.withTransaction
import com.swordfish.lemuroid.app.mobile.feature.shelf.CartridgeFacing
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfLayout
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfStore
import com.swordfish.lemuroid.app.mobile.feature.shelf.retiredShelfKey
import com.swordfish.lemuroid.app.mobile.feature.shelf.shelfGameKey
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.library.db.entity.RetiredCartridge
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * C2B: user-confirmed reassociation of an already indexed ROM source.
 *
 * Never searches, downloads, modifies or deletes ROMs. An unplayed,
 * unmodified source is deliberately required; existing candidate-owned
 * save data is never reassigned. C1's journal makes Room/shelf recovery
 * durable across process termination after the DB transaction.
 */
class CartridgeRecoveryManager(
    context: Context,
    private val db: RetrogradeDatabase,
) {
    private val appContext = context.applicationContext
    private val directories = DirectoriesManager(appContext)

    data class Inventory(val retired: List<RetiredCartridge>, val available: List<Game>)

    suspend fun inventory(): Inventory =
        withContext(Dispatchers.IO) {
            Inventory(db.retiredCartridgeDao().selectAll(), db.gameDao().selectShelfGames().first())
        }

    /** The choice is explicit, never an implicit inference from a title or filename. */
    suspend fun restore(
        retiredCartridgeId: String,
        selectedGameId: Int,
    ) = withContext(Dispatchers.IO) {
        LibraryIndexWork.refreshLock.withLock {
            val journal = LibraryRefreshJournal(appContext)
            val shelf = ShelfStore(appContext)
            val before = inventory()
            journal.reconcile(before.available, shelf, before.retired)
            val ready = inventory()
            val original = ready.retired.singleOrNull { it.cartridgeId == retiredCartridgeId }
                ?: error("The unavailable cartridge no longer requires recovery")
            val candidate = ready.available.singleOrNull { it.id == selectedGameId }
                ?: error("The selected ROM is no longer indexed")
            check(candidate.systemId == original.systemId) { "The consoles do not match" }
            check(candidate.cartridgeId != original.cartridgeId) { "The identity has already been recovered" }
            check(ready.available.none { it.cartridgeId == original.cartridgeId }) {
                "This cartridge identity is already active"
            }
            requireUnusedCandidate(candidate)
            val candidateKey = shelfGameKey(candidate.systemId, candidate.fileUri)
            verifyDefaultShelfCandidate(shelf.read(), original, candidateKey)
            // The transaction cannot begin until a durable recovery intent is
            // written. If interrupted, ordinary startup journal replay repairs
            // the shelf according to the actual committed Room state.
            journal.prepareManualRecovery(
                ready.available,
                ready.retired,
                original.cartridgeId,
                candidateKey,
            )
            db.withTransaction {
                val oldNow =
                    db.retiredCartridgeDao().selectAll()
                        .singleOrNull { it.cartridgeId == retiredCartridgeId }
                        ?: error("The retired cartridge changed during recovery")
                val current = db.gameDao().selectById(selectedGameId)
                    ?: error("The source changed during recovery")
                check(current.fileUri == candidate.fileUri && current.cartridgeId == candidate.cartridgeId) {
                    "The selected source changed during recovery"
                }
                check(current.systemId == oldNow.systemId)
                requireUnusedCandidate(current)
                db.gameDao().update(oldNow.restore(current))
                db.retiredCartridgeDao().delete(listOf(oldNow))
            }
            // A failure here retains the journal; startup/retry will replay.
            val after = inventory()
            journal.reconcile(after.available, shelf, after.retired)
        }
    }

    private fun requireUnusedCandidate(game: Game) {
        check(game.lastPlayedAt == null && !game.isFavorite &&
            game.customName == null && game.customCoverUri == null
        ) { "Selected ROM already has player activity or customisation" }
        val uuid = game.cartridgeId
        val roots =
            listOf(
                File(directories.getSaveBindingsDirectory(), uuid),
                File(directories.getSavesDirectory(), "cartridges/$uuid"),
                File(directories.getStatesDirectory(), "cartridges/$uuid"),
                File(directories.getStatesPreviewDirectory(), "cartridges/$uuid"),
            )
        check(roots.none { it.exists() }) { "Selected ROM has existing progress; recovery would be unsafe" }
    }

    private fun verifyDefaultShelfCandidate(
        layout: ShelfLayout,
        original: RetiredCartridge,
        candidateKey: String,
    ) {
        val missing = retiredShelfKey(original.cartridgeId)
        val entries =
            layout.sections.flatMap { section ->
                section.cells.filter { it.gameKey == candidateKey }.map { section to it }
            }
        check(entries.size <= 1) { "Duplicate source placement requires review" }
        if (layout.sections.flatMap { it.cells }.none { it.gameKey == missing } || entries.isEmpty()) return
        val (section, cell) = entries.single()
        check(section.id == ShelfLayout.UNSORTED &&
            cell.id == "game:$candidateKey" &&
            cell.facing == CartridgeFacing.SPINE
        ) { "Selected source shelf layout was customised; recovery requires review" }
    }
}
