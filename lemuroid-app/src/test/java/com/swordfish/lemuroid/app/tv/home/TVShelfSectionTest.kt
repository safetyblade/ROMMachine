package com.swordfish.lemuroid.app.tv.home

import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfCell
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfLayout
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfSection
import com.swordfish.lemuroid.app.mobile.feature.shelf.shelfGameKey
import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.junit.Assert.assertEquals
import org.junit.Test

class TVShelfSectionTest {
    @Test fun namedSectionsRetainOrderAndNewGamesRemainReachable() {
        val first = game(1)
        val second = game(2)
        val third = game(3)
        val layout =
            ShelfLayout(
                sections =
                    listOf(
                        ShelfSection(ShelfLayout.UNSORTED, "Unsorted"),
                        ShelfSection("favorites", "My games", listOf(cell(second), cell(first))),
                    ),
            )
        val result = tvShelfSections(layout, listOf(first, second, third))
        assertEquals(listOf("Unsorted", "My games"), result.map { it.title })
        assertEquals(listOf(third), result.first().games)
        assertEquals(listOf(second, first), result.last().games)
        assertEquals(emptyList<ShelfCell>(), layout.sections.first().cells)
    }

    private fun cell(game: Game) = ShelfCell(gameKey = shelfGameKey(game.systemId, game.fileUri))

    private fun game(id: Int) =
        Game(
            id = id,
            fileName = "$id.gb",
            fileUri = "content://test/$id",
            title = "Game $id",
            systemId = "gb",
            developer = null,
            coverFrontUrl = null,
            lastIndexedAt = 0,
        )
}
