package com.swordfish.lemuroid.app.mobile.feature.shelf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CartdeckSignatureDesignTest {
    @Test
    fun signatureIsAnAdditionalCuratedAppearanceNotAReplacementEdition() {
        val signature = CartdeckCuratedDesigns.byId(CartdeckCuratedDesigns.SIGNATURE_ID)
        assertNotNull(signature)
        assertEquals("original", signature!!.baseEdition)
        assertEquals(0xFF13131AL, signature.colours.getValue("chassis"))
        assertEquals(0xFFFF4FA3L, signature.colours.getValue("accent"))
        assertEquals(0xFF19F5DAL, signature.colours.getValue("interactionAccent"))
        assertNotNull(CartdeckCuratedDesigns.byId(CartdeckCuratedDesigns.ARCANA_GILDED_ID))
        assertNotNull(CartdeckCuratedDesigns.byId(CartdeckCuratedDesigns.LIGHT_ID))
        assertNotEquals(CartdeckCuratedDesigns.LIGHT_ID, signature.id)
        assertNull(CartdeckCuratedDesigns.byId("signature-not-defined"))
    }

    @Test
    fun signatureSelectionRetainsIndependentFactoryDesignsAndDoesNotMutateOriginal() {
        val original = ShelfAppearance()
        val selected = CartdeckCuratedDesigns.apply(original, CartdeckCuratedDesigns.SIGNATURE_ID)
        assertEquals("original", original.edition)
        assertEquals(original.edition, selected.edition)
        assertEquals(CartdeckCuratedDesigns.SIGNATURE_ID, selected.curatedDesignId)
        assertNotEquals(original.custom, selected.custom)
        assertEquals(0xFFFF4FA3L, selected.custom.getValue("accent"))
        assertEquals(CartdeckLighting.FINISH_NEON, selected.lighting.finish)
        assertEquals(CartdeckLighting.MODE_STATIC, selected.lighting.mode)
        assertEquals(40, selected.lighting.intensity)
    }
}
