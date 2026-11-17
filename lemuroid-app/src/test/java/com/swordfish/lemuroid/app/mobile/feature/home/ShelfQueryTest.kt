package com.swordfish.lemuroid.app.mobile.feature.home

import com.swordfish.lemuroid.app.mobile.feature.shelf.CartridgeFacing
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfCell
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfLayout
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfSection
import com.swordfish.lemuroid.app.mobile.feature.shelf.shelfGameKey
import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ShelfQueryTest {
    private fun game(
        id: Int,
        title: String,
        system: String,
        favourite: Boolean = false,
        played: Long? = null,
    ) = Game(
        id = id,
        fileName = "$title.rom",
        fileUri = "file:///$id",
        title = title,
        systemId = system,
        developer = null,
        coverFrontUrl = null,
        lastIndexedAt = 0,
        lastPlayedAt = played,
        isFavorite = favourite,
        cartridgeId = "fixture-$id",
    )

    private val alpha = game(1, "Alpha", "gba", true, 1)
    private val zeta = game(2, "Zeta", "gba", true, 2)
    private val beta = game(3, "Beta", "psx")
    private val games = listOf(alpha, zeta, beta)

    private fun cell(
        game: Game,
        facing: CartridgeFacing = CartridgeFacing.SPINE,
    ) = ShelfCell(id = "cell-${game.id}", gameKey = shelfGameKey(game.systemId, game.fileUri), facing = facing)

    private val layout =
        ShelfLayout(
            sections =
                listOf(
                    ShelfSection(
                        ShelfLayout.UNSORTED,
                        "Unsorted",
                        listOf(cell(zeta), ShelfCell(id = "gap"), cell(alpha, CartridgeFacing.FRONT), cell(beta)),
                    ),
                ),
        )

    @Test fun clearReturnsOriginalArrangementAndExplicitGap() {
        val original = projectShelf(layout, games, ShelfQuery())
        assertSame(layout, original.layout)
        assertEquals("gap", original.layout.sections.single().cells[1].id)
    }

    @Test fun combinedFiltersPreserveFacingAndNeverRemoveOriginalGap() {
        val filtered =
            projectShelf(layout, games, ShelfQuery(text = "ALPHA", favourites = true, systems = setOf("gba")))
        assertEquals(listOf(alpha), filtered.games)
        assertEquals(listOf(cell(alpha, CartridgeFacing.FRONT)), filtered.layout.sections.single().cells)
        assertEquals(4, layout.sections.single().cells.size)
    }

    @Test fun sortingIsTemporaryAndRecentFilterUsesPlayedRecords() {
        val sorted = projectShelf(layout, games, ShelfQuery(recent = true, sort = ShelfSort.TITLE_ASC))
        assertEquals(listOf("cell-1", "cell-2"), sorted.layout.sections.single().cells.map { it.id })
        val recent = projectShelf(layout, games, ShelfQuery(sort = ShelfSort.LAST_PLAYED))
        assertEquals(listOf("cell-2", "cell-1", "cell-3"), recent.layout.sections.single().cells.map { it.id })
        assertEquals("cell-2", layout.sections.single().cells.first().id)
    }

    @Test fun noMatchesDoesNotCreateVacantCartridges() {
        val filtered = projectShelf(layout, games, ShelfQuery(text = "not present"))
        assertTrue(filtered.games.isEmpty())
        assertTrue(filtered.layout.sections.isEmpty())
    }
}
