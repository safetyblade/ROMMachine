package com.swordfish.lemuroid.app.mobile.feature.home

import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfCell
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfLayout
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfSection
import com.swordfish.lemuroid.app.mobile.feature.shelf.shelfGameKey
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.cartridgeSystemLabel
import com.swordfish.lemuroid.lib.library.db.entity.Game
import java.util.Locale

internal enum class ShelfSort { ARRANGEMENT, TITLE_ASC, TITLE_DESC, SYSTEM, LAST_PLAYED }

internal data class ShelfQuery(
    val text: String = "",
    val favourites: Boolean = false,
    val systems: Set<String> = emptySet(),
    val recent: Boolean = false,
    val sort: ShelfSort = ShelfSort.ARRANGEMENT,
) {
    val active: Boolean
        get() = text.isNotBlank() || favourites || systems.isNotEmpty() || recent || sort != ShelfSort.ARRANGEMENT
}

internal data class ShelfProjection(val layout: ShelfLayout, val games: List<Game>)

/** Temporary rendering projection only; never passed to ShelfStore or reconciliation. */
internal fun projectShelf(
    layout: ShelfLayout,
    games: List<Game>,
    query: ShelfQuery,
): ShelfProjection {
    if (!query.active) return ShelfProjection(layout, games)
    val needle = query.text.trim()
    val visible =
        games.filter { game ->
            (!query.favourites || game.isFavorite) &&
                (!query.recent || game.lastPlayedAt != null) &&
                (query.systems.isEmpty() || game.systemId in query.systems) &&
                (
                    needle.isEmpty() ||
                        listOf(
                            game.displayName(),
                            game.title,
                            game.fileName,
                            game.systemId,
                            cartridgeSystemLabel(game.systemId),
                        ).any { it.contains(needle, ignoreCase = true) }
                )
        }
    val byKey = visible.associateBy { shelfGameKey(it.systemId, it.fileUri) }
    val title =
        compareBy<ShelfCell> {
            byKey.getValue(requireNotNull(it.gameKey)).displayName().lowercase(Locale.ROOT)
        }
    val sections =
        layout.sections.mapNotNull { section ->
            val cells = section.cells.filter { it.gameKey != null && it.gameKey in byKey }
            if (cells.isEmpty()) {
                null
            } else {
                val ordered =
                    when (query.sort) {
                        ShelfSort.ARRANGEMENT -> cells
                        ShelfSort.TITLE_ASC -> cells.sortedWith(title)
                        ShelfSort.TITLE_DESC -> cells.sortedWith(title.reversed())
                        ShelfSort.SYSTEM ->
                            cells.sortedWith(
                                compareBy<ShelfCell> {
                                    byKey.getValue(requireNotNull(it.gameKey)).systemId
                                }.then(title),
                            )
                        ShelfSort.LAST_PLAYED ->
                            cells.sortedWith(
                                compareByDescending<ShelfCell> {
                                    byKey.getValue(requireNotNull(it.gameKey)).lastPlayedAt ?: Long.MIN_VALUE
                                }.then(title),
                        )
                    }
                section.copy(cells = ordered)
            }
        }
    val represented = sections.flatMap { it.cells }.mapNotNull { it.gameKey }.toSet()
    // The full-source reconcile runs asynchronously; never lose a newly indexed match meanwhile.
    val missing = byKey.filterKeys { it !in represented }
    val completed =
        if (missing.isEmpty()) {
            sections
        } else {
            val cells =
                missing.map { (key, game) ->
                    ShelfCell(
                        id = "projection:${game.cartridgeId}",
                        gameKey = key,
                        titleHint = game.displayName(),
                    )
                }
            val existing = sections.firstOrNull { it.id == ShelfLayout.UNSORTED }
            if (existing == null) {
                sections + ShelfSection(ShelfLayout.UNSORTED, "Unsorted", cells)
            } else {
                sections.map { if (it.id == existing.id) it.copy(cells = it.cells + cells) else it }
            }
        }
    return ShelfProjection(layout.copy(sections = completed), visible)
}
