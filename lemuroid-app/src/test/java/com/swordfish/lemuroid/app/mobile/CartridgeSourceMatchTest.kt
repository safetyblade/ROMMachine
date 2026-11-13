package com.swordfish.lemuroid.app.mobile

import com.swordfish.lemuroid.lib.library.CartridgeSourceMatch
import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartridgeSourceMatchTest {
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

    @Test fun sameDocumentUnderNewTreeRetainsOriginalIdentity() {
        val original = game(
            "content://com.android.externalstorage.documents/tree/volumeA/document/primary%3AROMS%2Ftest.gb",
            12,
            5,
        )
        val replacement = game(
            "content://com.android.externalstorage.documents/tree/volumeB/document/primary%3AROMS%2Ftest.gb",
            22,
            10,
        )
        val result = CartridgeSourceMatch.uniqueRelocations(listOf(original), listOf(original, replacement), 10)
        assertEquals(1, result.size)
        assertEquals(original.cartridgeId, result.single().cartridgeId)
        assertEquals(replacement.fileUri, result.single().currentUri)
    }

    @Test fun neverMergePresentOldDocumentOrAmbiguousCandidate() {
        val original = game("content://com.android.externalstorage.documents/document/primary%3Aone", 12, 5)
        val seen = original.copy(lastIndexedAt = 10)
        val alias = game(
            "content://com.android.externalstorage.documents/tree/changed/document/primary%3Aone",
            22,
            10,
        )
        assertTrue(CartridgeSourceMatch.uniqueRelocations(listOf(original), listOf(seen, alias), 10).isEmpty())
        val other = original.copy(id = 30, fileUri = original.fileUri.replace("document/", "tree/other/document/"))
        val candidates = CartridgeSourceMatch.uniqueRelocations(
            listOf(original, other),
            listOf(original, other, alias),
            10,
        )
        assertTrue(candidates.isEmpty())
    }

    @Test fun filenameTitleProviderAndConsoleCannotProveIdentity() {
        val original = game("file:///roms/old.gb", 1, 5)
        val renamed = game("file:///new/old.gb", 2, 10)
        assertTrue(CartridgeSourceMatch.uniqueRelocations(listOf(original), listOf(original, renamed), 10).isEmpty())
        val document = game("content://com.android.externalstorage.documents/document/primary%3Aone", 1, 5)
        val otherConsole = game(
            "content://com.android.externalstorage.documents/tree/new/document/primary%3Aone",
            2,
            10,
            "gba",
        )
        assertTrue(
            CartridgeSourceMatch.uniqueRelocations(listOf(document), listOf(document, otherConsole), 10).isEmpty(),
        )
    }
}
