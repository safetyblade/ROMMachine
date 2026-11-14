package com.swordfish.lemuroid.app.mobile

import com.swordfish.lemuroid.lib.library.CartridgeSourceMatch
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.library.db.entity.RetiredCartridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartridgeSourceMatchTest {
    private val provider = "content://com.android.externalstorage.documents"

    private fun game(
        uri: String,
        id: Int,
        seen: Long,
        console: String = "gb",
    ) = Game(
        id = id,
        fileName = "Synthetic.gb",
        fileUri = uri,
        title = "Synthetic",
        systemId = console,
        developer = null,
        coverFrontUrl = null,
        lastIndexedAt = seen,
    )

    @Test fun retiredMatchRequiresUniqueDocumentAndConsoleAndRejectsAmbiguity() {
        val old = game("$provider/tree/a/document/primary%3Atombstone", 42, 5)
        val tombstone = RetiredCartridge.fromGame(old, 10L)
        val other = game("$provider/tree/b/document/primary%3Atombstone", 72, 12)
        val matched = CartridgeSourceMatch.uniqueRetiredMatches(listOf(tombstone), emptyList(), listOf(other), 12)
        assertEquals(1, matched.size)
        assertEquals(old.cartridgeId, matched.single().first.cartridgeId)

        val another = other.copy(id = 73)
        assertTrue(
            CartridgeSourceMatch.uniqueRetiredMatches(
                listOf(tombstone),
                emptyList(),
                listOf(other, another),
                12,
            ).isEmpty(),
        )
        assertTrue(
            CartridgeSourceMatch.uniqueRetiredMatches(
                listOf(tombstone),
                listOf(old),
                listOf(other),
                12,
            ).isEmpty(),
        )
        assertTrue(
            CartridgeSourceMatch.uniqueRetiredMatches(
                listOf(tombstone),
                emptyList(),
                listOf(other.copy(systemId = "gba")),
                12,
            ).isEmpty(),
        )
        assertTrue(
            CartridgeSourceMatch.uniqueRetiredMatches(
                listOf(tombstone),
                emptyList(),
                listOf(other.copy(fileUri = "$provider/document/primary%3Aother")),
                12,
            ).isEmpty(),
        )
    }

    @Test fun sameDocumentUnderNewTreeRetainsOriginalIdentity() {
        val original = game("$provider/tree/volumeA/document/primary%3Aone", 12, 5)
        val replacement = game("$provider/tree/volumeB/document/primary%3Aone", 22, 10)
        val moves = CartridgeSourceMatch.uniqueRelocations(listOf(original), listOf(original, replacement), 10)
        assertEquals(1, moves.size)
        assertEquals(original.cartridgeId, moves.single().cartridgeId)
        assertEquals(replacement.fileUri, moves.single().currentUri)
    }

    @Test fun neverMergePresentOldDocumentOrAmbiguousCandidate() {
        val original = game("$provider/document/primary%3Aone", 12, 5)
        val seen = original.copy(lastIndexedAt = 10)
        val alias = game("$provider/tree/changed/document/primary%3Aone", 22, 10)
        assertTrue(CartridgeSourceMatch.uniqueRelocations(listOf(original), listOf(seen, alias), 10).isEmpty())
        val other = game("$provider/tree/other/document/primary%3Aone", 30, 5)
        assertTrue(
            CartridgeSourceMatch.uniqueRelocations(listOf(original, other), listOf(original, other, alias), 10)
                .isEmpty(),
        )
    }

    @Test fun filenameTitleProviderAndConsoleCannotProveIdentity() {
        val original = game("file:///roms/old.gb", 1, 5)
        val renamed = game("file:///new/old.gb", 2, 10)
        assertTrue(CartridgeSourceMatch.uniqueRelocations(listOf(original), listOf(original, renamed), 10).isEmpty())
        val document = game("$provider/document/primary%3Aone", 1, 5)
        val otherConsole = game("$provider/tree/new/document/primary%3Aone", 2, 10, "gba")
        assertTrue(
            CartridgeSourceMatch.uniqueRelocations(listOf(document), listOf(document, otherConsole), 10)
                .isEmpty(),
        )
    }
}
