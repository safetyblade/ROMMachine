package com.swordfish.lemuroid.app.mobile

import com.swordfish.lemuroid.lib.saves.PlayStationMemoryCard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayStationMemoryCardTest {
    @Test
    fun onlyRawPlayStationCardFormatCanSeedTheModernEngine() {
        val card = ByteArray(PlayStationMemoryCard.SIZE)
        card[0] = 'M'.code.toByte()
        card[1] = 'C'.code.toByte()
        assertTrue(PlayStationMemoryCard.isRawCard(card))
        assertFalse(PlayStationMemoryCard.isRawCard(card.copyOf(card.size - 1)))
        assertFalse(PlayStationMemoryCard.isRawCard(card.copyOf(card.size + 1)))
        assertFalse(PlayStationMemoryCard.isRawCard(ByteArray(card.size)))
        assertFalse(PlayStationMemoryCard.isRawCard(ByteArray(0)))
    }
}
