package com.swordfish.lemuroid.app.shared.library

import android.content.Context
import android.util.AtomicFile
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfStore
import com.swordfish.lemuroid.app.mobile.feature.shelf.retiredShelfKey
import com.swordfish.lemuroid.app.mobile.feature.shelf.shelfGameKey
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.library.db.entity.RetiredCartridge
import org.json.JSONObject
import java.io.File

/** Write-ahead identity map. Room is atomic; shelf replay derives truth from committed Room. */
internal class LibraryRefreshJournal(context: Context) {
    private val file = File(context.filesDir, "cartdeck/library-refresh-v1.json")
    private val atomic = AtomicFile(file)

    fun pending(): Boolean = file.exists() || File(file.path + ".bak").exists()

    fun prepare(games: List<Game>, retired: List<RetiredCartridge> = emptyList()) {
        check(!pending()) { "Previous refresh requires recovery" }
        file.parentFile!!.mkdirs()
        val identities = JSONObject()
        retired.forEach { identities.put(it.cartridgeId, retiredShelfKey(it.cartridgeId)) }
        games.forEach {
            check(!identities.has(it.cartridgeId)) { "Live cartridge conflicts with retired identity" }
            identities.put(it.cartridgeId, shelfGameKey(it.systemId, it.fileUri))
        }
        val stream = atomic.startWrite()
        try {
            stream.write(
                JSONObject().put("version", 1).put("identities", identities).toString().toByteArray(Charsets.UTF_8),
            )
            atomic.finishWrite(stream)
        } catch (error: Throwable) {
            atomic.failWrite(stream)
            throw error
        }
    }

    suspend fun reconcile(
        games: List<Game>,
        shelf: ShelfStore,
        retired: List<RetiredCartridge> = emptyList(),
    ) {
        if (!pending()) return
        val root = atomic.openRead().bufferedReader().use { JSONObject(it.readText()) }
        require(root.getInt("version") == 1) { "Unknown refresh journal version" }
        val identities = root.getJSONObject("identities")
        // An unavailable slot has a distinct UUID key. A different cartridge
        // arriving at the same historical URI must never hijack that slot.
        val currentKeys =
            retired.associate { it.cartridgeId to retiredShelfKey(it.cartridgeId) } +
                games.associate { it.cartridgeId to shelfGameKey(it.systemId, it.fileUri) }
        val moves =
            currentKeys.mapNotNull { (cartridgeId, current) ->
                val old = identities.optString(cartridgeId).takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                if (old == current) null else old to current
            }.toMap()
        val names =
            retired.associate { retiredShelfKey(it.cartridgeId) to it.displayName() } +
                games.associate { shelfGameKey(it.systemId, it.fileUri) to it.displayName() }
        shelf.update { layout ->
            val known = layout.sections.flatMap { it.cells }.mapNotNull { it.gameKey }.toSet()
            check(moves.none { (old, new) -> old in known && new in known }) { "Ambiguous shelf identity move" }
            layout.copy(
                sections =
                    layout.sections.map { section ->
                        section.copy(
                            cells =
                                section.cells.map {
                                        cell ->
                                    cell.copy(gameKey = moves[cell.gameKey] ?: cell.gameKey)
                                },
                        )
                    },
            ).reconcile(names, removeMissing = true)
        }
        // Replay after a crash between shelf write and delete is idempotent.
        atomic.delete()
        check(!pending()) { "Could not complete refresh journal" }
    }
}
