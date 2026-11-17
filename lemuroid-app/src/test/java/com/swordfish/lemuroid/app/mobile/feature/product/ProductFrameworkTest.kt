package com.swordfish.lemuroid.app.mobile.feature.product

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductFrameworkTest {
    @Test fun adultDockOrderIsStableAndNestedProductsNeverBecomeTabs() {
        val screens = ProductScreen.values().toSet()
        val capabilities = ProductCapability.values().toSet()
        assertEquals(
            listOf("Shelf", "GEM", "SafeSave", "CARTMOD", "Settings"),
            ProductFramework.adultDestinations(screens, capabilities, true).map { it.label },
        )
        assertFalse(ProductScreen.MODERN in ProductFramework.adultOrder)
        assertFalse(ProductScreen.VAULT in ProductFramework.adultOrder)
    }

    @Test fun registryDoesNotGrantServiceOrMountReadiness() {
        val mounted = setOf(ProductScreen.SHELF, ProductScreen.GEM, ProductScreen.SAFESAVE)
        val verified = setOf(ProductCapability.SHELF, ProductCapability.GEM, ProductCapability.CARTMOD)
        assertEquals(
            listOf(ProductScreen.SHELF, ProductScreen.GEM),
            ProductFramework.adultDestinations(mounted, verified, true),
        )
        assertTrue(ProductFramework.adultDestinations(mounted, verified, false).isEmpty())
        assertTrue(ProductFramework.adultDestinations(emptySet(), verified, true).isEmpty())
        assertTrue(ProductFramework.adultDestinations(mounted, emptySet(), true).isEmpty())
    }

    @Test fun logicalDestinationsAreUniqueAndParentsExist() {
        val screens = ProductScreen.values()
        val ids = screens.map { it.stableId }.toSet()
        assertEquals(screens.size, ids.size)
        assertTrue(screens.all { it.parentId == null || it.parentId in ids })
    }
}
