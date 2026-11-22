package com.swordfish.lemuroid.app.mobile.feature.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProductDestinationTest {
    @Test
    fun tvProductEntryOpensTheRequestedMountedRoot() {
        assertEquals(MainRoute.SAFESAVE, initialProductRoute(openSafeSave = true, openGem = false))
        assertEquals(MainRoute.GEM, initialProductRoute(openSafeSave = false, openGem = true))
        assertEquals(MainRoute.HOME, initialProductRoute(openSafeSave = false, openGem = false))
    }

    @Test
    fun dockContainsOnlyMountedProductsInApprovedOrder() {
        assertEquals(
            listOf("Shelf", "GEM", "SafeSave", "CARTMOD", "Settings"),
            ProductDestination.mountedDock().map { it.second },
        )
        assertFalse(ProductDestination.isMountedRoot(null))
        assertFalse(ProductDestination.isMountedRoot(MainRoute.SETTINGS_DISPLAY))
    }

    @Test
    fun nestedProductsKeepTheirApprovedOwnership() {
        assertEquals(ProductDestination.SETTINGS, NestedProduct.MODERN.parentProduct)
        assertEquals(ProductDestination.SAFESAVE, NestedProduct.VAULT.parentProduct)
    }
}
