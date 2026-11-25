package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CatalogueCartridgeCoverTest {
    @Test fun motifIsStableAndIndependentOfSystemCase() {
        val first = catalogueCoverMotif("Battle Bull", "gb")
        assertEquals(first, catalogueCoverMotif(" Battle Bull ", "GB"))
        assertEquals(first, catalogueCoverMotif("battle bull", "gb"))
    }

    @Test fun differentTitlesHaveDifferentCatalogueCoverIdentities() {
        val titles = listOf("Battle Bull", "F-Zero", "Action Force", "Bank Heist")
        val motifs = titles.map { catalogueCoverMotif(it, "gb") }
        assertEquals(titles.size, motifs.distinct().size)
        assertNotEquals(catalogueCoverMotif("F-Zero", "wiiu"), catalogueCoverMotif("F-Zero", "gb"))
    }
}
