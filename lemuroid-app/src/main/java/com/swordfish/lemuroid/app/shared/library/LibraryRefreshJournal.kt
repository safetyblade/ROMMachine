package com.swordfish.lemuroid.app.shared.library

import android.content.Context
import android.util.AtomicFile
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfStore
import com.swordfish.lemuroid.app.mobile.feature.shelf.shelfGameKey
import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.json.JSONObject
import java.io.File

/** Write-ahead identity map. Room is atomic; shelf replay derives truth from committed Room. */
internal class LibraryRefreshJournal(context: Context) {
    private val file = File(context.filesDir, "cartdeck/library-refresh-v1.json")
    private val atomic = AtomicFile(file)

    fun pending(): Boolean = file.exists() || File(file.path + ".bak").exists()

    fun prepare(games: List<Game>) {
        check(!pending()) { "Previous refresh requires recovery" }
        file.parentFile!!.mkdirs()
        val identities = JSONObject()
        games.forEach { identities.put(it.cartridgeId, shelfGameKey(it.systemId, it.fileUri)) }
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
    ) {
        if (!pending()) return
        val root = atomic.openRead().bufferedReader().use { JSONObject(it.readText()) }
        require(root.getInt("version") == 1) { "Unknown refresh journal version" }
        val identities = root.getJSONObject("identities")
        val moves =
            games.mapNotNull { game ->
                val old = identities.optString(game.cartridgeId).takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                val current = shelfGameKey(game.systemId, game.fileUri)
                if (old == current) null else old to current
            }.toMap()
        val names = games.associate { shelfGameKey(it.systemId, it.fileUri) to it.displayName() }
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
