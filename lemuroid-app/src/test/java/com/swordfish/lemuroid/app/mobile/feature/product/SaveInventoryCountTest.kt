package com.swordfish.lemuroid.app.mobile.feature.product

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveInventoryCountTest {
    private fun row(
        gameSaves: Int = 0,
        manualStates: Int = 0,
        resumeStates: Int = 0,
        cards: Int = 0,
    ) = SaveInventoryRow(
        id = "test-uuid",
        title = "Test Game",
        system = "gba",
        gameSaves = gameSaves,
        manualStates = manualStates,
        resumeStates = resumeStates,
        cards = cards,
        summary = "",
    )

    @Test fun manualOnlySaveIsCountedAsGameWithProgress() {
        assertTrue(row(manualStates = 1).hasSaveProgress)
    }

    @Test fun resumeOnlySaveIsCountedAsGameWithProgress() {
        assertTrue(row(resumeStates = 1).hasSaveProgress)
    }

    @Test fun ramAndNativeCardsAreCounted() {
        assertTrue(row(gameSaves = 1).hasSaveProgress)
        assertTrue(row(cards = 1).hasSaveProgress)
    }

    @Test fun emptyInventoryRowIsNotCountedAsProgress() {
        assertFalse(row().hasSaveProgress)
    }
}
