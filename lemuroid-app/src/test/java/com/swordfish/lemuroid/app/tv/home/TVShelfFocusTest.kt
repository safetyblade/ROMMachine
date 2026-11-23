package com.swordfish.lemuroid.app.tv.home

import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TVShelfFocusTest {
    @Test fun restoresExactCartridgeInItsCollectionAfterRowsAndCardsMove() {
        val first = game("first")
        val selected = game("selected")
        val other = game("other")
        val before = listOf(TVShelfSection("mine", "A collection", listOf(selected, first)))
        val saved = TVShelfFocus(checkNotNull(tvShelfRowKey(-1L, before)), selected.cartridgeId)
        val after =
            listOf(
                TVShelfSection("new", "A collection", listOf(other)),
                TVShelfSection("mine", "Renamed collection", listOf(first, selected)),
            )
        val rows =
            ArrayObjectAdapter().apply {
                add(row(TVHomeFragment.RECENTS_ADAPTER, listOf(selected)))
                add(row(-1L, after[0].games))
                add(row(-2L, after[1].games))
            }
        assertEquals(2 to 1, locateTVShelfFocus(rows, after, saved))
    }

    @Test fun preservesTheRowIfTheCartridgeWasRemoved() {
        val remaining = game("remaining")
        val sections = listOf(TVShelfSection("mine", "A collection", listOf(remaining)))
        val rows = ArrayObjectAdapter().apply { add(row(-1L, sections.single().games)) }
        assertEquals(0 to null, locateTVShelfFocus(rows, sections, TVShelfFocus("section:mine", "removed")))
        assertNull(locateTVShelfFocus(rows, sections, TVShelfFocus("section:deleted", remaining.cartridgeId)))
    }

    private fun row(
        id: Long,
        games: List<Game>,
    ) = ListRow(HeaderItem(id, "Row"), ArrayObjectAdapter().apply { addAll(0, games) })

    private fun game(name: String) =
        Game(
            fileName = "$name.gb",
            fileUri = "content://fixture/$name",
            title = "Same title",
            systemId = "gb",
            developer = null,
            coverFrontUrl = null,
            lastIndexedAt = 0,
        )
}
